package com.ironmarket.app.ui.buyer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.lifecycleScope
import com.ironmarket.app.BuildConfig
import com.ironmarket.app.databinding.ActivityEpaycoBinding
import com.ironmarket.app.ui.common.RoleActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Integración con la pasarela de pagos ePayco (proveedor certificado en Colombia).
 *
 * Flujo:
 * 1. Carga en un WebView el checkout oficial (checkout.js) con la llave pública y el modo de pruebas.
 * 2. Cuando el pago termina, ePayco redirige a la URL de respuesta con el parámetro `ref_payco`.
 * 3. La app consulta la API de validación de ePayco con esa referencia para confirmar la transacción.
 * 4. Devuelve el resultado a [CheckoutActivity] (aceptada, pendiente o rechazada).
 *
 * La app nunca ve ni guarda los datos de la tarjeta: los maneja ePayco (PCI DSS).
 */
class EpaycoActivity : RoleActivity() {

    private lateinit var binding: ActivityEpaycoBinding
    private var finished = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasSession) return
        binding = ActivityEpaycoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.closeButton.setOnClickListener { setResult(Activity.RESULT_CANCELED); finish() }

        // En compilaciones de prueba se ofrece simular la aprobación si el sandbox no responde
        if (BuildConfig.DEBUG && BuildConfig.EPAYCO_TEST) {
            binding.simulateButton.visibility = View.VISIBLE
            binding.simulateButton.setOnClickListener {
                deliver(PaymentResult(true, "SANDBOX-${System.currentTimeMillis() % 10_000_000}", "Aceptada (simulada)"))
            }
        }

        with(binding.webView) {
            settings.javaScriptEnabled = true          // checkout.js necesita JavaScript
            settings.domStorageEnabled = true
            setBackgroundColor(0xFF0E0F11.toInt())
            webViewClient = object : WebViewClient() {
                /** Intercepta la URL de respuesta de ePayco para leer la referencia del pago. */
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    handleUrl(request.url)
                /** Muestra el indicador de carga. */
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    binding.progress.visibility = View.VISIBLE
                    handleUrl(Uri.parse(url))
                }
                /** Oculta el indicador de carga. */
                override fun onPageFinished(view: WebView, url: String) { binding.progress.visibility = View.GONE }
            }
            loadDataWithBaseURL(BASE_URL, checkoutHtml(), "text/html", "UTF-8", null)
        }
    }

    /** Detecta la URL de respuesta de ePayco y valida la transacción. */
    private fun handleUrl(uri: Uri): Boolean {
        if (!uri.toString().startsWith(RESPONSE_URL)) return false
        val ref = uri.getQueryParameter("ref_payco") ?: uri.getQueryParameter("x_ref_payco")
        if (ref == null || finished) return true
        finished = true
        lifecycleScope.launch { deliver(validate(ref)) }
        return true
    }

    /** Consulta el estado real de la transacción en la API pública de validación de ePayco. */
    private suspend fun validate(ref: String): PaymentResult = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://secure.epayco.co/validation/v1/reference/$ref").openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000; conn.readTimeout = 10_000
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val data = json.getJSONObject("data")
            val code = data.optInt("x_cod_response", 4)
            // 1 = Aceptada, 3 = Pendiente (ambas se registran), 2 y 4 = Rechazada/Fallida
            PaymentResult(code == 1 || code == 3, data.optString("x_ref_payco", ref), data.optString("x_response", "Desconocido"))
        }.getOrElse { PaymentResult(false, ref, "No se pudo confirmar la transacción") }
    }

    /** Devuelve el resultado del pago a CheckoutActivity y cierra la pasarela. */
    private fun deliver(result: PaymentResult) {
        setResult(Activity.RESULT_OK, Intent()
            .putExtra(EXTRA_APPROVED, result.approved)
            .putExtra(EXTRA_REF, result.reference)
            .putExtra(EXTRA_MESSAGE, result.message))
        finish()
    }

    /** HTML que abre el checkout de ePayco dentro de la página (modo onpage). */
    private fun checkoutHtml(): String {
        val i = intent
        /** Escapa un texto para insertarlo en el JavaScript del checkout. */
        fun js(s: String?) = JSONObject.quote(s ?: "")
        return """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <script src="https://checkout.epayco.co/checkout.js"></script>
            </head>
            <body style="background:#0E0F11;color:#A3A8B0;font-family:sans-serif;text-align:center;padding-top:45%">
            <p>Abriendo la pasarela de pagos…</p>
            <script>
              var handler = ePayco.checkout.configure({ key: ${js(BuildConfig.EPAYCO_PUBLIC_KEY)}, test: ${BuildConfig.EPAYCO_TEST} });
              handler.open({
                name: "IronMarket", description: ${js(i.getStringExtra(EXTRA_DESCRIPTION))},
                invoice: ${js(i.getStringExtra(EXTRA_INVOICE))}, currency: "cop",
                amount: "${i.getIntExtra(EXTRA_AMOUNT, 0)}", tax_base: "0", tax: "0",
                country: "co", lang: "es", external: "false",
                response: "$RESPONSE_URL",
                email_billing: ${js(i.getStringExtra(EXTRA_EMAIL))}, name_billing: ${js(i.getStringExtra(EXTRA_NAME))}
              });
            </script></body></html>
        """.trimIndent()
    }

    /** Resultado de la transacción. */
    data class PaymentResult(val approved: Boolean, val reference: String, val message: String)

    companion object {
        private const val BASE_URL = "https://ironmarket.app/"
        const val RESPONSE_URL = "https://ironmarket.app/epayco/respuesta"
        const val EXTRA_AMOUNT = "amount"; const val EXTRA_DESCRIPTION = "description"; const val EXTRA_INVOICE = "invoice"
        const val EXTRA_EMAIL = "email"; const val EXTRA_NAME = "name"
        const val EXTRA_APPROVED = "approved"; const val EXTRA_REF = "ref"; const val EXTRA_MESSAGE = "message"

        /** Crea el Intent para abrir la pasarela con los datos de la compra. */
        fun intent(context: Context, amount: Int, description: String, invoice: String, email: String, name: String) =
            Intent(context, EpaycoActivity::class.java)
                .putExtra(EXTRA_AMOUNT, amount).putExtra(EXTRA_DESCRIPTION, description).putExtra(EXTRA_INVOICE, invoice)
                .putExtra(EXTRA_EMAIL, email).putExtra(EXTRA_NAME, name)
    }
}
