package com.example.byahemoto.network

import com.example.byahemoto.models.AvailableBooking
import com.example.byahemoto.models.BookingDetails
import com.example.byahemoto.models.BookingRequest
import com.example.byahemoto.models.DriverLocationResponse
import com.example.byahemoto.models.GetProfileResponse
import com.example.byahemoto.models.LoginResponse
import com.example.byahemoto.models.OrderRequest
import com.example.byahemoto.models.OrderResponse
import com.example.byahemoto.models.ProfileUpdate
import com.example.byahemoto.models.ProfileUpdateResponse
import com.example.byahemoto.models.RefreshTokenRequest
import com.example.byahemoto.models.RefreshTokenResponse
import com.example.byahemoto.models.RegisterDriverRequest
import com.example.byahemoto.models.RegisterRequest
import com.example.byahemoto.models.ResetPasswordRequest
import com.example.byahemoto.models.RideHistoryResponse
import com.example.byahemoto.models.SignupDriverResponse
import com.example.byahemoto.models.SignupResponse
import com.example.byahemoto.models.TransactionResponse
import com.example.byahemoto.models.UpdateDriverLocation
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface AuthService {

    @POST("/auth/register")
    fun registerDriver(
        @Body request: RegisterDriverRequest
    ): Call<SignupDriverResponse>

    @POST("/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<SignupResponse>

    @Multipart
    @PUT("/profile/picture")
    fun updateProfilePicture(
        @Header("Authorization") token: String,
        @Part profilePicture: MultipartBody.Part // Use @Part directly
    ): Call<Void>

    @POST("/auth/refresh-token")
    fun refreshToken(
        @Body request: RefreshTokenRequest //@Body request: String?
    ): Call<RefreshTokenResponse>

    @GET("/profile/")
    fun getUserProfile(
        @Header("Authorization") token: String
    ): Call<GetProfileResponse>

    @PUT("/profile/")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body profileUpdate: ProfileUpdate
    ): Call<ProfileUpdateResponse>

    @Multipart
    @POST("/auth/verification")
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


    //Booking-related endpoints

    // Get available bookings
    @GET("/bookings/")
    fun getAvailableBookings(
        @Header("Authorization") token: String
    ): Call<AvailableBooking>

    // Create a new booking
    @POST("/bookings/")
    fun createBooking(@Body bookingRequest: BookingRequest): Call<BookingDetails>

    // Accept a booking
    @PUT("/bookings/{booking_id}/accept")
    fun acceptBooking(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Call<Void>

    // Reject a booking
    @PUT("/bookings/{booking_id}/reject")
    fun rejectBooking(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Call<Void>

    // Cancel a booking
    @PUT("/bookings/{booking_id}/cancel")
    fun cancelBooking(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Call<Void>

    // Show driver's current location
    @POST("/bookings/location")
    fun locateDriver(
        @Header("Authorization") token: String,
        @Body requestBody: UpdateDriverLocation
    ): Call<Void>

    // Get real-time driver location updates
    @GET("/bookings/{booking_id}/location")
    fun getDriverLocationUpdates(
        @Path("booking_id") bookingId: Int
    ): Call<DriverLocationResponse>

    //Booking Details Beta
    @GET("/bookings/{booking_id}")
    fun getDisplayBookingDetails(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Call<BookingDetails>

    //Booking Details
    @GET("/bookings/{booking_id}")
    fun getBookingDetails(
        @Path("booking_id") bookingId: Int
    ): Call<BookingDetails>

    //Ride History

    @GET("/profile/history/ride")
    fun getRideHistory(
        @Header("Authorization") token: String
    ): Call<RideHistoryResponse>
    
    @GET("/profile/history/transaction")
    fun getTransactionHistory(
        @Header("Authorization") token: String
    ): Call<TransactionResponse>


    //Top Up
    @POST("/profile/top-up")
    fun topUp(
        @Header("Authorization") token: String,
        @Body requestBody: OrderRequest // Map<String, Double>
    ): Call<OrderResponse>

    @POST("/profile/top-up/{order_id}/capture")
    fun captureTopUp(
        @Header("Authorization") token: String,
        @Path("order_id") orderId: String
    ): Call<Void>

}