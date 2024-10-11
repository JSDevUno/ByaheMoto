package com.example.byahemoto.models

data class RegisterDriverRequest(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val registrationType: String = "driver",
    val vehicleType: String
)