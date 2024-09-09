package com.example.byahemoto.models


data class TransactionResponse(
    val status: String,
    val transactions: List<Transaction>
)

data class Transaction(
    val id: Int,
    val type: String,
    val amount: Double,
    val description: String
)
