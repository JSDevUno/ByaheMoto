package com.example.byahemoto

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.byahemoto.models.AvailableBooking
import com.example.byahemoto.models.BookingDetails
import com.example.byahemoto.network.AuthService
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale


class OrderFragment : Fragment() {
    private val BASE_URL = "http://192.168.1.20:8000"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        listAvailableBookings()

        return inflater.inflate(
            R.layout.fragment_order, container, false
        )
    }

    private fun getTokenFromPreferences(): String {
        val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
        return "Bearer ${sharedPref?.getString("access_token", "") ?: ""}"
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

    private fun listAvailableBookings() {
        val token = getTokenFromPreferences()
        Log.i("Token", token)
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(AuthService::class.java)
        service.getAvailableBookings(token).enqueue(object : Callback<AvailableBooking> {

            @SuppressLint("CommitPrefEdits")
            override fun onResponse(
                call: Call<AvailableBooking>,
                response: Response<AvailableBooking>
            ) {
                if (response.isSuccessful) {
                    val availableBooking = response.body()
                    Log.i("Fetch Success", availableBooking.toString())
                    val container = activity?.findViewById<LinearLayout>(R.id.cardViewContainer)

                    availableBooking?.data?.forEach { booking ->
                        val bookingId = booking.id
                        // Inflate the layout and add multiple instances
                        val inflater = LayoutInflater.from(activity)

                        // Inflate the layout_panels.xml layout
                        val panelView = inflater.inflate(R.layout.layout_panels, container, false)
                        val fareText = panelView.findViewById<TextView>(R.id.fareText)
                        val myLocationText = panelView.findViewById<TextView>(R.id.myLocationText)
                        val destinationText = panelView.findViewById<TextView>(R.id.destinationText)
                        val statusText = panelView.findViewById<TextView>(R.id.statusText)
                        val timestampText = panelView.findViewById<TextView>(R.id.timestampText)

                        // Booking Actions
                        val acceptButton = panelView.findViewById<TextView>(R.id.acceptButton)
                        val rejectButton = panelView.findViewById<TextView>(R.id.rejectButton)
                        val mapButton = panelView.findViewById<TextView>(R.id.mapButton)
                        // Generate unique IDs for Actions
                        acceptButton.id = View.generateViewId()
                        rejectButton.id = View.generateViewId()
                        mapButton.id = View.generateViewId()
                        // Generate unique IDs for each view
                        fareText.id = View.generateViewId()
                        myLocationText.id = View.generateViewId()
                        destinationText.id = View.generateViewId()
                        statusText.id = View.generateViewId()
                        timestampText.id = View.generateViewId()

                        // View booking on map
                        mapButton.setOnClickListener {

                            // Save the booking id into user_prefs
                            val sharedPref = activity?.getSharedPreferences("user_prefs", MODE_PRIVATE)
                            val editor = sharedPref?.edit()
                            editor?.putInt("bookingId", bookingId)
                            editor?.apply()

                            val intent = Intent(context, DriverDashboard::class.java)
                            intent.putExtra("navigateTo", "nav_current")
                            startActivity(intent)
                        }

                        // Reject a booking
                        rejectButton.setOnClickListener {
                            service.rejectBooking(token, bookingId).enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Booking Rejected", Toast.LENGTH_SHORT).show()
                                        Log.i("Reject Success", "Booking Rejected")
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

                        // Accept a booking
                        acceptButton.setOnClickListener {
                            service.acceptBooking(token, bookingId).enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Booking Accepted", Toast.LENGTH_SHORT).show()
                                        Log.i("Accept Success", "Booking Accepted")

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
                        // It shows the details of the booking with the current ID
                        service.getDisplayBookingDetails(token,bookingId).enqueue(object : Callback<BookingDetails> {
                            @SuppressLint("SetTextI18n")
                            override fun onResponse(
                                call: Call<BookingDetails>,
                                response: Response<BookingDetails>
                            ) {
                                if (response.isSuccessful) {
                                    val bookingDetails = response.body()
                                    val locationFrom = LatLng(bookingDetails?.data?.locationFrom?.lat ?: 0.0, bookingDetails?.data?.locationFrom?.lng ?: 0.0)
                                    val locationTo = LatLng(bookingDetails?.data?.locationTo?.lat ?: 0.0, bookingDetails?.data?.locationTo?.lng ?: 0.0)
                                    if (bookingDetails != null) {
                                        Log.i("Details Success", bookingDetails.toString())
                                        fareText.text = bookingDetails.data.fare.toString()
                                        myLocationText.text = getPlaceName(locationFrom)
                                        destinationText.text = getPlaceName(locationTo)
                                        statusText.text = bookingDetails.data.status
                                        timestampText.text = bookingDetails.data.createdAt

                                        // Add the inflated view to the parent container
                                        container?.addView(panelView)
                                    } else {
                                        Log.i("Details Empty", "Empty response body")
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.i("Details Not Success", errorBody ?: "Unknown error")
                                }
                            }

                            override fun onFailure(call: Call<BookingDetails>, t: Throwable) {
                                Log.i("Details Failed", t.toString())
                            }
                        })

                    }
                } else {
                    Log.e("Fetch Error", response.errorBody()?.string() ?: "Unknown error")
                }
            }
            override fun onFailure(call: Call<AvailableBooking>, t: Throwable) {
                Toast.makeText(context, "Error Fetching Book!", Toast.LENGTH_SHORT).show()
                Log.e("Fetch Failed", t.message.toString())
            }
        })
    }
}

