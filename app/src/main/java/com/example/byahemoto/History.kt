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
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class History : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var listView: ListView
    private lateinit var rideHistoryAdapter: ArrayAdapter<String>
    private val rideHistoryList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_history)

        listView = findViewById(R.id.commuter_transaction_history)
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
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<RideHistoryResponse>, response: Response<RideHistoryResponse>) {
                    if (response.isSuccessful) {
                        val rideHistoryResponse = response.body()
                        val rideHistory = rideHistoryResponse?.data // This is the list of RideDetails

                        if (!rideHistory.isNullOrEmpty()) {
                            rideHistory.forEach { ride ->
                                val fromAddress = ride.locationFrom
                                val toAddress = ride.locationTo
                                val from = getPlaceName(LatLng(fromAddress.lat, fromAddress.lng))
                                val to = getPlaceName(LatLng(toAddress.lat, toAddress.lng))
                                val paymentMethod = ride.modeOfPayment
                                val fare = ride.fare
                                val vehicle = ride.vehicleType
                                val status = ride.status
                                val rawDate = ride.createdAt
                                val date = extractDate(rawDate)

                                val rideEntry = """
                                    Payment Method: $paymentMethod
                                    Vehicle Type: $vehicle
                                    Fare: ₱$fare
                                    From: $from
                                    To: $to
                                    Status: $status
                                    Date: $date
                                """.trimIndent()
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
    @RequiresApi(Build.VERSION_CODES.O)
    fun extractDate(rideDetails: String): String {
        val instant = Instant.parse(rideDetails)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDate = localDateTime.format(formatter)

        return formattedDate ?: "Date not found"
    }

    private fun getPlaceName(latLng: LatLng): String {

        val geocoder = Geocoder(this, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.getAddressLine(0) ?: "Unknown Location"
            } else {
                "Unknown Location"
            }
        } catch (e: IOException) {
            Log.i("GeocodingError", "Geocoding service not available")
            "Geocoding service not available"
        }
    }

    private fun getTokenFromSharedPreferences(): String? {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("access_token", null)
    }
}
