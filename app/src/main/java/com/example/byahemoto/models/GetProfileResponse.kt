package com.example.byahemoto.models

data class GetProfileResponse(
    val success: Boolean,
    val message: String,
    val data: UserProfile
)

data class UserProfile(
    val id: Int,
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val role: String,
    val isVerified: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val lastKnownLocation: LocCoordinates,
    val vehicleType: String,
    val wallet: Wallet
)

data class LocCoordinates(
    val lat: Double,
    val lng: Double
)

data class Wallet(
    val id: Int,
    val userId: Int,
    val balance: Double,
    val createdAt: String,
    val updatedAt: String
)
