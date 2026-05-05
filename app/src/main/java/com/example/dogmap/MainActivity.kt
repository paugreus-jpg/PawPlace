package com.example.dogmap

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dogmap.R
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.UserAvatar
import com.example.dogmap.ui.navigation.DogMapNav
import com.example.dogmap.ui.navigation.Route
import com.example.dogmap.ui.theme.BrandPrimary
import com.example.dogmap.ui.theme.DogMapTheme
import com.example.dogmap.viewmodel.DogViewModel
import com.example.dogmap.viewmodel.NotificationsViewModel
import com.example.dogmap.viewmodel.SettingsViewModel

class MainActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var contentReady = false
        splashScreen.setKeepOnScreenCondition { !contentReady }

        val app = application as DogMapApplication

        setContent {
            LaunchedEffect(Unit) { contentReady = true }
            CompositionLocalProvider(LocalViewModelFactory provides app.viewModelFactory) {
                val settingsVm: SettingsViewModel = viewModel(factory = app.viewModelFactory)
                val isDarkTheme by settingsVm.isDarkTheme.collectAsState()

                DogMapTheme(darkTheme = isDarkTheme) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val notifPermState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                        LaunchedEffect(Unit) {
                            if (!notifPermState.status.isGranted) notifPermState.launchPermissionRequest()
                        }
                    }

                    val navController = rememberNavController()
                    val currentRoute by navController.currentBackStackEntryAsState()

                    val route = currentRoute?.destination?.route
                    val showBars = route in listOf(Route.Map.path, Route.List.path, Route.Walks.path)

                    val dogVm: DogViewModel = viewModel(factory = app.viewModelFactory)
                    val currentUser by dogVm.currentUser.collectAsState()

                    val notifVm: NotificationsViewModel = viewModel(factory = app.viewModelFactory)
                    val unreadCount by notifVm.unreadCount.collectAsState()

                    Scaffold(
                        topBar = {
                            if (showBars) {
                                TopAppBar(
                                    title = {
                                        Text(stringResource(R.string.app_title))
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    actions = {
                                        IconButton(onClick = {
                                            navController.navigate(Route.Notifications.path)
                                        }) {
                                            BadgedBox(
                                                badge = {
                                                    if (unreadCount > 0) {
                                                        Badge { Text(unreadCount.toString()) }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Notifications,
                                                    contentDescription = stringResource(R.string.notifications_screen)
                                                )
                                            }
                                        }
                                        IconButton(onClick = {
                                            navController.navigate(Route.Profile.path)
                                        }) {
                                            UserAvatar(
                                                displayName = currentUser?.displayName
                                                    ?: currentUser?.email ?: "?",
                                                photoUrl = currentUser?.photoUrl ?: "",
                                                size = 32.dp
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        bottomBar = {
                            if (showBars) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ) {
                                    val itemColors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BrandPrimary,
                                        selectedTextColor = BrandPrimary,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Map, stringResource(R.string.map_screen)) },
                                        label = { Text(stringResource(R.string.map_screen)) },
                                        selected = route == Route.Map.path,
                                        colors = itemColors,
                                        onClick = {
                                            navController.navigate(Route.Map.path) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.list_screen)) },
                                        label = { Text(stringResource(R.string.list_screen)) },
                                        selected = route == Route.List.path,
                                        colors = itemColors,
                                        onClick = {
                                            navController.navigate(Route.List.path) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, stringResource(R.string.walks_screen)) },
                                        label = { Text(stringResource(R.string.walks_screen)) },
                                        selected = route == Route.Walks.path,
                                        colors = itemColors,
                                        onClick = {
                                            navController.navigate(Route.Walks.path) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            DogMapNav(navController)
                        }
                    }
                }
            }
        }
    }
}
