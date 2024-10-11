package com.example.byahemoto.models

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.byahemoto.network.AuthService
import com.example.byahemoto.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DriverLocationManager(private val authService: AuthService) : Fragment() {
    private val url = "http://192.168.1.20:8000"

    // Method to locate driver in the background every 5 seconds
    @OptIn(DelicateCoroutinesApi::class)
    fun startLocatingDriver(token: String, latLng: LatLng) {

        // Start a coroutine that runs on a background thread
        val updateDriverLocation = GlobalScope.launch(Dispatchers.IO) {

            while (true) {
                // Call the locateDriver method
                locateDriver(token, latLng)

                // Wait for 5 seconds before the next request
                delay(5000)

            }
        }
        updateDriverLocation.start()

    }

    // Method to send location to the API
    private fun locateDriver(token: String, latLng: LatLng) {
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(AuthService::class.java)

        // It sends the latitude and longitude of the driver to the UpdateDriverLocation data class
        val updateDriverLocation = UpdateDriverLocation(latLng.latitude, latLng.longitude)

        service.locateDriver(token, updateDriverLocation).enqueue(object : retrofit2.Callback<Void> {
            override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                if (response.isSuccessful) {
                    Log.i("SuccessDriverLocation", response.raw().message)

                } else {
                    response.errorBody()?.let { Log.e("ErrorDriverLocation", it.string()) }

                    // Detect 401 error
                    if (response.code() == 401) {
                        Log.e("ErrorDriverLocation", "Unauthorized access. Refreshing token...")
                    }
                }
            }
            override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                Log.e("FailureDriverLocation", t.message ?: "Failed to update driver location")
            }
        })
    }
}
