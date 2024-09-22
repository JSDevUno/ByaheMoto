package com.example.byahemoto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.byahemoto.models.BookingRequest
import com.example.byahemoto.models.BookingResponse
import com.example.byahemoto.models.DriverLocationResponse
import com.example.byahemoto.models.LocData
import com.example.byahemoto.models.LocationData
import com.example.byahemoto.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Booking : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var paymentMethodSpinner: Spinner
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bookingButton: Button
    private lateinit var amountTextView: TextView
    private lateinit var statusPanel: TextView
    private var vehicleType: String = ""
    private var currentLocation: LatLng? = null
    private var driverLocation: LatLng? = null
    private var driverMarker: Marker? = null
    private var bookingId: Int? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        bottomNavigationView = findViewById(R.id.BottomNavigation)
        paymentMethodSpinner = findViewById(R.id.paymentMethodSpinner)
        amountTextView = findViewById(R.id.amount)
        statusPanel = findViewById(R.id.statusPanel)
        bookingButton = findViewById(R.id.bookingButton)

        vehicleType = intent.getStringExtra("vehicleType") ?: ""
        setupUI()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFrame) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

    private fun setupUI() {
        val paymentMethods = arrayOf("CASH", "PAYPAL")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        paymentMethodSpinner.adapter = adapter

        amountTextView.text = "0.0"

        bookingButton.setOnClickListener {
            createBooking()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            googleMap.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 12f))
                    Toast.makeText(this, "Your location fetched", Toast.LENGTH_SHORT).show()

                } ?: run {
                    Toast.makeText(this, "Unable to get current location.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBooking() {
        val paymentMethod = paymentMethodSpinner.selectedItem.toString()
        val locationToText = findViewById<EditText>(R.id.locationToTxt).text.toString()


        if (locationToText.isEmpty()) {
            Toast.makeText(this, "Please enter a drop-off location", Toast.LENGTH_SHORT).show()
            return
        }


        currentLocation?.let { currentLoc ->

            // Log the current location
            Log.d("CreateBooking", "Current location: $currentLoc")

            geocodeLocation(locationToText) { locationToLatLng ->
                if (locationToLatLng != null) {

                    // Log the geocoded location
                    Log.d("CreateBooking", "Geocoded location: $locationToLatLng")

                    val bookingRequest = BookingRequest(
                        paymentMethod = paymentMethod,
                        vehicleType = vehicleType,
                        locationFrom = LocData(currentLoc.latitude, currentLoc.longitude),
                        locationTo = LocData(locationToLatLng.latitude, locationToLatLng.longitude)
                    )

                    // Log the booking request
                    Log.d("BookingRequest", Gson().toJson(bookingRequest))

                    RetrofitInstance.getAuthService(this).createBooking(bookingRequest).enqueue(object : Callback<BookingResponse> {
                        override fun onResponse(call: Call<BookingResponse>, response: Response<BookingResponse>) {
                            if (response.isSuccessful) {
                                val bookingResponse = response.body()
                                bookingResponse?.let {
                                    // Log the booking response
                                    Log.d("BookingResponse", Gson().toJson(it))

                                    bookingId = it.bookingId
                                    bookingButton.text = "Booking in progress..."
                                    bookingButton.isEnabled = false

                                    val fare = it.fare
                                    amountTextView.text = fare.toString()

                                    trackBookingStatus()
                                    startDriverLocationUpdates()
                                } ?: run {
                                    Toast.makeText(this@Booking, "Empty booking response", Toast.LENGTH_SHORT).show()
                                    Log.e("BookingResponse", "Response body is null")
                                }
                            } else {
                                Toast.makeText(this@Booking, "Failed to create booking", Toast.LENGTH_SHORT).show()
                                Log.e("BookingResponse", "Error: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                            Toast.makeText(this@Booking, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            Log.e("BookingResponse", "Failure: ${t.message}", t)
                        }
                    })
                } else {
                    Toast.makeText(this, "Failed to get coordinates for the drop-off location", Toast.LENGTH_SHORT).show()
                    Log.e("CreateBooking", "Geocoding failed for location: $locationToText")
                }

            }
        } ?: run {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
            Log.e("CreateBooking", "Current location is null")
        }

    }

    private fun geocodeLocation(address: String, callback: (LatLng?) -> Unit) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GeocodingApiService::class.java)
        val apiKey = "AIzaSyA7TdMg8XawtIx9QX1uDGl2H_CSJU7IKpE"

        service.getCoordinates(address, apiKey).enqueue(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (response.isSuccessful && response.body()?.results?.isNotEmpty() == true) {
                    val location = response.body()!!.results[0].geometry.location
                    callback(LatLng(location.lat, location.lng))
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                Toast.makeText(this@Booking, "Geocoding failed: ${t.message}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        })
    }




    private fun trackBookingStatus() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                bookingId?.let { id ->
                    RetrofitInstance.getAuthService(this@Booking).getBookingDetails(id).enqueue(object : Callback<BookingResponse> {
                        override fun onResponse(call: Call<BookingResponse>, response: Response<BookingResponse>) {
                            if (response.isSuccessful) {
                                val status = response.body()?.status
                                statusPanel.text = status ?: "Unknown"

                                // Check if the driver has accepted the booking
                                if (status == "Accepted" || status == "Picked Up") {
                                    // Update the fare when the driver accepts the booking
                                    val fare = response.body()?.fare ?: 0.0
                                    amountTextView.text = fare.toString()
                                }

                                // Stop polling if the booking is either completed or cancelled
                                if (status == "Dropped" || status == "Cancelled") {
                                    handler.removeCallbacksAndMessages(null)
                                }
                            } else {
                                Toast.makeText(this@Booking, "Failed to track booking status", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<BookingResponse>, t: Throwable) {
                            Toast.makeText(this@Booking, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                handler.postDelayed(this, 5000)  // Polling every 5 seconds
            }
        }, 5000)
    }


    private fun drawRoute() {
        if (currentLocation != null && driverLocation != null) {
            fetchDirections(currentLocation!!, driverLocation!!)
        } else {
            Toast.makeText(this, "Current location or driver location not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDirections(start: LatLng, end: LatLng) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(DirectionsApiService::class.java)

        val origin = "${start.latitude},${start.longitude}"
        val destination = "${end.latitude},${end.longitude}"
        val directionsApiKey = "AIzaSyA7TdMg8XawtIx9QX1uDGl2H_CSJU7IKpE"

        service.getDirections(origin, destination, directionsApiKey).enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val route = response.body()?.routes?.get(0)
                    route?.let {
                        val decodedPath = decodePolyline(it.legs[0].steps)
                        googleMap.addPolyline(PolylineOptions().addAll(decodedPath))
                    }
                } else {
                    Toast.makeText(this@Booking, "Failed to get directions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Toast.makeText(this@Booking, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun decodePolyline(steps: List<Step>): List<LatLng> {
        val polylinePoints = mutableListOf<LatLng>()
        for (step in steps) {
            polylinePoints.addAll(decode(step.polyline.points))
        }
        return polylinePoints
    }

    private fun decode(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun startDriverLocationUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                bookingId?.let { id ->
                    RetrofitInstance.getAuthService(this@Booking).getDriverLocationUpdates(id).enqueue(object : Callback<DriverLocationResponse> {
                        override fun onResponse(call: Call<DriverLocationResponse>, response: Response<DriverLocationResponse>) {
                            if (response.isSuccessful) {
                                val locationResponse = response.body()
                                locationResponse?.let {
                                    driverLocation = LatLng(it.lat, it.lng)
                                    updateDriverLocationOnMap(driverLocation!!)
                                    drawRoute()
                                }
                            } else {
                                Toast.makeText(this@Booking, "Failed to get driver location", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<DriverLocationResponse>, t: Throwable) {
                            Toast.makeText(this@Booking, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                handler.postDelayed(this, 5000)  // Update every 5 seconds
            }
        }, 5000)
    }

    private fun updateDriverLocationOnMap(driverLocation: LatLng) {
        if (driverMarker == null) {
            driverMarker = googleMap.addMarker(MarkerOptions().position(driverLocation).title("Driver"))
        } else {
            driverMarker?.position = driverLocation
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 15f))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
