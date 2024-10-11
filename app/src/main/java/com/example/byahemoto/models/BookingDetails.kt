package com.example.byahemoto.models

data class BookingDetails(
    val success: Boolean,
    val message: String,
    val data: BookingData
)

data class BookingData(
    val id: Int,
    val userId: Int,
    val driverId: Int,
    val modeOfPayment: String,
    val vehicleType: String,
    val fare: Double,
    val locationFrom: Coordinates,
    val locationTo: Coordinates,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val user: Info
)

data class Info(
    val id: Int,
    val fullName: String,
    val username: String,
    val email: String,
    val role: String,
    val password: String,
    val isVerified: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class Coordinates(
    val lat: Double,
    val lng: Double
)
