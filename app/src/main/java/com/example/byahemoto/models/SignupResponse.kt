package com.example.byahemoto.models

data class SignupResponse(
    val success: Boolean,
    val message: String,
    val data: User
)