package com.example.byahemoto.models

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val user: User
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String
)
