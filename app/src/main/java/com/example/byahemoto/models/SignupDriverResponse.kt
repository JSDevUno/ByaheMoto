package com.example.byahemoto.models

data class SignupDriverResponse(
    val success: Boolean,
    val message: String,
    val data: UserData
)

data class UserData(
    val id: Int,
    val fullName: String,
    val username: String,
    val email: String,
    val role: String,
    val isVerified: Boolean,
    val vehicleType: String
)