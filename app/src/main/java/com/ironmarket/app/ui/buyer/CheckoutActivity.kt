package com.ironmarket.app.ui.buyer

import android.os.Bundle
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ironmarket.app.R
import com.ironmarket.app.data.DataRepository
import com.ironmarket.app.data.Session
import com.ironmarket.app.databinding.ActivityCheckoutBinding
import com.ironmarket.app.ui.common.RoleActivity
import com.ironmarket.app.util.toCop

/**
 * Pasarela de pagos (interfaz): entrega, medio de pago y confirmación con huella.
 * En producción el pago se tokeniza con el SDK de la pasarela (p. ej. Wompi);
 * la app nunca guarda el número de la tarjeta.
 */
class CheckoutActivity : RoleActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val shipping get() = if (binding.deliveryHome.isChecked) 8_000 else 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.changeAddressButton.setOnClickListener {
            val addresses = arrayOf("Cra. 13 # 63-20, apto 502", "Calle 100 # 15-30, oficina 401")
            MaterialAlertDialogBuilder(this).setTitle("Direcciones guardadas")
                .setItems(addresses) { _, i -> binding.address.text = addresses[i] }.show()
        }
        binding.deliveryGroup.setOnCheckedChangeListener { _, _ -> updateTotals() }
        binding.paymentToggle.addOnButtonCheckedListener { _, id, checked ->
            if (checked) binding.cardForm.visibility = if (id == R.id.payCard) View.VISIBLE else View.GONE
        }
        binding.payButton.setOnClickListener { if (validate()) confirmWithBiometrics() }
        updateTotals()
    }

    private fun updateTotals() {
        val subtotal = DataRepository.cartSubtotal()
        binding.subtotal.text = subtotal.toCop()
        binding.shipping.text = if (shipping == 0) "Gratis" else shipping.toCop()
        binding.total.text = (subtotal + shipping).toCop()
    }

    private fun validate(): Boolean {
        if (binding.paymentToggle.checkedButtonId != R.id.payCard) return true
        var ok = true
        binding.cardNumberLayout.error = null; binding.expiryLayout.error = null; binding.cvvLayout.error = null
        if ((binding.cardNumberInput.text?.length ?: 0) < 15) { binding.cardNumberLayout.error = "Número incompleto"; ok = false }
        if (!Regex("\\d{2}/\\d{2}").matches(binding.expiryInput.text.toString())) { binding.expiryLayout.error = "Usa MM/AA"; ok = false }
        if ((binding.cvvInput.text?.length ?: 0) < 3) { binding.cvvLayout.error = "CVV inválido"; ok = false }
        return ok
    }

    /** Si el dispositivo tiene huella, se pide antes de pagar; si no, se confirma directamente. */
    private fun confirmWithBiometrics() {
        val auth = BIOMETRIC_STRONG or BIOMETRIC_WEAK
        if (BiometricManager.from(this).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
            completePayment(); return
        }
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = completePayment()
        }).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.bio_pay_title))
                .setSubtitle("Total: ${binding.total.text}")
                .setAllowedAuthenticators(auth)
                .setNegativeButtonText(getString(R.string.cancel))
                .build()
        )
    }

    private fun completePayment() {
        val user = Session.currentUser ?: return
        val delivery = if (shipping > 0) "Envío a domicilio" else "Recoge en tienda"
        val total = DataRepository.checkout(user, delivery, shipping)
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_check)
            .setTitle("¡Pago aprobado!")
            .setMessage("Pagaste ${total.toCop()}. Te enviamos el comprobante a ${user.email}.\nRecuerda que tienes 5 días hábiles para ejercer el derecho de retracto.")
            .setCancelable(false)
            .setPositiveButton("Seguir comprando") { _, _ -> finish() }
            .show()
    }
}
