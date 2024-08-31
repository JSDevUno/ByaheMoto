package com.example.byahemoto

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.RegisterRequest
import com.example.byahemoto.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class Signup : AppCompatActivity() {
    private lateinit var selectButton: Button
    private lateinit var idVerification: TextView
    private lateinit var fullNameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var userTypeSpinner: Spinner
    private var idVerificationUri: Uri? = null

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
        emailEditText = findViewById(R.id.email1)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.passwordEditText2)
        userTypeSpinner = findViewById(R.id.userTypeSpinner)

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
            val email = emailEditText.text?.toString()?.trim() ?: ""
            val password = passwordEditText.text?.toString()?.trim() ?: ""
            val confirmPassword = confirmPasswordEditText.text?.toString()?.trim() ?: ""
            val userType = userTypeSpinner.selectedItem?.toString()?.trim() ?: ""

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val registerRequest = RegisterRequest(fullName, username, email, password, confirmPassword, registration_type = userType)
            Log.d("Signup", "RegisterRequest: $registerRequest")

            val userId = RequestBody.create(MultipartBody.FORM, "1")
            val filePart = idVerificationUri?.let { uri ->
                val file = File(uri.path!!)
                Log.d("Signup", "File Path: ${file.absolutePath}")
                val requestFile = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
                MultipartBody.Part.createFormData("file", file.name, requestFile)
            }

            if (filePart == null) {
                Toast.makeText(this, "ID verification file not selected. Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("Signup", "File Part: $filePart")

            RetrofitInstance.authService.register(registerRequest).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    Log.d("Signup", "Register Response: ${response.code()} ${response.message()}")
                    if (response.isSuccessful) {
                        val identityType = RequestBody.create(MultipartBody.FORM, userType)

                        RetrofitInstance.authService.sendVerificationRequest(filePart, userId, identityType).enqueue(object : Callback<Void> {
                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                Log.d("Signup", "Verification Response: ${response.code()} ${response.message()}")
                                if (response.isSuccessful) {
                                    Toast.makeText(this@Signup, "Signup request and verification sent successfully! Await admin approval.", Toast.LENGTH_SHORT).show()
                                    finish()
                                } else {
                                    Toast.makeText(this@Signup, "Failed to send verification request. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Log.e("Signup", "Verification Request Failed: ${t.message}", t)
                                Toast.makeText(this@Signup, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(this@Signup, "Failed to send signup request. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("Signup", "Signup Request Failed: ${t.message}", t)
                    Toast.makeText(this@Signup, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            idVerificationUri = data.data
            idVerification.text = idVerificationUri?.toString()
        }
    }
}
