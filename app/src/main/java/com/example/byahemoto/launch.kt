package com.example.byahemoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Bundle
import android.os.Looper

class launch : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@launch, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}