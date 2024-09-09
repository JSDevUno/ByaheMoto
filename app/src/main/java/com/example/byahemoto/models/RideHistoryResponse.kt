package com.example.byahemoto.models

data class RideHistoryResponse(
    val success: Boolean,
    val message: String,
    val data: List<RideDetails>
)

data class RideDetails(
    val locationFrom: RideLocationData,
    val locationTo: RideLocationData,
    val timestamp: String
)

data class RideLocationData(
    val lat: Double,
    val lng: Double,
    val address: String?
)
