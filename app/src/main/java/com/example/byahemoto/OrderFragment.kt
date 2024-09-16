package com.example.byahemoto

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.example.byahemoto.models.AvailableBooking
import com.example.byahemoto.network.AuthService
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class OrderFragment : Fragment() {
    private val BASE_URL = "https://fond-beagle-prime.ngrok-free.app"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        listAvailableBookings()

        return inflater.inflate(R.layout.fragment_order, container
            , false)

    }

    private fun getTokenFromPreferences(): String {
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        return "Bearer ${sharedPref?.getString("access_token", "") ?: ""}"
    }

    private fun makeRequest(url : String) {
        val client = OkHttpClient()
        val token = getTokenFromPreferences()

        CoroutineScope(Dispatchers.IO).launch {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token") // Add your token here
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body?.string()
                Log.i("Response", responseBody ?: "Empty response")
            } catch (e: IOException) {
                Log.i("Error", e.message ?: "Unknown error")
            }
        }
    }

    private fun refreshToken(url: String) {
        val client = OkHttpClient()
        val sharedPreferences = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        val refreshToken = sharedPreferences?.getString("refresh_token", null)

        if (refreshToken != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val request = Request.Builder()
                    .url("$url/auth/refresh-token")
                    .addHeader("Authorization", "Bearer $refreshToken")
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val newToken = response.body?.string()
                        sharedPreferences.edit().putString("auth_token", newToken).apply()
                        Log.i("New Token", newToken ?: "Empty token")
                    } else {
                        throw IOException("Unexpected code $response")
                    }
                } catch (e: IOException) {
                    Log.i("Error", e.message ?: "Unknown error")
                }
            }
        } else {
            Log.e("AuthError", "No refresh token available")
        }
    }

    private fun listAvailableBookings(){
    // Add new booking to the list
        refreshToken(BASE_URL)
        makeRequest(BASE_URL)

        val token = getTokenFromPreferences()
        Log.i("Token", token)
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(AuthService::class.java)

        service.getAvailableBookings(token).enqueue(object : Callback<List<AvailableBooking>> {
            override fun onResponse(
                call: Call<List<AvailableBooking>>,
                response: Response<List<AvailableBooking>>
            ) {
                if (response.isSuccessful) {
                    val bookings = response.body()

                    if (bookings != null) {
                        for (booking in bookings) {
                            Toast.makeText(context, "Bookings Fetched!", Toast.LENGTH_SHORT).show()
                            Log.d("Bookings Fetched", booking.toString())

                            // Assuming booking is an object with properties you want to display
                            Log.d("Booking Details", "Vehicle Type: ${booking.vehicleType}, " +
                                    "Fare: ${booking.fare}, " +
                                    "Status: ${booking.status}, " +
                                    "Location From: ${booking.locationFrom.lat}, ${booking.locationFrom.lng}, " +
                                    "Location To: ${booking.locationTo.lat}, ${booking.locationTo.lng}, " +
                                    "Mode of Payment: ${booking.modeOfPayment}, " +
                                    "User ID: ${booking.userId}")
                        }
                    }
                } else {
                    Toast.makeText(context, "Booking not fetched!", Toast.LENGTH_SHORT).show()
                    Log.e("Booking not fetched", response.errorBody()?.string() ?: "Unknown error")
                }
            }

        override fun onFailure(call: Call<List<AvailableBooking>>, t: Throwable) {
            Toast.makeText(context, "Error Fetching Book!", Toast.LENGTH_SHORT).show()
            Log.e("Error Fetching Book", t.message.toString())
        }
    })

    }//END
}