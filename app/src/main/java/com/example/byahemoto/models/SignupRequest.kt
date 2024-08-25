package com.example.byahemoto.models

data class SignupRequest(
    val full_name: String,
    val username: String,
    val password: String,
    val id_verification_path: String,
    val registration_type: String = "regular"
)

//tentative
