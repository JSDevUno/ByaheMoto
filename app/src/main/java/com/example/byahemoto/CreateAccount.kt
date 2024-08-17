package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.RegisterRequest
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccount : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var fullnameEditText: EditText  // Changed to fullname
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var createAccButton: Button
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        emailEditText = findViewById(R.id.email)
        fullnameEditText = findViewById(R.id.fullname)  // Changed to fullname
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.passwordEditText2)
        createAccButton = findViewById(R.id.createAcc)
        backBtn = findViewById(R.id.backBtn)

        createAccButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val fullname = fullnameEditText.text.toString().trim()  // Changed to fullname
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || fullname.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                register(email, fullname, username, password, confirmPassword)
            }
        }

        backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun register(email: String, fullname: String, username: String, password: String, confirmPassword: String) {
        val registerRequest = RegisterRequest(fullname, username, email, password, confirmPassword)
        Log.d("CreateAccount", "Register Payload: $registerRequest")

        RetrofitInstance.authService.register(registerRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateAccount, "Registration successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CreateAccount, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@CreateAccount, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("CreateAccount", "Error during registration", t)
                Toast.makeText(this@CreateAccount, "Registration failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
