package com.example.byahemoto.models

//data class OrderResponse(
//    val success: Boolean,
//    val message: String,
//    val data: OrderData?
//)
//
//data class OrderData(
//    val orderId: String,
//    val approvalUrl: String
//)

data class OrderResponse(
    val success: Boolean,
    val message: String,
    val data: OrderData?
)

data class OrderData(
    val id: String,
    val status: String,
    val links: List<Links>
)

data class Links(
    val href: String,
    val rel: String,
    val method: String
)
