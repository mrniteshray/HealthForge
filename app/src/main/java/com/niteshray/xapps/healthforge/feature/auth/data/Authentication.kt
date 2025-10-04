package com.niteshray.xapps.healthforge.feature.auth.data

import com.niteshray.xapps.healthforge.feature.auth.domain.model.AuthResponse
import com.niteshray.xapps.healthforge.feature.auth.domain.model.RegisterUser
import com.niteshray.xapps.healthforge.feature.auth.domain.model.loginUser
import com.niteshray.xapps.healthforge.feature.doctors.domain.model.Doctor
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface Authentication {

    @POST("user/register")
    suspend fun RegisterUser(@Body resgiterUser : RegisterUser) : Response<AuthResponse>

    @POST("user/login")
    suspend fun loginUser(@Body loginUser: loginUser) : Response<AuthResponse>

    @POST("doctor/register")
    suspend fun RegisterDoctor(@Body doctorData : Doctor) : Response<AuthResponse>

    @POST("user/update-health-info")
    suspend fun updateHealthInfo(
        @Body body: Map<String, String>
    ): Response<ApiResponse>

    data class ApiResponse(
        val success: Boolean,
        val message: String
    )
}