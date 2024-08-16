package com.example.byahemoto.models

data class RegisterRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val password: String,
    val confirmPassword: String
)
