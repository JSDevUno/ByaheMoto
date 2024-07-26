package com.example.byahemoto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView

class MainLauncher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_launcher)

        val textView = findViewById<TextView>(R.id.textViewByahe)
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

        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideInFromRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_right)
        val slideInFromLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_left)

        textView.startAnimation(fadeInAnimation)
        commuterButton.startAnimation(slideInFromRightAnimation)
        driverButton.startAnimation(slideInFromLeftAnimation)
    }
}