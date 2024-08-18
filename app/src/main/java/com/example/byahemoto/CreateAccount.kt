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
import java.util.regex.Pattern

class CreateAccount : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var fullnameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var createAccButton: Button
    private lateinit var createPriorityAccButton: Button
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        emailEditText = findViewById(R.id.email)
        fullnameEditText = findViewById(R.id.fullname)
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.passwordEditText2)
        createAccButton = findViewById(R.id.createAcc)
        createPriorityAccButton = findViewById(R.id.createPriorityAcc)
        backBtn = findViewById(R.id.backBtn)

        createAccButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val fullname = fullnameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (validateInputs(email, fullname, username, password, confirmPassword)) {
                register(email, fullname, username, password, confirmPassword)
            }
        }
        createPriorityAccButton.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun validateInputs(email: String, fullname: String, username: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fullname.isEmpty()) {
            Toast.makeText(this, "Full name is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (username.isEmpty() || !Pattern.matches("^[a-zA-Z0-9_]+$", username)) {
            Toast.makeText(this, "Username must be alphanumeric with no spaces or special characters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty() || !Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&*!()_+\\-=\\[\\]{}|;:,.<>?/]).{8,}\$", password)) {
            Toast.makeText(this, "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one number, and one symbol", Toast.LENGTH_LONG).show()
            return false
        }

        if (confirmPassword.isEmpty() || password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun register(email: String, fullName: String, username: String, password: String, confirmPassword: String) {
        val registerRequest = RegisterRequest(fullName, username, email, password, confirmPassword)
        RetrofitInstance.authService.register(registerRequest).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@CreateAccount, "Registration successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CreateAccount, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    if (response.code() == 400) {
                        Toast.makeText(this@CreateAccount, "Email already exists. Please use a different email.", Toast.LENGTH_LONG).show()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("CreateAccount", "Registration failed: ${response.message()}, Error: $errorBody")
                        Toast.makeText(this@CreateAccount, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("CreateAccount", "Error during registration", t)
                Toast.makeText(this@CreateAccount, "Registration failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


}