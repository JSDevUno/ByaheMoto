package com.example.byahemoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainLauncher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_launcher)
        val commuterButton = findViewById<Button>(R.id.commuter)
        val driverButton = findViewById<Button>(R.id.driver)

        commuterButton.setOnClickListener {
            val intent = Intent(this, launch::class.java)
            startActivity(intent)
        }

        driverButton.setOnClickListener {
            val intent = Intent(this, LaunchDriver::class.java)
            startActivity(intent)
        }
    }
}