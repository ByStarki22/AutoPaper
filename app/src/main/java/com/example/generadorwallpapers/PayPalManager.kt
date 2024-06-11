package com.example.generadorwallpapers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.paypal.android.sdk.payments.*
import org.json.JSONException
import java.math.BigDecimal

class PayPalManager(private val context: Context, private val clientId: String) {

    private val paypalConfig: PayPalConfiguration = PayPalConfiguration()
        .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX) // Cambiar a PayPalConfiguration.ENVIRONMENT_PRODUCTION para producción
        .clientId(clientId)

    fun startPayment(amount: BigDecimal) {
        val paymentIntent = Intent(context, PaymentActivity::class.java)
        paymentIntent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, paypalConfig)
        paymentIntent.putExtra(PaymentActivity.EXTRA_PAYMENT, createPayPalPayment(amount))
        (context as Activity).startActivityForResult(paymentIntent, PAYPAL_REQUEST_CODE)
    }

    private fun createPayPalPayment(amount: BigDecimal): PayPalPayment {
        return PayPalPayment(amount, "USD", "Descripción del pago", PayPalPayment.PAYMENT_INTENT_SALE)
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val confirm = data?.getParcelableExtra<PaymentConfirmation>(PaymentActivity.EXTRA_RESULT_CONFIRMATION)
                confirm?.let {
                    try {
                        Toast.makeText(context, "Pago realizado con éxito", Toast.LENGTH_SHORT).show()
                        return true
                    } catch (ex: JSONException) {
                        ex.printStackTrace()
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(context, "Pago cancelado", Toast.LENGTH_SHORT).show()
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Toast.makeText(context, "Pago inválido", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    companion object {
        private const val PAYPAL_REQUEST_CODE = 7171
    }
}
