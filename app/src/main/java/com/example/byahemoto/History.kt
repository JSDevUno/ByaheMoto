package com.example.byahemoto

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.byahemoto.models.RideHistoryResponse
import com.example.byahemoto.network.RetrofitInstance
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log

class History : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var listView: ListView
    private lateinit var rideHistoryAdapter: ArrayAdapter<String>
    private val rideHistoryList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_history)

        listView = findViewById(R.id.list_view)
        rideHistoryAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rideHistoryList)
        listView.adapter = rideHistoryAdapter

        bottomNavigationView = findViewById(R.id.BottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_history

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, UserDashboard::class.java))
                    true
                }
                R.id.nav_history -> true
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

        // Load ride history from the server
        loadRideHistory()
    }

    private fun loadRideHistory() {
        val token = getTokenFromSharedPreferences() // Retrieve the token

        if (token != null) {
            RetrofitInstance.getAuthService(this).getRideHistory("Bearer $token").enqueue(object : Callback<RideHistoryResponse> {
                override fun onResponse(call: Call<RideHistoryResponse>, response: Response<RideHistoryResponse>) {
                    if (response.isSuccessful) {
                        val rideHistoryResponse = response.body()
                        val rideHistory = rideHistoryResponse?.data // This is the list of RideDetails

                        if (rideHistory != null && rideHistory.isNotEmpty()) {
                            rideHistory.forEach { ride ->
                                val fromAddress = ride.locationFrom.address ?: getAddressFromLatLng(ride.locationFrom.lat, ride.locationFrom.lng)
                                val toAddress = ride.locationTo.address ?: getAddressFromLatLng(ride.locationTo.lat, ride.locationTo.lng)
                                val rideEntry = "FROM $fromAddress TO $toAddress"
                                rideHistoryList.add(rideEntry)
                            }
                        } else {
                            rideHistoryList.add("No ride history available.")
                        }

                        rideHistoryAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@History, "Failed to load ride history", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<RideHistoryResponse>, t: Throwable) {
                    Toast.makeText(this@History, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Token not available. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getAddressFromLatLng(lat: Double, lng: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                "Unknown Location"
            }
        } catch (e: IOException) {
            "Unknown Location"
        }
    }

    private fun getTokenFromSharedPreferences(): String? {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("access_token", null)
    }
}
