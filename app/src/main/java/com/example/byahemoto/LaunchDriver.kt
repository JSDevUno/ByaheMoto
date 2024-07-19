package com.example.byahemoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class LaunchDriver : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch_driver)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@LaunchDriver, DriverLogin::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}