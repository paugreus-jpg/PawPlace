package com.example.dogmap.ui.navigation

import org.junit.Assert.*
import org.junit.Test

class RouteTest {

    @Test fun `Splash has correct path`() = assertEquals("splash", Route.Splash.path)
    @Test fun `Login has correct path`() = assertEquals("login", Route.Login.path)
    @Test fun `Register has correct path`() = assertEquals("register", Route.Register.path)
    @Test fun `Map has correct path`() = assertEquals("map", Route.Map.path)
    @Test fun `List has correct path`() = assertEquals("list", Route.List.path)
    @Test fun `Add has correct path`() = assertEquals("add", Route.Add.path)
    @Test fun `MapFocus has correct path`() = assertEquals("map_focus", Route.MapFocus.path)
    @Test fun `Profile has correct path`() = assertEquals("profile", Route.Profile.path)
    @Test fun `PublicProfile has correct path`() = assertEquals("publicProfile", Route.PublicProfile.path)
    @Test fun `PlaceDetail has correct path`() = assertEquals("placeDetail", Route.PlaceDetail.path)
    @Test fun `Walks has correct path`() = assertEquals("walks", Route.Walks.path)
    @Test fun `WalkRecord has correct path`() = assertEquals("walks/record", Route.WalkRecord.path)
    @Test fun `WalkDetail has correct path`() = assertEquals("walks/detail", Route.WalkDetail.path)
    @Test fun `WalkFollow has correct path`() = assertEquals("walks/follow", Route.WalkFollow.path)
    @Test fun `Notifications has correct path`() = assertEquals("notifications", Route.Notifications.path)
    @Test fun `NotificationsSettings has correct path`() =
        assertEquals("notifications/settings", Route.NotificationsSettings.path)
    @Test fun `ProfileSetup has correct path`() = assertEquals("profileSetup", Route.ProfileSetup.path)

    @Test fun `all() returns exactly five routes`() {
        assertEquals(5, Route.all().size)
    }

    @Test fun `all() contains Splash`() = assertTrue(Route.all().contains(Route.Splash))
    @Test fun `all() contains Map`() = assertTrue(Route.all().contains(Route.Map))
    @Test fun `all() contains List`() = assertTrue(Route.all().contains(Route.List))
    @Test fun `all() contains Add`() = assertTrue(Route.all().contains(Route.Add))
    @Test fun `all() contains Walks`() = assertTrue(Route.all().contains(Route.Walks))

    @Test fun `all() does not contain Login`() = assertFalse(Route.all().contains(Route.Login))
    @Test fun `all() does not contain Profile`() = assertFalse(Route.all().contains(Route.Profile))

    @Test fun `each route path is non-empty`() {
        val routes = listOf(
            Route.Splash, Route.Login, Route.Register, Route.Map, Route.List,
            Route.Add, Route.MapFocus, Route.Profile, Route.PublicProfile,
            Route.PlaceDetail, Route.Walks, Route.WalkRecord, Route.WalkDetail,
            Route.WalkFollow, Route.Notifications, Route.NotificationsSettings, Route.ProfileSetup
        )
        for (route in routes) {
            assertTrue("Path for ${route::class.simpleName} must not be blank", route.path.isNotBlank())
        }
    }

    @Test fun `all route paths are unique`() {
        val routes = listOf(
            Route.Splash, Route.Login, Route.Register, Route.Map, Route.List,
            Route.Add, Route.MapFocus, Route.Profile, Route.PublicProfile,
            Route.PlaceDetail, Route.Walks, Route.WalkRecord, Route.WalkDetail,
            Route.WalkFollow, Route.Notifications, Route.NotificationsSettings, Route.ProfileSetup
        )
        val paths = routes.map { it.path }
        assertEquals("Route paths must be unique", paths.distinct().size, paths.size)
    }
}
