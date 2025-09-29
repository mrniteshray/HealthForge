package com.niteshray.xapps.healthforge.feature.auth.domain.model

data class RegisterUser(
    val name : String,
    val email : String,
    val password : String
)

data class loginUser(
    val email: String,
    val password: String
)

data class AuthResponse(
    val success : Boolean,
    val token : String
)