package com.example.byahemoto.models

data class RefreshTokenResponse(
    val success: Boolean,
    val message: String,
    val data: TokenData? = null
)

data class TokenData(
    val access_token: String,
    val token_type: String
)
