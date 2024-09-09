package com.example.byahemoto.models

data class DriverLocationResponse(
    val driverId: Int,
    val lat: Double,
    val lng: Double,
    val timestamp: String
)
