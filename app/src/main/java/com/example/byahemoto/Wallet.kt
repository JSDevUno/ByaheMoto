package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.cardview.widget.CardView

class Wallet : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    private val activityMap = mapOf(
        R.id.nav_home to UserDashboard::class.java,
        R.id.nav_history to History::class.java,
        R.id.nav_wallet to Wallet::class.java,
        R.id.nav_profile to Profile::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        bottomNavigationView = findViewById(R.id.BottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_wallet

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val activityClass = activityMap[menuItem.itemId]
            if (activityClass != null) {
                val intent = Intent(this, activityClass)
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // Initialize CardViews
        val cardTopup = findViewById<CardView>(R.id.Topup)
        val cardTransacHistory = findViewById<CardView>(R.id.transacHistory)

        // Set click listener for Topup CardView
        cardTopup.setOnClickListener {
            val intent = Intent(this, Topup::class.java)
            startActivity(intent)
        }

        // Set click listener for Transaction History CardView
        cardTransacHistory.setOnClickListener {
            val intent = Intent(this, TransacHistory::class.java)
            startActivity(intent)
        }
    }
}
