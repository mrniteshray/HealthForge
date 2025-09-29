package com.niteshray.xapps.healthforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.LoginScreen
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.SignupScreen
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.UserSetupScreen
import com.niteshray.xapps.healthforge.feature.auth.presentation.viewmodel.AuthViewModel
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.HealthcareDashboard
import com.niteshray.xapps.healthforge.feature.home.presentation.compose.HomeScreen
import com.niteshray.xapps.healthforge.ui.theme.HealthForgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthForgeTheme {
                App()
            }
        }
    }
}

@Composable
fun App(){
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val authToken by authViewModel.authtoken.collectAsStateWithLifecycle()

    val startDestination = if (authToken.isNullOrBlank()) Routes.Login.route else Routes.Home.route
    NavHost(navController = navController , startDestination = startDestination){
        composable(Routes.Login.route){
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route)
                },
                onSignUpClick = {
                    navController.navigate(Routes.SignUp.route)
                }
            )
        }

        composable(Routes.SignUp.route){
            SignupScreen(
                onLoginClick = {
                    navController.navigate(Routes.Login.route)
                },
                onSignupSuccess = {
                    navController.navigate(Routes.UserSetup.route)
                }
            )
        }

        composable(Routes.Home.route){
            HomeScreen()
        }

        composable(Routes.UserSetup.route){
            UserSetupScreen(
                onProfileComplete = { userHealthInfo ->
                    authViewModel.saveHealthInfo(userHealthInfo)
                },
                authViewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.UserSetup.route) { inclusive = true }
                    }
                }
            )
        }
    }
}