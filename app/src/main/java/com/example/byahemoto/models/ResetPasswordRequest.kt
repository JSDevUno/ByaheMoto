package com.example.byahemoto.models

data class ResetPasswordRequest(
    val token: String,
    val password: String,
    val confirmPassword: String
)