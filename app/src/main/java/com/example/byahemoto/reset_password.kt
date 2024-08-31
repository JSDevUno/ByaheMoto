package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.ResetPasswordRequest
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class reset_password : AppCompatActivity() {

    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetPasswordButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        newPasswordEditText = findViewById(R.id.etNewPassword)
        confirmPasswordEditText = findViewById(R.id.etConfirmPassword)
        resetPasswordButton = findViewById(R.id.btnResetPassword)

        resetPasswordButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else if (!isPasswordValid(newPassword)) {
                Toast.makeText(this, "Password is too weak", Toast.LENGTH_SHORT).show()
            } else {
                val token = intent.getStringExtra("token")
                if (token != null) {
                    resetPassword(token, newPassword, confirmPassword)
                } else {
                    Toast.makeText(this, "Token is missing or invalid", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetPassword(token: String, password: String, confirmPassword: String) {
        val resetPasswordRequest = ResetPasswordRequest(token, password, confirmPassword)

        RetrofitInstance.authService.resetPassword(resetPasswordRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@reset_password, "Password reset successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@reset_password, MainActivity::class.java))
                    finish()
                } else {
                    val errorResponse = response.errorBody()?.string()
                    Toast.makeText(this@reset_password, "Failed to reset password: $errorResponse", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@reset_password, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = Regex("^(?=.*[0-9])(?=.*[!@#\$%^&*]).{8,}$")
        return passwordPattern.matches(password)
    }
}