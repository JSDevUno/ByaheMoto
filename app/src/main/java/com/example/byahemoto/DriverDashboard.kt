package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class DriverDashboard : AppCompatActivity() {

    private lateinit var ordersTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        ordersTextView = findViewById(R.id.orders)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_current -> {
                    loadFragment(CurrentFragment())
                    // Access the shared preferences to reset the bookingID to zero
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putInt("bookingId", 0)
                    editor.apply()
                    updateOrdersTextView("CURRENT LOCATION")
                    true
                }
                R.id.nav_map -> {
                    loadFragment(MapFragment())
                    updateOrdersTextView("MAP")
                    true
                }
                R.id.nav_order -> {
                    loadFragment(OrderFragment())
                    updateOrdersTextView("ORDERS") 
                    true
                }
                R.id.nav_transac -> {
                    loadFragment(MenuFragment())
                    updateOrdersTextView("TRANSACTIONS")
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileDriver::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        // Check the intent for a specific fragment to navigate to
        val navigateTo = intent.getStringExtra("navigateTo")

        if (savedInstanceState == null) {

            when (navigateTo) {
                "nav_current" -> {
                    loadFragment(CurrentFragment())
                }
                else -> {
                    // Default behavior, load the CurrentFragment
                    loadFragment(CurrentFragment())
                }
            }
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_layout, fragment)
            .commit()
    }

    private fun updateOrdersTextView(text: String) {
        ordersTextView.text = text
    }
}
