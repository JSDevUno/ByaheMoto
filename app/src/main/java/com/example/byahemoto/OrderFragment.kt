package com.example.byahemoto

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.byahemoto.models.AvailableBooking
import com.example.byahemoto.models.BookingResponse
import com.example.byahemoto.network.AuthService
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
        listAvailableBookings()
        return inflater.inflate(R.layout.fragment_order, container, false)
    }
    private fun listAvailableBookings(){
    // Add new booking to the list
    val api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AuthService::class.java)
    val sampleToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkcmF5YmFoQGdtYWlsLmNvbSIsImp0aSI6MTUsImZ1bGxfbmFtZSI6ImRyYXliYWgiLCJ1c2VybmFtZSI6ImRyYXliYWgiLCJyb2xlIjoiRFJJVkVSIiwiaXNfdmVyaWZpZWQiOmZhbHNlLCJleHAiOjE3MjYwNTM2ODZ9.2KilgKprhRBQ6k8X3IeMEmvYCJLTEv9OiAfGuhZjFsY"

        api.getAvailableBookings(sampleToken).enqueue(object : Callback<List<AvailableBooking>> {
        override fun onResponse(
            call: Call<List<AvailableBooking>>,
            response: Response<List<AvailableBooking>>
        ) {
            if (response.isSuccessful) {
                response.body()?.let {
                    for (booking in it) {
                        Log.d("AvailableBookingSuccess", booking.toString())
                    }
                }
            } else {
                Log.d("AvailableBookingError", response.errorBody().toString())
            }
        }

        override fun onFailure(call: Call<List<AvailableBooking>>, t: Throwable) {
            Log.d("AvailableBookingFailure", t.message.toString())
        }
    })

    }//END
}