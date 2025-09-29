package com.niteshray.xapps.healthforge

sealed class Routes(val route : String) {
    object Login : Routes("Login")
    object SignUp : Routes("Signup")

    object Home : Routes("Home")

    object UserSetup : Routes("UserSetup")
//    object Details : Routes("details/{itemId}") {          // route template
//        fun createRoute(itemId: Int) = "details/$itemId"   // helper to navigate
//    }
}