package com.example.byahemoto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.example.byahemoto.utils.Constants
import com.google.android.material.bottomnavigation.BottomNavigationView

class Profile : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var logoutLayout: LinearLayout
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var regionTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var phoneNumberTextView: TextView
    private lateinit var locationManager: LocationManager

    private val activityMap = mapOf(
        R.id.nav_home to UserDashboard::class.java,
        R.id.nav_history to History::class.java,
        R.id.nav_wallet to Wallet::class.java,
        R.id.nav_profile to Profile::class.java
    )

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val geocoder = Geocoder(this@Profile)
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val region = addresses?.get(0)?.adminArea ?: "Unknown Region"
            regionTextView.text = "Region: $region"
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        profileImageView = findViewById(R.id.imageView2)
        phoneNumberTextView = findViewById(R.id.textView10)
        nameTextView = findViewById(R.id.textView4)
        emailTextView = findViewById(R.id.textView7)
        regionTextView = findViewById(R.id.textView11)
        logoutLayout = findViewById(R.id.linearlayout)

        val editProfileButton = findViewById<ImageView>(R.id.imageView3)
        val backBtn = findViewById<ImageView>(R.id.backBtn)


        profileImageView.setImageResource(R.drawable.avatar)

        bottomNavigationView = findViewById(R.id.BottomNavigation)
        bottomNavigationView.selectedItemId = R.id.nav_profile

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val activityClass = activityMap[menuItem.itemId]
            if (activityClass != null) {
                val intent = Intent(this, activityClass)
                startActivity(intent)
                true
            } else {
                false
            }
        }


        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfile::class.java)
            startActivity(intent)
        }


        logoutLayout.setOnClickListener {
            logout()
        }


        loadUserData()


        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0f, locationListener
            )
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
        }

        // Back button
        backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "Unknown Name")
        val email = sharedPref.getString("email", "Unknown Email")
        val phoneNumber = sharedPref.getString("phone_number", "Unknown Phone Number")


        // Load profile image
        val profilePicUrl = GlideUrl("${Constants.BASE_URL}/profile/picture") {
            mapOf(
                Pair("Authorization", "Bearer ${sharedPref.getString("access_token", "")}")
            )
        }

        Glide.with(this).load(profilePicUrl).placeholder(R.drawable.avatar).into(profileImageView)

        phoneNumberTextView.text = phoneNumber
        nameTextView.text = username
        emailTextView.text = email
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0, 0f, locationListener
                )
            }
        } else {
            Toast.makeText(
                this, "Location permissions are required to fetch the region.", Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()

        loadUserData()
    }
}