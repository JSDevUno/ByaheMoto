package com.example.byahemoto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView

class Wallet : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var walletTextView: TextView
    private lateinit var topUpLauncher: ActivityResultLauncher<Intent>
    private val TAG = "WalletActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        bottomNavigationView = findViewById(R.id.BottomNavigation)
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

        walletTextView = findViewById(R.id.walletValue)


        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e(TAG, "User ID is null. Exiting activity.")
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            loadWalletBalanceForCurrentUser(userId)
        }


        topUpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val addedAmount = result.data?.getDoubleExtra("added_amount", 0.0)
                addedAmount?.let {
                    Log.d(TAG, "Added amount: $it")
                    updateWalletBalance(it)
                    Toast.makeText(this, "Wallet updated: ₱$it", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Log.e(TAG, "Added amount is null.")
                }
            } else {
                Log.e(TAG, "Activity result not OK: $result")
            }
        }


        val cardTopup = findViewById<CardView>(R.id.Topup)
        cardTopup.setOnClickListener {
            val intent = Intent(this, Topup::class.java)
            topUpLauncher.launch(intent)
        }
        val cardTransac = findViewById<CardView>(R.id.transacHistory)
        cardTransac.setOnClickListener {
            val intent = Intent(this, TransacHistory::class.java)
            startActivity(intent)
        }
    }


    private fun loadWalletBalanceForCurrentUser(userId: Int) {
        Log.d(TAG, "Loading wallet balance for user ID: $userId")
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val walletBalance = sharedPref.getFloat("wallet_balance_$userId", 0.0f)
        walletTextView.text = String.format("%.2f", walletBalance)
        Log.d(TAG, "Loaded wallet balance: $walletBalance")
    }


    private fun updateWalletBalance(addedAmount: Double) {
        val userId = getCurrentUserId()
        if (userId != null) {
            val currentBalance = walletTextView.text.toString().toDoubleOrNull() ?: 0.0
            val newBalance = currentBalance + addedAmount
            walletTextView.text = String.format("%.2f", newBalance)
            Log.d(TAG, "Updated wallet balance: $newBalance")
            saveWalletBalanceForCurrentUser(newBalance, userId)
        } else {
            Log.e(TAG, "Error updating balance: User not found.")
            Toast.makeText(this, "Error updating balance: User not found.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveWalletBalanceForCurrentUser(balance: Double, userId: Int) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putFloat("wallet_balance_$userId", balance.toFloat())
            apply()
        }
        Log.d(TAG, "Saved wallet balance: $balance for user ID: $userId")
    }


    private fun getCurrentUserId(): Int? {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        return if (userId == -1) null else userId
    }
}
