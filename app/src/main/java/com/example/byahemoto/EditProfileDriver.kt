package com.example.byahemoto

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.example.byahemoto.models.ProfileUpdate
import com.example.byahemoto.models.ProfileUpdateResponse
import com.example.byahemoto.network.RetrofitInstance
import com.example.byahemoto.utils.Constants
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class EditProfileDriver : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var saveButton: Button
    private lateinit var phoneNumberEditText: EditText
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private var selectedImageFile: File? = null
    private val REQUEST_CODE_STORAGE_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile_driver)

        profileImageView = findViewById(R.id.profileImageViewDriver)
        saveButton = findViewById(R.id.saveButtonDriver)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditTextDriver)

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
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
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
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val profileUpdate = ProfileUpdate(phoneNumber = phoneNumber)

        // Update phone number
        RetrofitInstance.getAuthService(this).updateProfile(token, profileUpdate)
            .enqueue(object : Callback<ProfileUpdateResponse> {
                override fun onResponse(
                    call: Call<ProfileUpdateResponse>,
                    response: Response<ProfileUpdateResponse>
                ) {
                    if (response.isSuccessful) {
                        // Save updated phone number in SharedPreferences
                        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("phone_number", phoneNumber)
                            apply()
                        }

                        Toast.makeText(
                            this@EditProfileDriver,
                            "Profile updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Return success result
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditProfileDriver,
                            "Failed to update profile: ${response.errorBody()?.string()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ProfileUpdateResponse>, t: Throwable) {
                    Toast.makeText(this@EditProfileDriver, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

        // Upload profile picture if a new image was selected
        selectedImageFile?.let { uploadProfilePicture(token, it) }
    }

    private fun loadCurrentProfileData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val phoneNumber = sharedPref.getString("phone_number", "")
        phoneNumberEditText.setText(phoneNumber)

        // Load profile picture with the token
        val profilePicUrl = GlideUrl("${Constants.BASE_URL}/profile/picture") {
            mapOf(
                Pair("Authorization", "Bearer ${sharedPref.getString("access_token", "")}")
            )
        }

        Glide.with(this)
            .load(profilePicUrl)
            .placeholder(R.drawable.avatar)
            .error(R.drawable.avatar)
            .circleCrop()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(profileImageView)
    }

    @Deprecated("Deprecated in Java")
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
        cursor?.let {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                result = it.getString(idx)
            }
            it.close()
        }
        return result
    }

    private fun uploadProfilePicture(token: String, file: File) {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("profilePicture", file.name, requestFile)

        // Upload the profile picture using the token
        RetrofitInstance.getAuthService(this).updateProfilePicture(token, body)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        selectedImageFile = null
                        Toast.makeText(
                            this@EditProfileDriver,
                            "Profile picture updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        saveProfileChanges()
                        loadCurrentProfileData()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(
                            this@EditProfileDriver,
                            "Failed to update profile picture: $errorBody",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(
                        this@EditProfileDriver,
                        "An error occurred: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun getTokenFromPreferences(): String {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return "Bearer ${sharedPref.getString("access_token", "") ?: ""}"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openFileManager()
        } else {
            Toast.makeText(
                this,
                "Storage permission is required to select a profile picture",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
