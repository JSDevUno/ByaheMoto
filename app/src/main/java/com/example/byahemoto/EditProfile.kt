package com.example.byahemoto

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.byahemoto.models.ProfileUpdate
import com.example.byahemoto.models.ProfileUpdateResponse
import com.example.byahemoto.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class EditProfile : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var saveButton: Button
    private lateinit var phoneNumberEditText: EditText
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private var selectedImageFile: File? = null
    private val REQUEST_CODE_STORAGE_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        profileImageView = findViewById(R.id.profileImageView)
        saveButton = findViewById(R.id.saveButton)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)

        profileImageView.setOnClickListener {
            if (checkStoragePermission()) {
                openFileManager()
            } else {
                requestStoragePermission()
            }
        }

        saveButton.setOnClickListener {
            saveProfileChanges()
        }

        loadCurrentProfileData()
    }

    private fun checkStoragePermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_CODE_STORAGE_PERMISSION
        )
    }

    private fun openFileManager() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun saveProfileChanges() {
        val token = getTokenFromPreferences()
        Log.d("AuthToken", "Token: $token") // Log the token to ensure it's correctly retrieved

        val phoneNumber = phoneNumberEditText.text.toString().trim()

        // Save phone number locally
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("phone_number", phoneNumber)
            apply()
        }

        val fullName = sharedPref.getString("full_name", null)
        val username = sharedPref.getString("username", null)
        val email = sharedPref.getString("email", null)

        val profileUpdate = ProfileUpdate(
            fullName = fullName,
            username = username,
            email = email,
            phoneNumber = phoneNumber
        )
        Log.d(TAG, "Updating profile with: $profileUpdate")

        if (selectedImageFile != null) {
            uploadProfilePicture(token, selectedImageFile!!)
        } else {
            RetrofitInstance.authService.updateProfile(token, profileUpdate).enqueue(object : Callback<ProfileUpdateResponse> {
                override fun onResponse(call: Call<ProfileUpdateResponse>, response: Response<ProfileUpdateResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditProfile, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Profile update response: ${response.body()}")
                        finish() // Go back to profile page after saving changes
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(this@EditProfile, "Failed to update profile: $errorBody", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Profile update failed: $errorBody")
                    }
                }

                override fun onFailure(call: Call<ProfileUpdateResponse>, t: Throwable) {
                    Toast.makeText(this@EditProfile, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Profile update error: ${t.message}", t)
                }
            })
        }
    }

    private fun loadCurrentProfileData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val phoneNumber = sharedPref.getString("phone_number", "")
        phoneNumberEditText.setText(phoneNumber)

        val profilePicUrl = sharedPref.getString("profile_pic_url", null)
        if (profilePicUrl != null) {
            Glide.with(this).load(profilePicUrl).into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.avatar)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            selectedImageUri?.let { uri ->
                val filePath = getRealPathFromURI(uri)
                filePath?.let { path ->
                    selectedImageFile = File(path)
                    profileImageView.setImageURI(uri)
                }
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String? {
        var result: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }

    private fun uploadProfilePicture(token: String, file: File) {
        val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("profile_picture", file.name, requestFile)

        RetrofitInstance.authService.updateProfilePicture("Bearer $token", body).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    val newProfilePicUrl = file.absolutePath // Replace with actual URL if provided
                    val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("profile_pic_url", newProfilePicUrl)
                        apply()
                    }
                    saveProfileChanges()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Toast.makeText(this@EditProfile, "Failed to update profile picture: $errorBody", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@EditProfile, "An error occurred: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getTokenFromPreferences(): String {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val token = sharedPref.getString("auth_token", "") ?: ""
        Log.d("AuthToken", "Retrieved token: $token")
        return "Bearer $token" // If your API requires "Bearer " prefix
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileManager()
            } else {
                Toast.makeText(this, "Storage permission is required to select a profile picture", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
