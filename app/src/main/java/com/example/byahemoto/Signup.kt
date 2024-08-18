package com.example.byahemoto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.RegisterRequest
import com.example.byahemoto.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Signup : AppCompatActivity() {
    private lateinit var selectButton: Button
    private lateinit var idVerification: TextView
    private lateinit var fullNameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var emailEditText: EditText
    private var idVerificationPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            onBackPressed()
        }

        selectButton = findViewById(R.id.selectButton)
        idVerification = findViewById(R.id.idVerification)
        fullNameEditText = findViewById(R.id.fullName1)
        usernameEditText = findViewById(R.id.username1)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.passwordEditText2)
        emailEditText = findViewById(R.id.email1)

        selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, FILE_PICKER_REQUEST)
        }

        val createAcc = findViewById<Button>(R.id.createAcc1)
        createAcc.setOnClickListener {
            registerPriorityAccount()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                idVerificationPath = uri.path
                idVerification.text = idVerificationPath
            }
        }
    }

    private fun registerPriorityAccount() {
        val fullName = fullNameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        if (validateInputs(fullName, username, password, confirmPassword, email)) {
            val request = RegisterRequest(
                full_name = fullName,
                username = username,
                email = email,
                password = password,
                confirm_password = confirmPassword,
                registration_type = "priority"
            )

            RetrofitInstance.authService.register(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Signup, "Registration successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@Signup, UserDashboard::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@Signup, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@Signup, "Error during registration: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun validateInputs(fullName: String, username: String, password: String, confirmPassword: String, email: String): Boolean {
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        if (idVerificationPath.isNullOrEmpty()) {
            Toast.makeText(this, "Please select an ID for verification", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    companion object {
        private const val FILE_PICKER_REQUEST = 1
    }
}
