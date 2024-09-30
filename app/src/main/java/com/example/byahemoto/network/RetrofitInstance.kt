package com.example.byahemoto.network

import android.content.Context
import android.util.Log
import com.example.byahemoto.models.RefreshTokenRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private fun getToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("access_token", null)
    }

    private fun getClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()


                getToken(context)?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                var response = chain.proceed(requestBuilder.build())


                if (response.code == 401) {

                    val newToken = refreshToken(context)
                    if (newToken != null) {

                        requestBuilder.removeHeader("Authorization")
                        requestBuilder.addHeader("Authorization", "Bearer $newToken")


                        with(context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()) {
                            putString("access_token", newToken)
                            apply()
                        }


                        response.close()
                        response = chain.proceed(requestBuilder.build())
                    } else {
                        Log.e("RetrofitInstance", "Token refresh failed, cannot retry request")
                    }
                }
                response
            }

            .build()
    }

    private fun refreshToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val refreshToken = sharedPref.getString("refresh_token", null)

        if (refreshToken != null) {
            val refreshTokenRequest = RefreshTokenRequest(refreshToken)
            val call = getAuthService(context).refreshToken(refreshTokenRequest)

            try {
                val response = call.execute()  // Synchronous call to refresh token
                Log.d("RetrofitInstance", "Refresh token response: ${response.raw()}")  // Log raw response for debugging

                if (response.isSuccessful) {
                    // Log the entire response body to check the structure
                    Log.d("RetrofitInstance", "Response Body: ${response.body().toString()}")

                    val refreshTokenResponse = response.body()
                    refreshTokenResponse?.data?.let { tokenData ->

                        // Save the new access token
                        with(sharedPref.edit()) {
                            putString("access_token", tokenData.access_token)
                            apply()
                        }

                        Log.d("RetrofitInstance", "Token refreshed successfully: ${tokenData.access_token}")
                        return tokenData.access_token
                    } ?: run {
                        Log.e("RetrofitInstance", "Response body is missing 'data' or 'access_token'")
                    }
                } else {
                    Log.e("RetrofitInstance", "Failed to refresh token. Response code: ${response.code()}")
                    Log.e("RetrofitInstance", "Response error body: ${response.errorBody()?.string()}")  // Log the error body for debugging
                }
            } catch (e: Exception) {
                Log.e("RetrofitInstance", "Exception while refreshing token", e)
            }
        } else {
            Log.e("RetrofitInstance", "No valid refresh token found in SharedPreferences")
        }
        return null
    }


    private fun getRetrofit(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://192.168.1.20:8000")
            .client(getClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getAuthService(context: Context): AuthService {
        return getRetrofit(context).create(AuthService::class.java)
    }
}