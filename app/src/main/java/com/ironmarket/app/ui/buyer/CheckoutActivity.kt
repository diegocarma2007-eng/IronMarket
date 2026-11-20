package com.ironmarket.app.ui.buyer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ironmarket.app.BuildConfig
import com.ironmarket.app.R
import com.ironmarket.app.data.CartItem
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityCheckoutBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.ui.common.RoleHostActivity
import com.ironmarket.app.util.Biometrics
import com.ironmarket.app.util.LocationHelper
import com.ironmarket.app.util.badge
import com.ironmarket.app.util.launchSafe
import com.ironmarket.app.util.toCop

/**
 * Pasarela de pagos:
 * 1. Dirección de entrega con geolocalización (FusedLocationProvider + Geocoder).
 * 2. Confirmación con huella (si el dispositivo la tiene).
 * 3. Pago en ePayco ([EpaycoActivity]) y, si se aprueba, registro de las órdenes en Firestore.
 */
class CheckoutActivity : RoleActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private var items: List<CartItem> = emptyList()
    private var lat: Double? = null
    private var lng: Double? = null
    private val shipping get() = if (binding.deliveryHome.isChecked) 8_000 else 0
    private val total get() = items.sumOf { it.subtotal } + shipping

    /** Solicitud del permiso de ubicación en tiempo de ejecución. */
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) locate()
        else binding.addressLayout.error = "Sin permiso de ubicación: escribe la dirección manualmente"
    }

    /** Recibe el resultado de ePayco. */
    private val paymentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val ref = data.getStringExtra(EpaycoActivity.EXTRA_REF).orEmpty()
        if (data.getBooleanExtra(EpaycoActivity.EXTRA_APPROVED, false)) registerOrders(ref, data.getStringExtra(EpaycoActivity.EXTRA_MESSAGE).orEmpty())
        else MaterialAlertDialogBuilder(this).setTitle("Pago no aprobado")
            .setMessage("ePayco respondió: ${data.getStringExtra(EpaycoActivity.EXTRA_MESSAGE)}.\nReferencia: $ref")
            .setPositiveButton("Entendido", null).show()
    }

    /** Configura el pago del pedido: infla la vista y registra los eventos de los controles. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasSession) return
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.addressInput.setText(Session.currentUser?.address)
        binding.locateButton.setOnClickListener { requestLocation() }
        binding.deliveryGroup.setOnCheckedChangeListener { _, _ -> updateTotals() }
        binding.testBadge.badge(if (BuildConfig.EPAYCO_TEST) "PRUEBAS" else "PRODUCCIÓN", R.color.im_warning_dark, R.color.im_warning)
        binding.payButton.setOnClickListener { pay() }

        launchSafe {
            items = Session.repo.getCart(Session.currentUser!!.id)
            if (items.isEmpty()) { finish(); return@launchSafe }
            binding.itemsSummary.text = items.joinToString("\n") { "${it.quantity} × ${it.product.name}  ·  ${it.subtotal.toCop()}" }
            updateTotals()
        }
    }

    /** Recalcula envío y total según el tipo de entrega. */
    private fun updateTotals() {
        binding.subtotal.text = items.sumOf { it.subtotal }.toCop()
        binding.shipping.text = if (shipping == 0) "Gratis" else shipping.toCop()
        binding.total.text = total.toCop()
    }

    /** Pide el permiso de ubicación si hace falta y luego obtiene la dirección. */
    private fun requestLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) locate()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    /** Obtiene la ubicación actual y la convierte en una dirección legible. */
    private fun locate() = launchSafe(binding.locateButton) {
        binding.locateButton.text = "Buscando tu ubicación…"
        val place = LocationHelper.currentPlace(this)
        binding.locateButton.text = "Usar mi ubicación actual"
        if (place == null) { binding.addressLayout.error = "No se pudo obtener la ubicación. Activa el GPS."; return@launchSafe }
        lat = place.lat; lng = place.lng
        binding.addressLayout.error = null
        binding.addressInput.setText(place.address)
        binding.coordinates.text = "Ubicación: %.5f, %.5f".format(place.lat, place.lng)
        binding.coordinates.visibility = View.VISIBLE
    }

    /** Valida la dirección, pide la huella (si existe) y abre ePayco. */
    private fun pay() {
        val address = binding.addressInput.text.toString().trim()
        if (binding.deliveryHome.isChecked && address.length < 6) { binding.addressLayout.error = "Escribe la dirección de entrega"; return }
        val openGateway = {
            val user = Session.currentUser!!
            paymentLauncher.launch(EpaycoActivity.intent(this, total,
                "Compra IronMarket (${items.sumOf { it.quantity }} productos)",
                "IM-${System.currentTimeMillis()}", user.email, user.name))
        }
        if (Biometrics.unavailableReason(this) == null)
            Biometrics.prompt(this, getString(R.string.bio_pay_title), "Total: ${total.toCop()}", onSuccess = openGateway)
        else openGateway()
    }

    /** Registra las órdenes en la base de datos con la referencia de ePayco y muestra la confirmación. */
    private fun registerOrders(paymentRef: String, message: String) = launchSafe(binding.payButton) {
        val user = Session.currentUser!!
        val delivery = if (shipping > 0) "Envío a domicilio" else "Recoge en tienda"
        val address = binding.addressInput.text.toString().trim().ifEmpty { "Recoge en tienda" }
        val orders = Session.repo.checkout(user, items, delivery, address, lat, lng, shipping, paymentRef)
        if (address != user.address && shipping > 0) { user.address = address; Session.repo.updateUser(user) }
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_check)
            .setTitle("¡Pago aprobado!")
            .setMessage("ePayco: $message\nReferencia: $paymentRef\nTotal: ${total.toCop()}\n\nSe crearon ${orders.size} orden(es). " +
                "Tienes 5 días hábiles para ejercer el derecho de retracto.")
            .setCancelable(false)
            .setPositiveButton("Seguir comprando") { _, _ ->
                startActivity(Intent(this, BuyerActivity::class.java)
                    .putExtra(RoleHostActivity.EXTRA_TAB, R.id.nav_home)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            }
            .show()
    }
}
