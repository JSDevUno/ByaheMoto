package com.example.byahemoto.models

data class OrderResponse(
    val success: Boolean,
    val message: String,
    val data: OrderData?
)

data class OrderData(
    val orderId: String,
    val approvalUrl: String
)
