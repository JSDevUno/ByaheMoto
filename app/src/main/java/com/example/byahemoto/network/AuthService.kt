package com.example.byahemoto.network

import com.example.byahemoto.models.LoginResponse
import com.example.byahemoto.models.RegisterRequest
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthService {
    @POST("/auth/register")
    fun register(@Body request: RegisterRequest): Call<Void>

    @Multipart
    @POST("/auth/login")
    fun login(
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody
    ): Call<LoginResponse>
}