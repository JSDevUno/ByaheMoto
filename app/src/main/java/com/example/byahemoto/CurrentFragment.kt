package com.example.byahemoto

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.cardview.widget.CardView
import com.example.byahemoto.network.AuthInterceptor
import com.example.byahemoto.models.BookingDetails
import com.example.byahemoto.models.DriverLocationManager
import com.example.byahemoto.models.TokenManager
import com.example.byahemoto.models.UpdateDriverLocation
import com.example.byahemoto.network.AuthService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            // Get current location
            googleMap.isMyLocationEnabled = false
            getCurrentLocation()
//           startToGetCurrentLocation()
        }
    }

//    // Method to locate driver in the background every 5 seconds
//    @OptIn(DelicateCoroutinesApi::class)
//    fun startToGetCurrentLocation() {
//
//        // Start a coroutine that runs on a background thread
//        GlobalScope.launch(Dispatchers.IO) {
//
//            while (true) {
//                // Call the getCurrentLocation method
//                getCurrentLocation()
//
//                // Wait for 5 seconds before the next request
//                delay(5000)
//            }
//        }
//    }

    @SuppressLint("DefaultLocale")
    private fun getCurrentLocation() {
        // Passenger's current location and destination location are default to zero
        passengerCurrentLocation = LatLng(1.1, 1.1)
        passengerDestinationLocation = LatLng(1.1, 1.1)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED)
        {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {

                    // Retrieve the bookingId from user_prefs in shared preferences
                    val userPrefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val selectedBookingIDFromOrders = userPrefs?.getInt("bookingId", 0)


                    // Get the selectedBookingDetailsID
                    val selectedBookingDetailsID = view?.findViewById<CardView>(R.id.selectedBookingDetailsID)

                    // Get driver's current location
                    val driverCurrentLocation = LatLng(location.latitude, location.longitude)

                    // It prevents going further at startup
                    if (selectedBookingIDFromOrders == 0) {
                        selectedBookingDetailsID?.visibility = View.INVISIBLE
                        val retrofit = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                        val authService = retrofit.create(AuthService::class.java)

                        // Create an instance of the AuthInterceptor and TokenManager classes to monitor (401 Unauthorized)
//                        val authInterceptor = AuthInterceptor(userPrefs)
//                        val okHttpClientBuilder = OkHttpClient.Builder()
//                            .addInterceptor(authInterceptor)  // Automatically add the access token to requests
//                        val retrofit = Retrofit.Builder()
//                            .baseUrl(BASE_URL)
//                            .addConverterFactory(GsonConverterFactory.create())
//                            .build()
//                        val authServiceForInterceptor = retrofit.create(AuthService::class.java)
//                        val tokenAuthenticator = TokenManager(authServiceForInterceptor, userPrefs)
//                        val okHttpClientForInterceptor = okHttpClientBuilder
//                            .authenticator(tokenAuthenticator)
//                            .build()
//                        val finalRetrofit = Retrofit.Builder()
//                            .baseUrl(BASE_URL)
//                            .client(okHttpClientForInterceptor)  // Use finalized OkHttpClient
//                            .addConverterFactory(GsonConverterFactory.create())
//                            .build()
//                        val finalAuthService = finalRetrofit.create(AuthService::class.java)


                        // It executes the startLocatingDriver method
                        val driverLocationManager = DriverLocationManager(authService)

                        // It sends the driver's current location to the API
                        driverLocationManager.startLocatingDriver(getTokenFromPreferences(), LatLng(location.latitude, location.longitude))
                        googleMap.clear()
                        googleMap.addMarker(MarkerOptions().position(driverCurrentLocation).title("Driver's Location"))
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverCurrentLocation, 10f))
                        return@addOnSuccessListener
                    }

                    // Show the selectedBookingDetailsID visible
                    selectedBookingDetailsID?.visibility = View.VISIBLE

                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    val authService = retrofit.create(AuthService::class.java)


                    authService.getDisplayBookingDetails("Bearer ${userPrefs?.getString("access_token", "") ?: ""}",
                        selectedBookingIDFromOrders!!).enqueue(object : Callback<BookingDetails> {
                        override fun onResponse(call: Call<BookingDetails>, response: Response<BookingDetails>) {
                            if (response.isSuccessful) {
                                val bookingDetails = response.body()
                                if (bookingDetails != null) {
                                    // Get the passenger's current location and destination location
                                    passengerCurrentLocation = LatLng(bookingDetails.data.locationFrom.lat, bookingDetails.data.locationFrom.lng)
                                    passengerDestinationLocation = LatLng(bookingDetails.data.locationTo.lat, bookingDetails.data.locationTo.lng)

                                    // Display booking info to current location page of driver
                                    val distance = getTotalDistance(passengerCurrentLocation, passengerDestinationLocation)
                                    val averageSpeed = 20.0 // Average speed in km/h
                                    val fromLocationText = view?.findViewById<TextView>(R.id.fromLocationText)
                                    val toLocationText = view?.findViewById<TextView>(R.id.toLocationText)
                                    val distanceText = view?.findViewById<TextView>(R.id.distanceText)
                                    val arrivalTimeText = view?.findViewById<TextView>(R.id.arrivalTimeText)
                                    val clientNameText = view?.findViewById<TextView>(R.id.clientNameText)
                                    val statusText = view?.findViewById<TextView>(R.id.passengerStatusText)

                                    // Calculate estimated time
                                    val estimatedTime = getEstimatedTime(distance, averageSpeed) // Time in hours

                                    // Display marker on the map base on the status of the booking
                                    if (response.body()!!.data.status == "ACCEPTED" || response.body()!!.data.status == "PICKING") {
                                        showLocationFromDriverToPassenger(driverCurrentLocation)
                                    } else {
                                        showLocationFromPassengerToDestination()
                                    }

                                    // Apply the values to the text views
                                    arrivalTimeText?.text = String.format("%.2f hours", estimatedTime)
                                    distanceText?.text = String.format("%.2f km", distance)
                                    fromLocationText?.text = getPlaceName(passengerCurrentLocation)
                                    toLocationText?.text = getPlaceName(passengerDestinationLocation)
                                    clientNameText?.text = userPrefs.getString("username", "Unknown")
                                    statusText?.text = bookingDetails.data.status

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
                    Toast.makeText(requireContext(), "Unable to get current location.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.d("PermissionError", "Location permission denied.")
        }
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
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
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

    private fun getTokenFromPreferences(): String {
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        return "Bearer ${sharedPref?.getString("access_token", "") ?: ""}"
    }

    private fun showLocationFromDriverToPassenger(currentLocation: LatLng) {
        // Save the current location of the driver to user_prefs in shared preferences
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref?.edit()
        editor?.putString("mapLocationFrom", currentLocation.toString())
        editor?.putString("mapLocationTo", passengerCurrentLocation.toString())
        editor?.apply()

        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(passengerCurrentLocation).title("Passenger's Location"))
        googleMap.addMarker(MarkerOptions().position(currentLocation).title("Driver's Location"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 10f))
        fetchDirections(currentLocation, passengerCurrentLocation)
    }

    private fun showLocationFromPassengerToDestination() {
        // Save the current location of the driver to user_prefs in shared preferences
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        val editor = sharedPref?.edit()
        editor?.putString("mapLocationFrom", passengerCurrentLocation.toString())
        editor?.putString("mapLocationTo", passengerDestinationLocation.toString())
        editor?.apply()

        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(passengerCurrentLocation).title("Passenger's Location"))
        googleMap.addMarker(MarkerOptions().position(passengerDestinationLocation).title("Destination"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(passengerCurrentLocation, 10f))
        fetchDirections(passengerCurrentLocation, passengerDestinationLocation)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    googleMap.isMyLocationEnabled = true
                    getCurrentLocation()
                }
            } else {
                Log.d("PermissionError", "Location permission denied.")
            }
        }
    }
}
