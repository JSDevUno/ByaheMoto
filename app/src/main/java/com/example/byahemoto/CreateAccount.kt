package com.example.byahemoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

class CreateAccount : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val createPriorityButton = findViewById<Button>(R.id.createPriority)

        createPriorityButton.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener {
            onBackPressed()
        }
    }
}