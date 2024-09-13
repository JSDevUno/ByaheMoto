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

    private fun listAvailableBookings(){
    // Add new booking to the list
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
                        Log.d("AvailableBooking", booking.toString())
                        response.body()?.forEach {
                            Log.d("AvailableBooking", it.toString())
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Booking not fetched!", Toast.LENGTH_SHORT).show()
                Log.e("AvailableBooking", response.errorBody().toString())
            }
        }

        override fun onFailure(call: Call<List<AvailableBooking>>, t: Throwable) {
            Toast.makeText(context, "Error Fetching Book!", Toast.LENGTH_SHORT).show()
            Log.e("AvailableBooking", t.message.toString())
        }
    })

    }//END
}