package com.example.byahemoto.models

import com.google.gson.annotations.SerializedName

data class AvailableBooking(
    val success: Boolean,
    val message: String,
    val data: List<Booking>
)
data class Booking(
    @SerializedName("id") val id: Int,
    @SerializedName("userId") val userId: Int,
    @SerializedName("modeOfPayment") val modeOfPayment: String,
    @SerializedName("vehicleType") val vehicleType: String,
    @SerializedName("fare") val fare: Double,
    @SerializedName("locationFrom") val locationFrom: LocationDataDriver,
    @SerializedName("locationTo") val locationTo: LocationDataDriver,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class LocationDataDriver(
    val lat: Double,
    val lng: Double
)
