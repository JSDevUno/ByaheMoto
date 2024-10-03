package com.example.byahemoto

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.OrderRequest
import com.example.byahemoto.models.OrderResponse
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Topup : AppCompatActivity() {

    private lateinit var amountEditText: EditText
    private lateinit var webView: WebView
    private lateinit var orderId: String
    private var topUpAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_up)

        amountEditText = findViewById(R.id.textInputLayout)
        val refillButton = findViewById<RelativeLayout>(R.id.refill)

        refillButton.setOnClickListener {
            topUpAmount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            if (topUpAmount in 20.0..3000.0) {
                createPayPalOrder(topUpAmount)
            } else {
                Toast.makeText(this, "Please enter a valid amount between 20 and 3000", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createPayPalOrder(amount: Double) {
//        val requestBody = mapOf("amount" to amount)
        val orderRequest = OrderRequest(amount)
        RetrofitInstance.getAuthService(this).topUp("Bearer ${getTokenForCurrentUser()}", orderRequest)
            .enqueue(object : Callback<OrderResponse> {
                override fun onResponse(call: Call<OrderResponse>, response: Response<OrderResponse>) {
                    if (response.isSuccessful) {
                        val approvalUrl = response.body()?.data?.links?.find { it.rel == "approve" }?.href
                        orderId = response.body()?.data?.id ?: ""

                        approvalUrl?.let {
                            Toast.makeText(this@Topup, "Please wait...", Toast.LENGTH_LONG).show()
                            openWebView(it)
                        }
                    } else {
                        if (response.code() == 401) {
                            // It returns the actual cause of 401 error
                            val errorBody = response.errorBody()?.string()
                            val errorMessage = errorBody?.substringAfter("message\":\"")?.substringBefore("\"")
                            Toast.makeText(this@Topup, errorMessage, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@Topup, "Failed to create PayPal order", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<OrderResponse>, t: Throwable) {
                    Toast.makeText(this@Topup, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebView(approvalUrl: String) {
        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d("WEBVIEW", "URL: $url")
                if (url.contains("return")) {
                    Log.d("WEBVIEW", "Payment Captured")
                    capturePayPalOrder()
                } else if (url.contains("cancel")) {
                    Toast.makeText(this@Topup, "Payment cancelled", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        webView.loadUrl(approvalUrl)
    }

    private fun capturePayPalOrder() {
        RetrofitInstance.getAuthService(this).captureTopUp("Bearer ${getTokenForCurrentUser()}", orderId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Topup, "Top-up successful!", Toast.LENGTH_SHORT).show()
                        returnToWalletActivity()
                    } else {
                        Toast.makeText(this@Topup, "Failed to capture payment", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@Topup, "Error capturing payment: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun returnToWalletActivity() {
        val resultIntent = Intent()
        resultIntent.putExtra("added_amount", topUpAmount)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun getTokenForCurrentUser(): String {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPref.getString("access_token", "") ?: ""
    }
}