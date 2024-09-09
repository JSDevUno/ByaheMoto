package com.example.byahemoto

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class forgot_password : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var sendResetLinkButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailEditText = findViewById(R.id.etEmail)
        sendResetLinkButton = findViewById(R.id.btnSendResetLink)

        sendResetLinkButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                sendResetLink(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendResetLink(email: String) {
        val emailMap = mapOf("email" to email)

        RetrofitInstance.getAuthService(this).sendResetLink(emailMap).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@forgot_password, "Reset link sent to $email", Toast.LENGTH_SHORT).show()
                } else {
                    val errorResponse = response.errorBody()?.string()
                    if (response.code() == 404) {
                        Toast.makeText(this@forgot_password, "Email not found", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("ForgotPassword", "Failed to send reset link: $errorResponse")
                        Toast.makeText(this@forgot_password, "Failed to send reset link: $errorResponse", Toast.LENGTH_SHORT).show()
                    }

                    Log.e("ForgotPassword", "Response headers: ${response.headers()}")
                    Log.e("ForgotPassword", "Response message: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("ForgotPassword", "Error sending reset link", t)
                Toast.makeText(this@forgot_password, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                t.printStackTrace()
            }
        })
    }
}
