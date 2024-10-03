package com.example.byahemoto

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.example.byahemoto.models.AvailableBooking
import com.example.byahemoto.models.BookingDetails
import com.example.byahemoto.models.RefreshTokenRequest
import com.example.byahemoto.models.RefreshTokenResponse
import com.example.byahemoto.network.AuthService
import com.example.byahemoto.network.RetrofitInstance
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

    @SuppressLint("ShowToast")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if(isAdded) {
            listAvailableBookings()
            return inflater.inflate(
                R.layout.fragment_order, container, false
            )
        }
        return Toast.makeText(context, "Error Fetching Book!", Toast.LENGTH_SHORT).view
    }

    private fun getTokenFromPreferences(): String {
        val activity = activity ?: return ""
        val sharedPref = activity.getSharedPreferences("user_prefs", MODE_PRIVATE)
        return "Bearer ${sharedPref?.getString("access_token", "") ?: ""}"
    }

    private fun getPlaceName(latLng: LatLng): String {
        if (isAdded) {
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
        return "Unknown Location"
    }

    private fun listAvailableBookings() {
        if (!isAdded) return
        val activity = activity ?: return
        val context = context ?: return
        val token = getTokenFromPreferences()
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
                    val container = activity.findViewById<LinearLayout>(R.id.cardViewContainer)
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
                        val mapButton = panelView.findViewById<TextView>(R.id.mapButton)
                        // Generate unique IDs for Actions
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
                            val sharedPref = activity.getSharedPreferences("user_prefs", MODE_PRIVATE)
                            val editor = sharedPref?.edit()
                            editor?.putInt("bookingId", bookingId)
                            editor?.apply()
                            val intent = Intent(context, DriverDashboard::class.java)
                            intent.putExtra("navigateTo", "nav_current")
                            startActivity(intent)
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
                    if (response.code() == 401) {
                        Toast.makeText(context, "Please wait...", Toast.LENGTH_LONG).show()
                        refreshTheToken()
                    } else {
                        Toast.makeText(context, "Error Fetching Book!", Toast.LENGTH_SHORT).show()
                        Log.e("Fetch Error", response.errorBody()?.string() ?: "Unknown error")
                    }
                }
            }
            override fun onFailure(call: Call<AvailableBooking>, t: Throwable) {
                Toast.makeText(context, "Error Fetching Book!", Toast.LENGTH_SHORT).show()
                Log.e("Fetch Failed", t.message.toString())
            }
        })
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

