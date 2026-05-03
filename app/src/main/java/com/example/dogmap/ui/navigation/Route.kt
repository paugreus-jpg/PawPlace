package com.example.dogmap.ui.navigation

sealed class Route(val path: String) {
    object Splash : Route("splash")
    object Login : Route("login")
    object Register : Route("register")
    object Map : Route("map")
    object List : Route("list")
    object Add : Route("add")
    object MapFocus : Route("map_focus")
    object Profile : Route("profile")
    object PublicProfile : Route("publicProfile")
    object PlaceDetail : Route("placeDetail")
    object Walks : Route("walks")
    object WalkRecord : Route("walks/record")
    object WalkDetail : Route("walks/detail")
    object WalkFollow : Route("walks/follow")
    object Notifications : Route("notifications")
    object NotificationsSettings : Route("notifications/settings")
    object ProfileSetup : Route("profileSetup")
    object Settings : Route("settings")

    companion object {
        fun all() = listOf(Splash, Map, List, Add, Walks)
    }
}
