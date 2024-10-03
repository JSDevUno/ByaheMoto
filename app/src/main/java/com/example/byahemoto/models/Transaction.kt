package com.example.byahemoto.models


data class TransactionResponse(
    val success: Boolean,
    val message: String,
    val data: List<Transaction>
)

data class Transaction(
    val id: Int,
    val userId: Int,
    val amount: Double,
    val type: String,
    val createdAt: String,
    val updatedAt: String,
    val user: UserInfo?,
    val booking: BookingInfo?
)

data class UserInfo(
    val name: String?,
    val email: String?
)

data class BookingInfo(
    val bookingId: String?,
    val date: String?,
    val location: String?
)
