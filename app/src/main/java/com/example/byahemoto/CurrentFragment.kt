package com.example.byahemoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kotlin.math.*
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.cardview.widget.CardView
import com.example.byahemoto.models.BookingDetails
import com.example.byahemoto.models.RefreshTokenRequest
import com.example.byahemoto.models.RefreshTokenResponse
import com.example.byahemoto.models.UpdateDriverLocation
import com.example.byahemoto.network.AuthService
import com.example.byahemoto.network.RetrofitInstance
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.Locale

class CurrentFragment : Fragment(), OnMapReadyCallback {
    private val BASE_URL = "http://192.168.1.20:8000"
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var passengerCurrentLocation : LatLng
    private lateinit var passengerDestinationLocation : LatLng
    private val apiKey = "AIzaSyA7TdMg8XawtIx9QX1uDGl2H_CSJU7IKpE"

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_current, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val activity = activity ?: return view
        fusedLocationClient = activity.let { LocationServices.getFusedLocationProviderClient(it) }
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        if (isAdded) {
            googleMap = map
            if (context?.let {
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
                != PackageManager.PERMISSION_GRANTED
            ) {
                activity?.let {
                    ActivityCompat.requestPermissions(
                        it,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                    )
                }
            } else {
                // Get current location
                googleMap.isMyLocationEnabled = false
                getCurrentLocation()

            }
        }
    }

    @SuppressLint("DefaultLocale", "CommitPrefEdits")
    private fun getCurrentLocation() {
        if (!isAdded) return
        val context = context ?: return
        val activity = activity ?: return

        if (context.let { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) }
            == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val userPrefs = activity.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val selectedBookingIDFromOrders = userPrefs.getInt("bookingId", 0)
                    Log.i("!!!BOOKING ID!!!", selectedBookingIDFromOrders.toString())
                    val driverCurrentLocation = LatLng(location.latitude, location.longitude)

                    // DEFAULT LOCATION STATE
                    if (selectedBookingIDFromOrders == 0) {
                        defaultLocationState(driverCurrentLocation)
                        return@addOnSuccessListener
                    }

                    // DISPLAY BOOKING DETAILS
                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val authService = retrofit.create(AuthService::class.java)
                    authService.getDisplayBookingDetails("Bearer ${userPrefs.getString("access_token", "")}",
                        selectedBookingIDFromOrders).enqueue(object : Callback<BookingDetails> {
                        override fun onResponse(call: Call<BookingDetails>, response: Response<BookingDetails>) {
                            if (response.isSuccessful) {
                                val bookingDetails = response.body()
                                val token = getTokenFromPreferences()

                                val acceptButton = view?.findViewById<TextView>(R.id.accept_button)
                                val rejectButton = view?.findViewById<TextView>(R.id.reject_button)

                                if (bookingDetails != null) {
                                    // Get the passenger's current location and destination location
                                    passengerCurrentLocation = LatLng(bookingDetails.data.locationFrom.lat, bookingDetails.data.locationFrom.lng)
                                    passengerDestinationLocation = LatLng(bookingDetails.data.locationTo.lat, bookingDetails.data.locationTo.lng)

                                    // Accept or reject booking
                                    rejectButton?.setOnClickListener {
                                        authService.rejectBooking(token, selectedBookingIDFromOrders).enqueue(object : Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Booking Rejected", Toast.LENGTH_SHORT).show()
                                                    rejectButton.isEnabled = false
                                                } else {
                                                    Toast.makeText(context, "Error Rejecting Booking", Toast.LENGTH_SHORT).show()
                                                    Log.e("Reject Failed", response.errorBody()?.string() ?: "Unknown error")
                                                }
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                                Toast.makeText(context, "Error Rejecting Booking", Toast.LENGTH_SHORT).show()
                                                Log.e("Reject Error", t.message.toString())
                                            }
                                        })
                                    }
                                    acceptButton?.setOnClickListener {
                                        authService.acceptBooking(token, selectedBookingIDFromOrders).enqueue(object : Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Booking Accepted", Toast.LENGTH_SHORT).show()
                                                    acceptButton.isEnabled = false
                                                    // Navigate to the CurrentFragment
                                                    val intent = Intent(context, DriverDashboard::class.java)
                                                    intent.putExtra("navigateTo", "nav_current")
                                                    startActivity(intent)
                                                } else {
                                                    Toast.makeText(context, "Error Accepting Booking", Toast.LENGTH_SHORT).show()
                                                    Log.e("Accept Failed", response.errorBody()?.string() ?: "Unknown error")
                                                }
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {
                                                Toast.makeText(context, "Error Accepting Booking", Toast.LENGTH_SHORT).show()
                                                Log.e("Accept Error", t.message.toString())
                                            }
                                        })
                                    }

                                    // Display marker on the map base on the status of the booking
                                    val bookingStatus = response.body()!!.data.status
                                    when (bookingStatus) {

                                        "CANCELLED" -> {
                                            rejectButton?.visibility = View.INVISIBLE
                                            acceptButton?.visibility = View.INVISIBLE
                                            showLocationFromPassengerToDestination(driverCurrentLocation)
                                            showBookingDetails(userPrefs, bookingDetails)
                                        }
                                        "DROPPED" -> {
                                            rejectButton?.visibility = View.INVISIBLE
                                            acceptButton?.visibility = View.INVISIBLE
                                            showLocationFromPassengerToDestination(driverCurrentLocation)
                                            showBookingDetails(userPrefs, bookingDetails)
                                        }
                                        "REJECTED" -> {
                                            rejectButton?.visibility = View.INVISIBLE
                                            acceptButton?.visibility = View.INVISIBLE
                                            showLocationFromPassengerToDestination(driverCurrentLocation)
                                            showBookingDetails(userPrefs, bookingDetails)
                                        }

                                        "ACCEPTED" -> {
                                            rejectButton?.isEnabled = false
                                            acceptButton?.isEnabled = false
                                            showBookingDetails(userPrefs, bookingDetails)
                                            showLocationFromDriverToPassenger()
                                        }
                                        "PICKING" -> {
                                            rejectButton?.isEnabled = false
                                            acceptButton?.isEnabled = false
                                            showLocationFromDriverToPassenger()
                                            showBookingDetails(userPrefs, bookingDetails)
                                        }
                                        "PICKED_UP" -> {
                                            rejectButton?.isEnabled = false
                                            acceptButton?.isEnabled = false
                                            showLocationFromDriverToDestination()
                                            showBookingDetails(userPrefs, bookingDetails)
                                        }
                                        else -> {
                                            rejectButton?.isEnabled = true
                                            acceptButton?.isEnabled = true
                                            showLocationFromPassengerToDestination(driverCurrentLocation)
                                        }
                                    }
                                    showBookingDetails(userPrefs, bookingDetails)
                                } else {
                                    Log.d("BookingDetailsError", "Booking details response is null.")
                                }
                            } else {
                                Log.d("BookingDetailsError", "Booking details API request failed with status code: ${response.code()}")
                            }
                        }
                        override fun onFailure(call: Call<BookingDetails>, t: Throwable) {
                            Log.e("BookingDetailsError", "Error fetching booking details.", t)
                        }
                    })
                } else {
                    Toast.makeText(context, "Unable to get current location.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("PermissionError", "Location permission denied.")
        }
    }

    private fun acceptOrRejectBooking(booking: Int) {


    }

    @SuppressLint("DefaultLocale")
    private fun showBookingDetails(userPrefs: SharedPreferences, bookingDetails: BookingDetails){
        val distance = getTotalDistance(passengerCurrentLocation, passengerDestinationLocation)
        val averageSpeed = 20.0
        val fromLocationText = view?.findViewById<TextView>(R.id.fromLocationText)
        val toLocationText = view?.findViewById<TextView>(R.id.toLocationText)
        val distanceText = view?.findViewById<TextView>(R.id.distanceText)
        val arrivalTimeText = view?.findViewById<TextView>(R.id.arrivalTimeText)
        val clientNameText = view?.findViewById<TextView>(R.id.clientNameText)
        val statusText = view?.findViewById<TextView>(R.id.passengerStatusText)
        val estimatedTime = getEstimatedTime(distance, averageSpeed)

        arrivalTimeText?.text = String.format("%.2f hours", estimatedTime)
        distanceText?.text = String.format("%.2f km", distance)
        fromLocationText?.text = getPlaceName(passengerCurrentLocation)
        toLocationText?.text = getPlaceName(passengerDestinationLocation)
        clientNameText?.text = bookingDetails.data.user.fullName
        statusText?.text = bookingDetails.data.status
    }

    private fun defaultLocationState(driverCurrentLocation: LatLng) {
        if (!isAdded) return
        val context = context ?: return
        Toast.makeText(context, "Have a safe trip!", Toast.LENGTH_SHORT).show()
        fusedLocationClient = context.let { LocationServices.getFusedLocationProviderClient(it) }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locResult: LocationResult) {
                for (defaultLocation in locResult.locations) {
                    val driverLiveLocation = LatLng(defaultLocation.latitude, defaultLocation.longitude)
                    Log.d("DriverLocation", "Driver's location: $driverLiveLocation")
                    val authService = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AuthService::class.java)
                    val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val selectedBookingIDFromOrders = userPrefs.getInt("bookingId", 0)

                    if (selectedBookingIDFromOrders != 0) {
                        // DISPLAY BOOKING DETAILS
                        authService.getDisplayBookingDetails("Bearer ${userPrefs?.getString("access_token", "")}",
                            selectedBookingIDFromOrders).enqueue(object : Callback<BookingDetails> {
                            override fun onResponse(
                                call: Call<BookingDetails>,
                                response: Response<BookingDetails>
                            ) {
                                if (response.isSuccessful) {
                                    val bookingStatus = response.body()!!.data.status
                                    if (bookingStatus == "PICKING" || bookingStatus == "ACCEPTED") {
                                        fusedLocationClient.removeLocationUpdates(locationCallback)
                                        showLocationFromDriverToPassenger()
                                    }else{
                                        if (response.code() == 401) {
                                            Log.d("DriverLocationResponse", "Unauthorized access. Refreshing token...")
                                            refreshTheToken()
                                        } else {
                                            Log.e("DriverLocationResponse", "Error: ${response.errorBody()?.string()}")
                                        }
                                    }
                                }
                            }
                            override fun onFailure(call: Call<BookingDetails>, t: Throwable) {
                                Log.i("BookingDetailsResponse", "Error fetching booking details. Error: ${t.message.toString()}", t)
                            }
                        })
                    }
                    // LOCATE DRIVER
                    authService.locateDriver("Bearer ${userPrefs?.getString("access_token", "")}",
                        UpdateDriverLocation(driverLiveLocation.latitude, driverLiveLocation.longitude)).enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful) {
                                Log.i("DriverLocationResponse", "Driver's location updated successfully.")
                            } else {
                                if (response.code() == 401) {
                                    Log.d("DriverLocationResponse", "Unauthorized access. Refreshing token...")
                                    refreshTheToken()
                                } else {
                                    Log.e("DriverLocationResponse", "Error: ${response.errorBody()?.string()}")
                                }
                            }
                        }
                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.e("DriverLocationResponse", "Error updating driver's location.", t)
                        }
                    })
                }
            }
        }
        googleMap.clear()
        val yourLocation = googleMap.addMarker(MarkerOptions().position(driverCurrentLocation).title("Your Location"))
        yourLocation?.showInfoWindow()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverCurrentLocation, 15f))
        val selectedBookingDetailsID = view?.findViewById<CardView>(R.id.selectedBookingDetailsID)
        selectedBookingDetailsID?.visibility = View.INVISIBLE
        startLocationUpdates()
    }

    private fun showLocationFromDriverToPassenger() {
        if (!isAdded) return
        val context = context ?: return
        fusedLocationClient = context.let { LocationServices.getFusedLocationProviderClient(it) }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locResult: LocationResult) {
                val activity = activity ?: return
                val authService = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthService::class.java)
                val userPrefs = activity.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val selectedBookingIDFromOrders = userPrefs.getInt("bookingId", 0)

                for (location in locResult.locations) {
                    authService.getDisplayBookingDetails("Bearer ${userPrefs?.getString("access_token", "")}",
                        selectedBookingIDFromOrders).enqueue(object : Callback<BookingDetails> {
                        override fun onResponse(
                            call: Call<BookingDetails>,
                            response: Response<BookingDetails>
                        ) {
                            if (response.isSuccessful) {
                                val bookingStatus = response.body()!!.data.status
                                if (bookingStatus == "PICKED_UP") {
                                    Toast.makeText(context, "Passenger picked-up!", Toast.LENGTH_LONG).show()
                                    fusedLocationClient.removeLocationUpdates(locationCallback)
                                    showLocationFromDriverToDestination()
                                }
                            }
                        }
                        override fun onFailure(call: Call<BookingDetails>, t: Throwable) {
                            Log.i("BookingDetailsResponse", "Error fetching booking details. Error: ${t.message.toString()}", t)
                        }
                    })
                    val driverLiveLocation = LatLng(location.latitude, location.longitude)
                    updateLocationDriverToPassenger(driverLiveLocation)
                }
            }
        }
        startLocationUpdates()
    }

    private fun showLocationFromDriverToDestination(){
        if (!isAdded) {
            Log.e("FragmentError", "Fragment not attached to an activity.")
            return
        }
        val context = context ?: return
        fusedLocationClient = context.let { LocationServices.getFusedLocationProviderClient(it) }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locResult: LocationResult) {
                val activity = activity ?: return
                val authService = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthService::class.java)
                val userPrefs = activity.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val selectedBookingIDFromOrders = userPrefs.getInt("bookingId", 0)
                for (location in locResult.locations) {
                    authService.getDisplayBookingDetails("Bearer ${userPrefs?.getString("access_token", "")}",
                        selectedBookingIDFromOrders).enqueue(object : Callback<BookingDetails> {
                        override fun onResponse(
                            call: Call<BookingDetails>,
                            response: Response<BookingDetails>
                        ) {
                            if (response.isSuccessful) {
                                val bookingStatus = response.body()!!.data.status
                                if (bookingStatus == "DROPPED") {
                                    Toast.makeText(context, "You have arrived to destination!", Toast.LENGTH_LONG).show()
                                    Log.i("BookingDetailsResponse", "You have arrived to destination!")
                                    // Reset booking id to default
                                    val editor = userPrefs.edit()
                                    editor.putInt("bookingId", 0)
                                    editor.apply()
                                    // Restart the fragment to default state
                                    val fragment = CurrentFragment()
                                    val fragmentManager = activity.supportFragmentManager
                                    val fragmentTransaction = fragmentManager.beginTransaction()
                                    fragmentTransaction.replace(R.id.fragment_layout, fragment)
                                    fragmentTransaction.commitAllowingStateLoss()
                                    fusedLocationClient.removeLocationUpdates(locationCallback)
                                    getCurrentLocation()
                                }
                            }
                        }
                        override fun onFailure(call: Call<BookingDetails>, t: Throwable) {
                            Log.i("BookingDetailsResponse", "Error fetching booking details. Error: ${t.message.toString()}", t)
                        }
                    })
                    val driverLiveLocation = LatLng(location.latitude, location.longitude)
                    updateLocationDriverToDestination(driverLiveLocation)
                }
            }
        }
        startLocationUpdates()
    }

    private fun showLocationFromPassengerToDestination(driverCurrentLocation: LatLng) {
        if (!isAdded) return
        val context = context ?: return
        fusedLocationClient = context.let { LocationServices.getFusedLocationProviderClient(it) }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locResult: LocationResult) {
                for (location in locResult.locations) {
                    val activity = activity ?: return
                    val userPrefs = activity.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val selectedBookingIDFromOrders = userPrefs.getInt("bookingId", 0)
                    val authService = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AuthService::class.java)
                    authService.getDisplayBookingDetails("Bearer ${userPrefs?.getString("access_token", "")}", selectedBookingIDFromOrders).enqueue(object : Callback<BookingDetails> {
                        override fun onResponse(
                            call: Call<BookingDetails>,
                            response: Response<BookingDetails>
                        ) {
                            if (response.isSuccessful) {
                                val bookingStatus = response.body()!!.data.status
                                if (bookingStatus == "PICKING" || bookingStatus == "ACCEPTED") {
                                    Toast.makeText(context, "Passenger is waiting for pick-up!", Toast.LENGTH_LONG).show()
                                    fusedLocationClient.removeLocationUpdates(locationCallback)
                                    showLocationFromDriverToPassenger()
                                }
                            }
                        }
                        override fun onFailure(call: Call<BookingDetails>, t: Throwable) {
                            Log.i("BookingDetailsResponse", "Error fetching booking details. Error: ${t.message.toString()}", t)
                        }
                    })
                }
            }
        }
        startLocationUpdates()
        googleMap.clear()
        val yourLocation = googleMap.addMarker(MarkerOptions().position(driverCurrentLocation).title("Your Location"))
        yourLocation?.showInfoWindow()
        googleMap.addMarker(MarkerOptions().position(passengerCurrentLocation).title("Passenger's Location"))
        googleMap.addMarker(MarkerOptions().position(passengerDestinationLocation).title("Destination"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerCurrentLocation, 15f))
        fetchDirections(passengerCurrentLocation, passengerDestinationLocation)
    }

    private fun updateLocationDriverToDestination(currentLocation: LatLng) {
        if (!::passengerCurrentLocation.isInitialized) return Toast.makeText(context, "Passenger's location not initialized.", Toast.LENGTH_SHORT).show()
        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()
        editor?.putString("mapLocationFrom", currentLocation.toString())
        editor?.putString("mapLocationTo", passengerDestinationLocation.toString())
        editor?.apply()

        // Clear and update the map with new markers
        googleMap.clear()
        val yourLocation = googleMap.addMarker(MarkerOptions().position(currentLocation).title("Your Location"))
        yourLocation?.showInfoWindow()
        googleMap.addMarker(MarkerOptions().position(passengerDestinationLocation).title("Destination"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        fetchDirections(currentLocation, passengerDestinationLocation)
    }

    private fun updateLocationDriverToPassenger(currentLocation: LatLng) {
        if (!::passengerCurrentLocation.isInitialized) return Toast.makeText(context, "Passenger's location not initialized.", Toast.LENGTH_SHORT).show()
        val sharedPref = activity?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref?.edit()
        editor?.putString("mapLocationFrom", currentLocation.toString())
        editor?.putString("mapLocationTo", passengerCurrentLocation.toString())
        editor?.apply()

        googleMap.clear()
        val yourLocation = googleMap.addMarker(MarkerOptions().position(currentLocation).title("Your Location"))
        yourLocation?.showInfoWindow()
        googleMap.addMarker(MarkerOptions().position(passengerCurrentLocation).title("Passenger's Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        fetchDirections(currentLocation, passengerCurrentLocation)
    }

    private fun startLocationUpdates() {
        if (context?.let { ActivityCompat.checkSelfPermission(it, android.Manifest.permission.ACCESS_FINE_LOCATION) } != PackageManager.PERMISSION_GRANTED) {
            activity?.let { ActivityCompat.requestPermissions(it, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1) }
            return
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
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
                            val simplifiedPath = simplifyPolyline(decodedPath, 0.0005)  // Adjust tolerance value

                            polylineOptions.addAll(simplifiedPath)
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

    fun getEstimatedTime(distance: Double, speed: Double): Double {
        return distance / speed // Time in hours
    }

    fun getTotalDistance(origin: LatLng, destination: LatLng): Double {
        val R = 6371.0 // Radius of the Earth in kilometers

        val lat1 = origin.latitude
        val lon1 = origin.longitude
        val lat2 = destination.latitude
        val lon2 = destination.longitude

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c // Distance in kilometers
    }

    private fun getPlaceName(latLng: LatLng): String {
        val geocoder = context?.let { Geocoder(it, Locale.getDefault()) }
        return try {
            val addresses = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1)
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

    private fun getTokenFromPreferences(): String {
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        return "Bearer ${sharedPref?.getString("access_token", "") ?: ""}"
    }
    private fun simplifyPolyline(points: List<LatLng>, tolerance: Double): List<LatLng> {
        if (points.size < 3) return points

        val simplified = mutableListOf<LatLng>()
        var previousPoint = points.first()

        simplified.add(previousPoint)

        for (i in 1 until points.size - 1) {
            val currentPoint = points[i]
            val distance = distanceBetween(previousPoint, currentPoint)

            if (distance > tolerance) {
                simplified.add(currentPoint)
                previousPoint = currentPoint
            }
        }

        simplified.add(points.last())
        return simplified
    }

    private fun distanceBetween(p1: LatLng, p2: LatLng): Double {
        val latDiff = p2.latitude - p1.latitude
        val lngDiff = p2.longitude - p1.longitude
        return Math.sqrt(latDiff * latDiff + lngDiff * lngDiff)
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
            val dlat = if (result and 1 != 0) result.inv() shr 1 else result shr 1
            lat += dlat
            shift = 0
            result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) result.inv() shr 1 else result shr 1
            lng += dlng

            val pLat = (lat / 1E5).toDouble()
            val pLng = (lng / 1E5).toDouble()
            poly.add(LatLng(pLat, pLng))
        }

        return poly
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) }
                    == PackageManager.PERMISSION_GRANTED) {
                    googleMap.isMyLocationEnabled = true
                    getCurrentLocation()
                }
            } else {
                Log.d("PermissionError", "Location permission denied.")
            }
        }
    }

    private fun refreshTheToken() {
        Log.d("GETTING CREDENTIALS, ", "Getting credentials...")
        if (!isAdded) return
        val context = context ?: return
        val activity = activity ?: return
        Log.d("REFRESHING TOKEN", "Refreshing token...")
        val userPrefs = activity.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val authService = RetrofitInstance.getAuthService(context)
        val refreshToken = userPrefs.getString("refresh_token", "")
        refreshToken?.let { RefreshTokenRequest(it) }?.let {
            authService.refreshToken(it).enqueue(object : Callback<RefreshTokenResponse> {
                override fun onResponse(call: Call<RefreshTokenResponse>, response: Response<RefreshTokenResponse>) {
                    if (response.isSuccessful) {
                        val newAccessToken = response.body()?.data?.access_token
                        val editor = userPrefs.edit()
                        editor.putString("access_token", newAccessToken)
                        editor.apply()
                        Log.i("TOKENREFRESH", "Token refreshed successfully.")
                    } else {
                        Log.e("TOKENREFRESH", "Failed to refresh token.")
                    }
                }
                override fun onFailure(call: Call<RefreshTokenResponse>, t: Throwable) {
                    Log.e("TOKENREFRESH", "Error refreshing token.", t)
                }
            })
        }
        Log.d("END OF REFRESH", "End of refresh token.")
    }

}
