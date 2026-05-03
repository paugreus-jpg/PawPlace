// ui/navigation/NavGraph.kt
package com.example.dogmap.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.notifications.NotificationsScreen
import com.example.dogmap.ui.notifications.NotificationsSettingsScreen
import com.example.dogmap.ui.screens.*
import com.example.dogmap.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DogMapNav(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash.path
    ) {
        composable(Route.Splash.path) {
            val factory = LocalViewModelFactory.current
            val userVm: UserViewModel = viewModel(factory = factory)
            SplashScreen {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    userVm.checkProfileSetupAndNavigate(
                        uid = user.uid,
                        onSetupCompleted = {
                            navController.navigate(Route.Map.path) {
                                popUpTo(Route.Splash.path) { inclusive = true }
                            }
                        },
                        onSetupNeeded = {
                            // Perfil incompleto → Login primero, no ProfileSetup directamente
                            navController.navigate(Route.Login.path) {
                                popUpTo(Route.Splash.path) { inclusive = true }
                            }
                        }
                    )
                } else {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            }
        }

        composable(Route.Login.path) {
            val factory = LocalViewModelFactory.current
            val userVm: UserViewModel = viewModel(factory = factory)
            LoginScreen(
                onLoginSuccess = {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null) {
                        userVm.checkProfileSetupAndNavigate(
                            uid = user.uid,
                            onSetupCompleted = {
                                navController.navigate(Route.Map.path) {
                                    popUpTo(Route.Login.path) { inclusive = true }
                                }
                            },
                            onSetupNeeded = {
                                navController.navigate(Route.ProfileSetup.path) {
                                    popUpTo(Route.Login.path) { inclusive = true }
                                }
                            }
                        )
                    } else {
                        navController.navigate(Route.Map.path) {
                            popUpTo(Route.Login.path) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Route.Register.path)
                }
            )
        }

        composable(Route.Register.path) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Route.ProfileSetup.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Route.ProfileSetup.path) {
            ProfileSetupScreen(
                onSetupComplete = {
                    navController.navigate(Route.Map.path) {
                        popUpTo(Route.ProfileSetup.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Map.path) {
            MapScreen(
                onAddClick = { lat, lon ->
                    navController.navigate("${Route.Add.path}/$lat/$lon")
                },
                onDogDetailClick = { remoteId ->
                    navController.navigate("${Route.PlaceDetail.path}/$remoteId")
                },
                onAuthorClick = { uid ->
                    navController.navigate("${Route.PublicProfile.path}/$uid")
                }
            )
        }

        composable("${Route.MapFocus.path}/{lat}/{lon}") { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()

            MapScreen(
                focusLat = lat,
                focusLon = lon,
                onBackClick = { navController.popBackStack() },
                onAddClick = { addLat, addLon ->
                    navController.navigate("${Route.Add.path}/$addLat/$addLon")
                },
                onDogDetailClick = { remoteId ->
                    navController.navigate("${Route.PlaceDetail.path}/$remoteId")
                },
                onAuthorClick = { uid ->
                    navController.navigate("${Route.PublicProfile.path}/$uid")
                }
            )
        }

        composable(Route.List.path) {
            ListScreen(
                onDogClick = { dog ->
                    if (dog.remoteId.isNotBlank()) {
                        navController.navigate("${Route.PlaceDetail.path}/${dog.remoteId}")
                    }
                },
                onAuthorClick = { uid ->
                    navController.navigate("${Route.PublicProfile.path}/$uid")
                }
            )
        }

        composable(
            route = "${Route.PlaceDetail.path}/{remoteId}",
            arguments = listOf(navArgument("remoteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val remoteId = backStackEntry.arguments?.getString("remoteId") ?: ""
            PlaceDetailScreen(
                dogRemoteId = remoteId,
                onBackClick = { navController.popBackStack() },
                onAuthorClick = { uid ->
                    navController.navigate("${Route.PublicProfile.path}/$uid")
                },
                onMapClick = { lat, lon ->
                    navController.navigate("${Route.MapFocus.path}/$lat/$lon")
                }
            )
        }

        composable("${Route.Add.path}/{lat}/{lon}") { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 39.4699
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: -0.3763

            AddScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() },
                lat = lat,
                lon = lon
            )
        }

        composable(Route.Profile.path) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDogClick = { dog ->
                    navController.navigate(
                        "${Route.MapFocus.path}/${dog.latitude}/${dog.longitude}"
                    )
                },
                onPublicProfileClick = { uid ->
                    navController.navigate("${Route.PublicProfile.path}/$uid")
                },
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Route.Settings.path) }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = "${Route.PublicProfile.path}/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            PublicProfileScreen(
                uid = uid,
                onBackClick = { navController.popBackStack() },
                onDogClick = { dog ->
                    navController.navigate(
                        "${Route.MapFocus.path}/${dog.latitude}/${dog.longitude}"
                    )
                }
            )
        }

        composable(Route.Walks.path) {
            WalkListScreen(
                onBackClick = { navController.popBackStack() },
                onWalkClick = { remoteId ->
                    navController.navigate("${Route.WalkDetail.path}/$remoteId")
                },
                onRecordClick = {
                    navController.navigate(Route.WalkRecord.path)
                }
            )
        }

        composable(Route.WalkRecord.path) {
            WalkRecordScreen(
                onBackClick = { navController.popBackStack() },
                onWalkSaved = { remoteId ->
                    navController.popBackStack()
                    if (remoteId.isNotBlank()) {
                        navController.navigate("${Route.WalkDetail.path}/$remoteId")
                    }
                }
            )
        }

        composable(
            route = "${Route.WalkDetail.path}/{remoteId}",
            arguments = listOf(navArgument("remoteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val remoteId = backStackEntry.arguments?.getString("remoteId") ?: ""
            WalkDetailScreen(
                walkRemoteId = remoteId,
                onBackClick = { navController.popBackStack() },
                onFollowClick = { id ->
                    navController.navigate("${Route.WalkFollow.path}/$id")
                },
                onDeleted = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Route.WalkFollow.path}/{remoteId}",
            arguments = listOf(navArgument("remoteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val remoteId = backStackEntry.arguments?.getString("remoteId") ?: ""
            WalkFollowScreen(
                walkRemoteId = remoteId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Route.Notifications.path) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Route.NotificationsSettings.path) }
            )
        }

        composable(Route.NotificationsSettings.path) {
            NotificationsSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
