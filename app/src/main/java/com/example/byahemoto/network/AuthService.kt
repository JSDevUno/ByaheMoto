package com.example.byahemoto.network

import com.example.byahemoto.models.LoginResponse
import com.example.byahemoto.models.RegisterRequest
import com.example.byahemoto.models.ResetPasswordRequest
import com.example.byahemoto.models.SignupRequest
import com.example.byahemoto.models.SignupResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthService {

    @POST("/auth/register")
    fun register(@Body request: RegisterRequest): Call<SignupResponse>

    @Multipart
    @POST("/auth/verificatio")
    fun sendVerificationRequest(
        @Part file: MultipartBody.Part,
        @Part("userId") userId: RequestBody,
        @Part("identityType") identityType: RequestBody
    ): Call<Void>

    @Multipart
    @POST("/auth/login")
    fun login(
        @Part("username") username: RequestBody,
        @Part("password") password: RequestBody
    ): Call<LoginResponse>

    @POST("/auth/forgot-password")
    fun sendResetLink(
        @Body email: Map<String, String>
    ): Call<Void>

    @POST("/auth/reset-password")
    fun resetPassword(
        @Body resetPasswordRequest: ResetPasswordRequest
    ): Call<Void>
}