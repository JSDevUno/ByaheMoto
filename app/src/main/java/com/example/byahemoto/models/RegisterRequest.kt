package com.example.byahemoto.models

data class RegisterRequest(
    val full_name: String,
    val username: String,
    val email: String,
    val password: String,
    val confirm_password: String,
    val registration_type: String = "regular"
)



