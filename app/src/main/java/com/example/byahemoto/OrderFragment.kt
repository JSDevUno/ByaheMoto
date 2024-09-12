package com.example.byahemoto

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.byahemoto.models.AvailableBooking
import com.example.byahemoto.models.BookingResponse
import com.example.byahemoto.models.DriverLocationResponse
import com.example.byahemoto.network.AuthService
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.security.cert.CertPathValidatorException.BasicReason


class OrderFragment : Fragment() {
    private val BASE_URL = "https://fond-beagle-prime.ngrok-free.app"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        getTokenForCurrentUser()
        listAvailableBookings()

        return inflater.inflate(R.layout.fragment_order, container
            , false)

    }

    private fun getTokenForCurrentUser(): String {
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        return sharedPref?.getString("access_token", "") ?: ""
    }

    private fun listAvailableBookings(){
    // Add new booking to the list


    }//END
}