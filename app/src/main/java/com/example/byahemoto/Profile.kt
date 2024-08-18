package com.example.byahemoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class Profile : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var logoutLayout: LinearLayout

    private val activityMap = mapOf(
        R.id.nav_home to UserDashboard::class.java,
        R.id.nav_history to History::class.java,
        R.id.nav_wallet to Wallet::class.java,
        R.id.nav_profile to Profile::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bottomNavigationView = findViewById(R.id.BottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_profile

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

        logoutLayout = findViewById(R.id.linearlayout)
        logoutLayout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        // Redirect to the login screen
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
