package com.example.byahemoto.models

data class BookingRequest(
    val paymentMethod: String,
    val vehicleType: String,
    val locationFrom: LocData,
    val locationTo: LocData
)

data class LocData(
    val lat: Double,
    val lng: Double
)
