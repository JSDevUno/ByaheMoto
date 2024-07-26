package com.example.byahemoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class DriverLogin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)

        val loginAcc = findViewById<Button>(R.id.loginButtonDriver)

        loginAcc.setOnClickListener {
            val intent = Intent(this, DriverDashboard::class.java)
            startActivity(intent)
        }
        val createAccountTextView = findViewById<TextView>(R.id.createAccountDriver)
        createAccountTextView.setOnClickListener {
            val intent = Intent(this, SignupDriver::class.java)
            startActivity(intent)
        }
    }
}