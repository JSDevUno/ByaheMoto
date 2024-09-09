package com.example.byahemoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.ErrorResponse
import com.example.byahemoto.models.LoginResponse
import com.example.byahemoto.models.Role
import com.example.byahemoto.network.RetrofitInstance
import com.google.gson.Gson
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountTextView: TextView
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var rememberMeCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        createAccountTextView = findViewById(R.id.createAccount)
        forgotPasswordTextView = findViewById(R.id.forgot_password)
        rememberMeCheckBox = findViewById(R.id.remember_me_checkbox)

        if (loadSavedCredentials()) {
            navigateToDashboard()
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            } else {
                login(username, password)
            }
        }

        createAccountTextView.setOnClickListener {
            val intent = Intent(this, CreateAccount::class.java)
            startActivity(intent)
        }
        forgotPasswordTextView.setOnClickListener {
            val intent = Intent(this, forgot_password::class.java)
            startActivity(intent)
        }
    }

    private fun login(username: String, password: String) {
        RetrofitInstance.getAuthService(this).login(username.toRequestBody(), password.toRequestBody())
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()

                        if (loginResponse?.user?.role != Role.USER) {
                            handleLoginError(null)
                            return
                        }

                        // Log success
                        Log.d("UserLogin", "Login Response: ${loginResponse.toString()}")

                        if (rememberMeCheckBox.isChecked) {
                            saveCredentials(username, password)
                        } else {
                            clearSavedCredentials()
                        }

                        saveUserDetails(loginResponse) // Save user details and token
                        navigateToDashboard() // Go to the dashboard
                    } else {
                        handleLoginError(response.errorBody())
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("MainActivity", "Error logging in", t)
                    Toast.makeText(this@MainActivity, "Login failed: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveUserDetails(loginResponse: LoginResponse) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("access_token", loginResponse.access_token)
            putString("refresh_token", loginResponse.refresh_token)
            putInt("user_id", loginResponse.user.id)
            putString("username", loginResponse.user.username)
            putString("email", loginResponse.user.email)
            putString("phone_number", loginResponse.user.phone_number)
            putString("registration_type", loginResponse.user.registration_type)
            apply()
        }
    }

    private fun saveCredentials(username: String, password: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", username)
            putString("password", password)
            apply()
        }
    }

    private fun loadSavedCredentials(): Boolean {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedUsername = sharedPref.getString("username", null)
        val savedPassword = sharedPref.getString("password", null)

        return if (!savedUsername.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            usernameEditText.setText(savedUsername)
            passwordEditText.setText(savedPassword)
            rememberMeCheckBox.isChecked = true
            true
        } else {
            false
        }
    }

    private fun clearSavedCredentials() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear() // Clear all user-specific data
            apply()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, UserDashboard::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleLoginError(errorBody: ResponseBody?) {
        errorBody?.let {
            try {
                val json = Gson().fromJson(it.string(), ErrorResponse::class.java)
                Toast.makeText(this, json.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Login Failed.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Login Failed.", Toast.LENGTH_SHORT).show()
        }
    }
}
