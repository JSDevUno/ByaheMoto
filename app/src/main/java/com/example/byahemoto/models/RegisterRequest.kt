package com.example.byahemoto.models

data class RegisterRequest(
    val fullname: String, // Change `fullName` to `fullname`
    val username: String,
    val email: String,
    val password: String,
    val confirm_password: String,  // This seems correct
    val registration_type: String = "regular"
)



