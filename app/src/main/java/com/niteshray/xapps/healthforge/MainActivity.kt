package com.niteshray.xapps.healthforge

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.niteshray.xapps.healthforge.core.permissions.PermissionManager
import com.niteshray.xapps.healthforge.feature.auth.presentation.compose.DoctorSetupScreen
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
    
    companion object {
        const val TAG = "MainActivity"
    }
    
    private lateinit var permissionManager: PermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity created")
        
        permissionManager = PermissionManager(this)
        
        enableEdgeToEdge()
        setContent {
            HealthForgeTheme {
                App(permissionManager = permissionManager)
            }
        }
    }
}

@Composable
fun App(permissionManager: PermissionManager){
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val authToken by authViewModel.authtoken.collectAsStateWithLifecycle()
    
    var permissionsGranted by remember { mutableStateOf(false) }
    
    // Request permissions when app starts
    LaunchedEffect(Unit) {
        Log.d("App", "Requesting permissions on app start")
        if (permissionManager.areAllPermissionsGranted()) {
            Log.d("App", "Permissions already granted")
            permissionsGranted = true
        } else {
            permissionManager.requestAllPermissions(
                onGranted = {
                    Log.d("App", "All permissions granted successfully")
                    permissionsGranted = true
                },
                onDenied = {
                    Log.w("App", "Permissions denied by user")
                    // Show a dialog or message to user about required permissions
                    permissionsGranted = false
                }
            )
        }
    }

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
                onPatientSignupSuccess = {
                    navController.navigate(Routes.UserSetup.route)
                },
                onDoctorNavigate = { name, email, password ->
                    // Navigate to DoctorSetup with user data
                    navController.navigate(Routes.DoctorSetup.createRoute(name, email, password))
                }
            )
        }

        composable(Routes.Home.route){
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
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

        composable(
            route = Routes.DoctorSetup.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType },
                navArgument("password") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            
            DoctorSetupScreen(
                userName = name,
                userEmail = email,
                userPassword = password,
                onSetupComplete = {
                    navController.navigate(Routes.DoctorDashboard.route) {
                        popUpTo(Routes.DoctorSetup.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.DoctorDashboard.route){
            DoctorDashboardScreen(
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.DoctorDashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Composable
fun DoctorDashboardScreen(onLogout: () -> Unit) {

}