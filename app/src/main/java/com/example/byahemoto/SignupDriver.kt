package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.RegisterDriverRequest
import com.example.byahemoto.models.SignupDriverResponse
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

class SignupDriver : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var fullNameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var createAccButton: Button
    private lateinit var backBtnDriver: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_driver)

        val vehicleTypeSpinner: Spinner = findViewById(R.id.vehicle_type_spinner)
        val vehicleTypes = arrayOf("EMC","ECART","MOTORCYCLE","TRICYCLE")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vehicleTypeSpinner.adapter = adapter

        emailEditText = findViewById(R.id.emailDriver)
        fullNameEditText = findViewById(R.id.fullNameDriver)
        usernameEditText = findViewById(R.id.usernameDriver)
        passwordEditText = findViewById(R.id.passwordDriver1)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordDriver1)
        createAccButton = findViewById(R.id.createAccDriver)
        backBtnDriver = findViewById(R.id.backBtnDriver)
        createAccButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val fullName = fullNameEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (validateInputs(email, fullName, username, password, confirmPassword)) {
                registerDriver(email, fullName, username, password, confirmPassword, vehicleTypeSpinner.selectedItem.toString())
            }
        }
        backBtnDriver.setOnClickListener {
            onBackPressed()
        }
    }

    private fun validateInputs(
        email: String,
        fullName: String,
        username: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Full name is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (username.isEmpty() || !Pattern.matches("^[a-zA-Z0-9_]+$", username)) {
            Toast.makeText(
                this,
                "Username must be alphanumeric with no spaces or special characters",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (password.isEmpty() || !Pattern.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&*!()_+\\-=\\[\\]{}|;:,.<>?/]).{8,}\$",
                password
            )
        ) {
            Toast.makeText(
                this,
                "Password must be at least 8 characters long with a mix of upper/lowercase letters, numbers, and symbols",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun registerDriver(
        email: String,
        fullName: String,
        username: String,
        password: String,
        confirmPassword: String,
        vehicleType: String
    ) {
        val registrationType = "driver" // Set registration type to "driver"
        val registerRequest = RegisterDriverRequest(
            fullName = fullName,
            username = username,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            registrationType = registrationType,
            vehicleType = vehicleType
        )
        Log.d("SignupDriver", "Register Request: $registerRequest")

        // Ensure context is passed to RetrofitInstance
        RetrofitInstance.getAuthService(this).registerDriver(registerRequest)
            .enqueue(object : Callback<SignupDriverResponse> {
                override fun onResponse(
                    call: Call<SignupDriverResponse>,
                    response: Response<SignupDriverResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@SignupDriver,
                            "Registration successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this@SignupDriver, DriverLogin::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(
                            "SignupDriver",
                            "Registration failed: ${response.message()}, Error: $errorBody"
                        )
                        Toast.makeText(
                            this@SignupDriver,
                            "Registration failed: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<SignupDriverResponse>, t: Throwable) {
                    Log.e("SignupDriver", "Error during registration", t)
                    Toast.makeText(
                        this@SignupDriver,
                        "Registration failed: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
