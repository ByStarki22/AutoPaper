package com.example.generadorwallpapers

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var editText: EditText? = null
    private var textView: TextView? = null
    private val stringURLEndPoint = "https://api.openai.com/v1/images/generations"
    private val stringAPIKey = "OpenAi Api Key"
    private var bitmapOutputImage: Bitmap? = null
    private var stringOutput: String? = null
    private lateinit var payPalManager: PayPalManager
    private var mInterstitialAd: InterstitialAd? = null
    lateinit var btnImage:Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@MainActivity) {}
        }

        imageView = findViewById(R.id.imageView)
        editText = findViewById(R.id.editTextText2)
        textView = findViewById(R.id.textView)
        val scrollView = findViewById<ScrollView>(R.id.scrollView)

        scrollView.viewTreeObserver.addOnScrollChangedListener(
            ViewTreeObserver.OnScrollChangedListener {
                val scrollY = scrollView.scrollY
                val alpha = (1 - scrollY / 1000f).coerceAtLeast(0f)
                imageView?.alpha = alpha
            }
        )

        // Inicializar PayPalManager con tu ID de cliente de PayPal
        payPalManager = PayPalManager(this, "Your PayPal api ")

        val buttonPay = findViewById<Button>(R.id.buttonPay)
        buttonPay.setOnClickListener {
            val editTextDonationAmount = findViewById<EditText>(R.id.editTextDonationAmount)
            val amountString = editTextDonationAmount.text.toString()
            if (amountString.isNotEmpty()) {
                val amount = BigDecimal(amountString.trim()) // Usando el constructor que toma una cadena de texto
            } else {
                Toast.makeText(this, getString(R.string.cantidad_invalida), Toast.LENGTH_SHORT).show()
            }
        }

        btnImage =findViewById(R.id.ButtonImageGallery)
        btnImage.setOnClickListener{
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

    }

    fun buttonAction(view: View) {
        val stringInputText = editText!!.text.toString()

        if (stringInputText.isEmpty()) {
            showAlert(getString(R.string.input_empty_warning))
            return
        }

        val button = findViewById<Button>(R.id.buttonGenerarImagen)
        button.isEnabled = false // Deshabilita el botón

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE // Muestra el ProgressBar

        val jsonObject = JSONObject()

        try {
            jsonObject.put("prompt", stringInputText)
            jsonObject.put("n", 1) // Número de imágenes a generar
            jsonObject.put("size", "1024x1792") // Tamaño de la imagen
            jsonObject.put("model", "dall-e-3") // Modelo a utilizar

        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            stringURLEndPoint,
            jsonObject,
            Response.Listener { response ->
                try {
                    stringOutput = response.getJSONArray("data").getJSONObject(0).getString("url")
                    textView!!.text = stringOutput
                    textView!!.text = getString(R.string.progress_dialog_message)
                    val thread = Thread {
                        bitmapOutputImage = try {
                            val url = URL(stringOutput)
                            BitmapFactory.decodeStream(url.openStream())
                        } catch (e: MalformedURLException) {
                            throw RuntimeException(e)
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                    }
                    thread.start()
                    thread.join()
                    if (bitmapOutputImage != null) {
                        val bitmapFinalImage = Bitmap.createScaledBitmap(
                            bitmapOutputImage!!,
                            imageView!!.width,
                            imageView!!.height,
                            true
                        )
                        imageView!!.setImageBitmap(bitmapFinalImage)
                        textView!!.text = getString(R.string.image_generation_successful)
                    }
                    // Habilita el botón y oculta el ProgressBar
                    button.isEnabled = true
                    progressBar.visibility = View.GONE

                    loadAd()
                    mInterstitialAd?.show(this)
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            },
            Response.ErrorListener { error ->
                error.networkResponse?.let {
                    val statusCode = it.statusCode
                    val data = it.data?.let { data -> String(data) } ?: "No additional data"
                    textView!!.text = "Error $statusCode: $data"
                } ?: run {
                    textView!!.text = "Unknown error occurred"
                }
                // Habilita el botón y oculta el ProgressBar en caso de error
                button.isEnabled = true
                progressBar.visibility = View.GONE
            }

        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val mapHeader: MutableMap<String, String> = HashMap()
                mapHeader["Authorization"] = "Bearer $stringAPIKey"
                mapHeader["Content-Type"] = "application/json"
                return mapHeader
            }
        }

        val intTimeOutPeriod = 60000 // 60 segundos
        val retryPolicy: RetryPolicy = DefaultRetryPolicy(
            intTimeOutPeriod,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        jsonObjectRequest.retryPolicy = retryPolicy

        Volley.newRequestQueue(applicationContext).add(jsonObjectRequest)

        loadAd()
        mInterstitialAd?.show(this)
    }

    fun showAlert(message: String) {
        val dialogView = layoutInflater.inflate(R.layout.custom_alert_dialog, null)
        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOk)

        textViewMessage.text = message

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()

        buttonOk.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    fun buttonTopRightAction(view: View) {
        if (bitmapOutputImage != null) {
            try {
                // Establece la imagen generada como fondo de pantalla
                WallpaperManager.getInstance(applicationContext).setBitmap(bitmapOutputImage)
                textView!!.text = getString(R.string.wallpaper_set_success)
            } catch (e: IOException) {
                textView!!.text = getString(R.string.set_wallpaper_error)
            }
        } else {
            showAlert(getString(R.string.background_set_warning))
        }
    }

    fun saveImageToGallery(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(path, filename)

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Agrega la imagen a la galería
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            sendBroadcast(mediaScanIntent)

            Toast.makeText(applicationContext, getString(R.string.save_image_success), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, getString(R.string.save_image_error), Toast.LENGTH_SHORT).show()
        }
    }

    fun buttonTopLeftAction(view: View) {
        if (bitmapOutputImage != null) {
            saveImageToGallery(bitmapOutputImage!!)
        } else {
            showAlert(getString(R.string.input_text_empty_warning))
        }
    }

    fun setWallpaper(view: View) {
        if (bitmapOutputImage != null) {
            val wallpaperButton = view as Button
            val wallpaperIndex = wallpaperButton.text.toString().substring(10).trim().toInt()
        } else {
            showAlert(getString(R.string.background_set_warning))
        }
    }

    fun showDonationDialog(view: View) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_donation, null)
        val editTextDonationAmount = dialogView.findViewById<EditText>(R.id.editTextDonationAmount)
        val buttonPay = dialogView.findViewById<Button>(R.id.buttonPay)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val alertDialog = builder.create()

        buttonPay.setOnClickListener {
            val amountString = editTextDonationAmount.text.toString()
            if (amountString.isNotEmpty()) {
                val amount = BigDecimal(amountString.trim()) // Usando el constructor que toma una cadena de texto
                payPalManager.startPayment(amount)
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.cantidad_invalida), Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    fun loadAd() {
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri != null){
            try {
                bitmapOutputImage = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                imageView?.setImageBitmap(bitmapOutputImage)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.error_cargar_imagen), Toast.LENGTH_SHORT).show()
            }

        }
    }

}
