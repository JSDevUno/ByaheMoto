package com.example.byahemoto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.SignupRequest
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
    private lateinit var userTypeSpinner: Spinner
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
        userTypeSpinner = findViewById(R.id.userTypeSpinner)

        // Set up the Spinner
        val userTypeOptions = arrayOf("Student", "Senior", "PWD")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userTypeSpinner.adapter = adapter

        selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        val createAccButton = findViewById<Button>(R.id.createAcc1)
        createAccButton.setOnClickListener {
            val fullName = fullNameEditText.text?.toString()?.trim() ?: ""
            val username = usernameEditText.text?.toString()?.trim() ?: ""
            val password = passwordEditText.text?.toString()?.trim() ?: ""
            val confirmPassword = confirmPasswordEditText.text?.toString()?.trim() ?: ""
            val userType = userTypeSpinner.selectedItem?.toString()?.trim() ?: ""

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idVerificationPathSafe = idVerificationPath ?: ""
            val signupRequest = SignupRequest(fullName, username, password, idVerificationPathSafe, userType)

            val retrofitInstance = RetrofitInstance.authService
            retrofitInstance.sendSignupRequest(signupRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Signup, "Signup request sent successfully! Await admin approval.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@Signup, "Failed to send signup request. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(this@Signup, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            idVerificationPath = data.data.toString()
            idVerification.text = idVerificationPath
        }
    }
}

//tentative
