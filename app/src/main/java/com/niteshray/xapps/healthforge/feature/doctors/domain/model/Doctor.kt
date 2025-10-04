package com.niteshray.xapps.healthforge.feature.doctors.domain.model

data class Address(
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String,
    val zip: String
)

data class Doctor(
    val name: String,
    val email: String,
    val password: String,
    val speciality: String,
    val degree: String,
    val experience: Int,
    val about: String,
    val fees: Int,
    val address: Address,
    val image: String = "https://www.shutterstock.com/image-vector/male-doctor-smiling-selfconfidence-flat-600nw-2281709217.jpg"
)
