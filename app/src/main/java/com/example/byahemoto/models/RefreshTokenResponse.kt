package com.example.byahemoto.models

import com.google.gson.annotations.SerializedName

data class RefreshTokenResponse(
    val success: Boolean,
    val message: String,
    val data: TokenData?
)

data class TokenData(
    @SerializedName("accessToken") val access_token: String,
    @SerializedName("tokenType") val token_type: String
)

