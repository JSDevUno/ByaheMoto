package com.example.byahemoto.models

data class ErrorResponse(
    val success: Boolean,
    val message: String
)

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
    val full_name: String,
    val phone_number: String?,
    val registration_type: String,
    val role: Role
)

enum class Role(val value: String) {
    DRIVER("DRIVER"),
    USER("USER"),
    ADMIN("ADMIN")
}
