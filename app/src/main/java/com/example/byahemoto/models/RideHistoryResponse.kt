package com.example.byahemoto.models

data class RideHistoryResponse(
    val success: Boolean,
    val message: String,
    val data: List<RideData>
)

data class RideData(
    val id: Int,
    val userId: Int,
    val modeOfPayment: String,
    val vehicleType: String,
    val fare: Double,
    val locationFrom: Location,
    val locationTo: Location,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

data class Location(
    val lat: Double,
    val lng: Double
)
