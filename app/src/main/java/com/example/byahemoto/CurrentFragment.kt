package com.example.byahemoto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class CurrentFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val batangas = LatLng(13.7563, 121.0604)
    private val bauang = LatLng(13.7963, 120.9762)
    private val apiKey = "AIzaSyA7TdMg8XawtIx9QX1uDGl2H_CSJU7IKpE"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_current, container, false)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            // Use fixed location
            googleMap.isMyLocationEnabled = false
            showLocation()
        }
    }

    private fun showLocation() {
        googleMap.clear()
        googleMap.addMarker(MarkerOptions().position(batangas).title("Batangas"))
        googleMap.addMarker(MarkerOptions().position(bauang).title("Bauan"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(batangas, 10f))
        fetchDirections(batangas, bauang)
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
                    showLocation()
                }
            } else {
                Log.d("PermissionError", "Location permission denied.")
            }
        }
    }
}
