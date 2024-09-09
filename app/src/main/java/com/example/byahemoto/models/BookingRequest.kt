package com.example.byahemoto.models

data class BookingRequest(
    val paymentMethod: String,
    val vehicleType: String,
    val locationFrom: Map<String, Double>,
    val locationTo: Map<String, Double>
)
