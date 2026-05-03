---
name: navigation-manager
description: >
  Manages Jetpack Compose Navigation for the Dogmap Android project, including route definitions,
  NavHost configuration, argument passing between screens, and deep linking. Use this skill
  whenever you need to: add a new screen to the navigation graph, pass arguments (like a dog ID)
  between screens, handle back navigation, set up bottom navigation or drawer navigation,
  configure the start destination, or debug navigation issues. Trigger when anyone mentions
  "navigation", "route", "NavController", "NavHost", "navigate", "back stack", "screen transition",
  "deep link", or wants to connect a new screen to the app flow in Dogmap.
---

# Navigation Manager — Dogmap

## Navigation Stack

- **Library:** `androidx.navigation:navigation-compose:2.8.4`
- **Pattern:** Centralized NavHost with sealed route definitions

## Route Definitions

Keep all routes in a single sealed class or object for type safety:

```kotlin
package com.example.dogmap.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object DogList : Screen("dog_list")
    object DogDetail : Screen("dog_detail/{dogId}") {
        fun createRoute(dogId: Int) = "dog_detail/$dogId"
    }
    object AddDog : Screen("add_dog")
    object EditDog : Screen("edit_dog/{dogId}") {
        fun createRoute(dogId: Int) = "edit_dog/$dogId"
    }
    object MapView : Screen("map_view")
}
```

## NavHost Setup

```kotlin
@Composable
fun DogmapNavHost(
    navController: NavHostController,
    viewModel: DogViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                navController.navigate(Screen.DogList.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.DogList.route) {
            DogListScreen(
                viewModel = viewModel,
                onDogClick = { dogId ->
                    navController.navigate(Screen.DogDetail.createRoute(dogId))
                },
                onAddDog = {
                    navController.navigate(Screen.AddDog.route)
                },
                onMapView = {
                    navController.navigate(Screen.MapView.route)
                }
            )
        }

        composable(
            route = Screen.DogDetail.route,
            arguments = listOf(navArgument("dogId") { type = NavType.IntType })
        ) { backStackEntry ->
            val dogId = backStackEntry.arguments?.getInt("dogId") ?: return@composable
            DogDetailScreen(
                viewModel = viewModel,
                dogId = dogId,
                onNavigateBack = { navController.popBackStack() },
                onEditDog = { navController.navigate(Screen.EditDog.createRoute(dogId)) }
            )
        }

        composable(Screen.AddDog.route) {
            AddDogScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MapView.route) {
            MapScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onDogClick = { dogId ->
                    navController.navigate(Screen.DogDetail.createRoute(dogId))
                }
            )
        }
    }
}
```

## Navigation Conventions

1. **Never pass NavController to screens.** Use callback lambdas (`onNavigateBack`, `onDogClick`). This makes screens testable and previewable.

2. **Pop the splash screen from the back stack** — users shouldn't be able to "go back" to it:
   ```kotlin
   navController.navigate(Screen.DogList.route) {
       popUpTo(Screen.Splash.route) { inclusive = true }
   }
   ```

3. **Avoid duplicate destinations** on fast taps:
   ```kotlin
   navController.navigate(route) {
       launchSingleTop = true
   }
   ```

4. **Arguments must match** between the route template (`{dogId}`) and `navArgument("dogId")`.

## Adding a New Screen — Step by Step

1. Add a new entry in the `Screen` sealed class with its route.
2. Add the `composable(...)` block inside `NavHost`.
3. Wire navigation callbacks from the calling screen.
4. Create the actual `@Composable` screen file (see ui-specialist skill).
5. If the screen needs arguments, define them with `navArgument` and extract from `backStackEntry`.

## Passing Complex Data

Don't pass complex objects through navigation arguments. Instead:
- Pass only the ID (Int or String)
- Let the destination screen's ViewModel load the full object by ID

```kotlin
// Good — pass ID only
onDogClick = { dogId -> navController.navigate(Screen.DogDetail.createRoute(dogId)) }

// In DogDetailScreen
LaunchedEffect(dogId) {
    viewModel.loadDogById(dogId)
}
```
