package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loginAcc = findViewById<Button>(R.id.loginButton)

        loginAcc.setOnClickListener {
            val intent = Intent(this, UserDashboard::class.java)
            startActivity(intent)
        }

        val createAccountTextView = findViewById<TextView>(R.id.createAccount)
        createAccountTextView.setOnClickListener {
            val intent = Intent(this, CreateAccount::class.java)
            startActivity(intent)
        }
    }
}