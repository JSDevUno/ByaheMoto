package com.example.byahemoto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response

class Booking : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var paymentMethodSpinner: Spinner
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val batangas = LatLng(13.7563, 121.0604)
    private val apiKey = "AIzaSyA7TdMg8XawtIx9QX1uDGl2H_CSJU7IKpE"

    private val activityMap = mapOf(
        R.id.nav_home to UserDashboard::class.java,
        R.id.nav_history to History::class.java,
        R.id.nav_wallet to Wallet::class.java,
        R.id.nav_profile to Profile::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)
        paymentMethodSpinner = findViewById(R.id.paymentMethodSpinner)
        val paymentMethods = arrayOf("CASH", "PAYPAL")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodSpinner.adapter = adapter


        bottomNavigationView = findViewById(R.id.BottomNavigation)
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

        // Initialize map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFrame) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            // Get current location
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLocation = LatLng(location.latitude, location.longitude)
                    showLocation(currentLocation)
                } else {
                    Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLocation(currentLocation: LatLng) {
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(currentLocation).title("Current Location"))
        googleMap.addMarker(MarkerOptions().position(batangas).title("Batangas"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10f))
        fetchDirections(currentLocation, batangas)
    }

    private fun fetchDirections(start: LatLng, end: LatLng) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(DirectionsApiService::class.java)

        val origin = "${start.latitude},${start.longitude}"
        val destination = "${end.latitude},${end.longitude}"

        service.getDirections(origin, destination, apiKey).enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    if (directionsResponse != null) {
                        val polylineOptions = PolylineOptions().color(0xFF0000FF.toInt()).width(5f)
                        directionsResponse.routes.firstOrNull()?.legs?.firstOrNull()?.steps?.forEach { step ->
                            val points = step.polyline.points
                            val decodedPath = decodePolyline(points)
                            polylineOptions.addAll(decodedPath)
                        }
                        googleMap.addPolyline(polylineOptions)
                    } else {
                        Log.d("DirectionsError", "Directions response is null.")
                    }
                } else {
                    Log.d("DirectionsError", "Directions API request failed with status code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Error fetching directions.", t)
            }
        })
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(
                (lat / 1E5).toDouble(),
                (lng / 1E5).toDouble()
            )
            poly.add(p)
        }

        return poly
    }
}
