package com.example.byahemoto

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.RegisterRequest
import com.example.byahemoto.models.SignupResponse
import com.example.byahemoto.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class PriorityDriver : AppCompatActivity() {
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
        setContentView(R.layout.activity_priority_driver)

        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            onBackPressed()
        }

        selectButton = findViewById(R.id.selectButtonDriver)
        idVerification = findViewById(R.id.idVerificationDriver)
        fullNameEditText = findViewById(R.id.fullName1Driver)
        usernameEditText = findViewById(R.id.username1Driver)
        emailEditText = findViewById(R.id.email1Driver)
        passwordEditText = findViewById(R.id.passwordEditTextDriver)
        confirmPasswordEditText = findViewById(R.id.passwordEditText2Driver)
        userTypeSpinner = findViewById(R.id.userTypeSpinnerDriver)

        val userTypeOptions = arrayOf("Driver")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userTypeOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userTypeSpinner.adapter = adapter

        selectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        val createAccButton = findViewById<Button>(R.id.createAcc1Driver)
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

            val registerRequest = RegisterRequest(
                fullName,
                username,
                email,
                password,
                confirmPassword,
                registration_type = "priority"
            )
            Log.d("Signup", "RegisterRequest: $registerRequest")

            val filePart = idVerificationUri?.let { uri ->
                val file = getFileFromUri(uri)
                if (file != null) {
                    Log.d("Signup", "File Path: ${file.absolutePath}")
                    file.asRequestBody("image/*".toMediaTypeOrNull()).let {
                        MultipartBody.Part.createFormData("file", file.name, it)
                    }
                } else {
                    Log.e("Signup", "File not found at the given URI")
                    null
                }
            }

            if (filePart == null) {
                Toast.makeText(
                    this,
                    "ID verification file not selected. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Log.d("Signup", "File Part: $filePart")

            // Retrieve token and pass it with requests
            RetrofitInstance.getAuthService(this).register(registerRequest)
                .enqueue(object : Callback<SignupResponse> {
                    override fun onResponse(
                        call: Call<SignupResponse>,
                        response: Response<SignupResponse>
                    ) {
                        Log.d(
                            "Signup",
                            "Register Response: ${response.code()} ${response.message()}"
                        )

                        if (!response.isSuccessful) {
                            Toast.makeText(
                                this@PriorityDriver,
                                "Failed to send verification request. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        val signupResponse = response.body()
                        val userId = signupResponse?.data?.id.toString()

                        Log.d("Signup", "User ID: $userId")

                        if (userId.isEmpty()) {
                            Toast.makeText(
                                this@PriorityDriver,
                                "User ID not found in the response.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        val identityType = userType.uppercase().toRequestBody(MultipartBody.FORM)

                        // Send the verification request with token
                        RetrofitInstance.getAuthService(this@PriorityDriver).sendVerificationRequest(
                            filePart,
                            userId.toRequestBody(MultipartBody.FORM),
                            identityType
                        ).enqueue(object : Callback<Void> {
                            override fun onResponse(
                                call: Call<Void>,
                                response: Response<Void>
                            ) {
                                Log.d(
                                    "Signup",
                                    "Verification Response: ${response.code()} ${response.message()}"
                                )
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        this@PriorityDriver,
                                        "Signup request and verification sent successfully! Await admin approval.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@PriorityDriver,
                                        "Failed to send verification request. Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                Log.e("Signup", "Verification Request Failed: ${t.message}", t)
                                Toast.makeText(
                                    this@PriorityDriver,
                                    "An error occurred: ${t.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    }

                    override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                        Log.e("Signup", "Signup Request Failed: ${t.message}", t)
                        Toast.makeText(
                            this@PriorityDriver,
                            "An error occurred: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            idVerificationUri = data.data
            idVerification.text = idVerificationUri?.toString()
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        val returnCursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val name = nameIndex?.let { returnCursor.getString(it) }
        returnCursor?.close()

        val file = File(cacheDir, name ?: "temp_file")
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
        } catch (e: Exception) {
            Log.e("Signup", "Failed to copy file from URI: ${e.message}", e)
            return null
        }
        return file
    }
}
