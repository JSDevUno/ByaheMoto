    package com.example.byahemoto.models

data class BookingResponse(
    val bookingId: Int,
    val status: String,
    val fare: Double,
    val vehicleType: String,
    val locationFrom: LocationData,
    val locationTo: LocationData
)

data class LocationData(
    val lat: Double,
    val lng: Double
)
