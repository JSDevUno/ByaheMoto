package com.example.byahemoto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class SignupDriver : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_driver)
        val backBtnDriver = findViewById<ImageView>(R.id.backBtnDriver)
        backBtnDriver.setOnClickListener {
            onBackPressed()
        }
    }
}