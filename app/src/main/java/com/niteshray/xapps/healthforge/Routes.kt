package com.niteshray.xapps.healthforge

sealed class Routes(val route : String) {
    object Login : Routes("Login")
    object SignUp : Routes("Signup")

    object Home : Routes("Home")

    object UserSetup : Routes("UserSetup")
    object DoctorSetup : Routes("DoctorSetup/{name}/{email}/{password}") {
        fun createRoute(name: String, email: String, password: String) = "DoctorSetup/$name/$email/$password"
    }
    object DoctorDashboard : Routes("DoctorDashboard")
//    object Details : Routes("details/{itemId}") {          // route template
//        fun createRoute(itemId: Int) = "details/$itemId"   // helper to navigate
//    }
}