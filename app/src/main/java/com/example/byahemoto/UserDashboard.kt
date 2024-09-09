package com.example.byahemoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.cardview.widget.CardView

class UserDashboard : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var card1: CardView
    private lateinit var card2: CardView
    private lateinit var card3: CardView
    private lateinit var card4: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        bottomNavigationView = findViewById(R.id.BottomNavigation)

        card1 = findViewById(R.id.EMC)
        card2 = findViewById(R.id.ECART)
        card3 = findViewById(R.id.MOTORCYCLE)
        card4 = findViewById(R.id.TRICYCLE)

        card1.setOnClickListener { openBookingActivity("EMC") }
        card2.setOnClickListener { openBookingActivity("ECART") }
        card3.setOnClickListener { openBookingActivity("MOTORCYCLE") }
        card4.setOnClickListener { openBookingActivity("TRICYCLE") }

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, UserDashboard::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, History::class.java))
                    true
                }
                R.id.nav_wallet -> {
                    startActivity(Intent(this, Wallet::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun openBookingActivity(vehicleType: String) {
        val intent = Intent(this, Booking::class.java)
        intent.putExtra("vehicleType", vehicleType)
        startActivity(intent)
    }
}
