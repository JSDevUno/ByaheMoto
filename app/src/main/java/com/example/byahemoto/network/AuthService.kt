package com.example.byahemoto.network

import com.example.byahemoto.models.LoginRequest
import com.example.byahemoto.models.LoginResponse
import com.example.byahemoto.models.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("/auth/register")
    fun register(@Body request: RegisterRequest): Call<Void>

    @POST("/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}
