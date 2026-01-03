package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            MyApplicationTheme {

                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    auth.addAuthStateListener { firebaseAuth ->
                        if (firebaseAuth.currentUser == null) {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

//                val startScreen = if (isLoggedIn) "flightSearch" else "splash"
                val startScreen = "splash"


                NavHost(
                    navController = navController,
                    startDestination = startScreen
                ) {

                    composable("splash") {
                        LaunchedEffect(Unit) {
                            delay(2500)
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                        SplashScreen(onTimeout = {})
                    }

                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("flightSearch") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            }
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("flightSearch") {
                        FlightSearchScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
