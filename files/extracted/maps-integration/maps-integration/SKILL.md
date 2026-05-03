---
name: maps-integration
description: >
  Handles Google Maps integration in the Dogmap Android project using Maps Compose SDK for
  displaying dog locations, markers, camera positioning, and user location. Use this skill
  whenever you need to: display dogs on a map, add or customize map markers, handle map camera
  movement, get or display the user's current location, draw geofences or radius circles,
  implement map clustering for many markers, handle map click events, or configure the Google
  Maps API key. Trigger when anyone mentions "map", "marker", "location", "GPS", "coordinates",
  "latitude", "longitude", "camera", "geolocation", "Google Maps", or wants to visualize dog
  positions on a map in Dogmap.
---

# Maps Integration — Dogmap

## Dependencies

- **Google Play Services Maps:** `com.google.android.gms:play-services-maps:19.0.0`
- **Maps Compose:** `com.google.maps.android:maps-compose:6.2.1`

## API Key Configuration

The Google Maps API key must be in `local.properties` (not committed to git):
```properties
MAPS_API_KEY=your_actual_key_here
```

And referenced in `AndroidManifest.xml`:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

In `build.gradle.kts`, read from local.properties:
```kotlin
android {
    defaultConfig {
        manifestPlaceholders["MAPS_API_KEY"] = 
            project.findProperty("MAPS_API_KEY") as? String ?: ""
    }
}
```

## Basic Map Screen

```kotlin
package com.example.dogmap.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.dogmap.R

@Composable
fun MapScreen(
    viewModel: DogViewModel,
    onNavigateBack: () -> Unit,
    onDogClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Default center — adjust to your region or user's location
    val defaultPosition = LatLng(43.23, -5.69) // Asturias, Spain
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Perros") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            ),
            properties = MapProperties(
                isMyLocationEnabled = false // Set true after permission check
            )
        ) {
            uiState.dogs.forEach { dog ->
                Marker(
                    state = MarkerState(position = LatLng(dog.latitude, dog.longitude)),
                    title = dog.name,
                    snippet = dog.breed,
                    onClick = {
                        onDogClick(dog.id)
                        true // consume the event
                    }
                )
            }
        }
    }
}
```

## Custom Marker with Info Window

```kotlin
@Composable
fun DogMarkerInfoWindow(dog: Dog) {
    MarkerInfoWindowContent(
        state = MarkerState(position = LatLng(dog.latitude, dog.longitude)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = dog.imageUri ?: R.drawable.placeholder_dog,
                contentDescription = dog.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Text(dog.name, style = MaterialTheme.typography.titleSmall)
            Text(dog.breed, style = MaterialTheme.typography.bodySmall)
        }
    }
}
```

## Location Permissions

Before enabling `isMyLocationEnabled`, request runtime permissions:

```kotlin
val locationPermissionState = rememberMultiplePermissionsState(
    listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
)

LaunchedEffect(Unit) {
    if (!locationPermissionState.allPermissionsGranted) {
        locationPermissionState.launchMultiplePermissionRequest()
    }
}

GoogleMap(
    properties = MapProperties(
        isMyLocationEnabled = locationPermissionState.allPermissionsGranted
    )
)
```

Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Camera Animation

Move the camera to a specific dog or to fit all markers:

```kotlin
// Animate to a single dog
LaunchedEffect(selectedDog) {
    selectedDog?.let {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(it.latitude, it.longitude), 15f
            )
        )
    }
}

// Fit all dogs in view
LaunchedEffect(uiState.dogs) {
    if (uiState.dogs.isNotEmpty()) {
        val bounds = LatLngBounds.builder().apply {
            uiState.dogs.forEach { include(LatLng(it.latitude, it.longitude)) }
        }.build()
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(bounds, 100) // 100px padding
        )
    }
}
```

## Adding a Dog at a Tapped Location

```kotlin
GoogleMap(
    onMapClick = { latLng ->
        viewModel.setNewDogLocation(latLng.latitude, latLng.longitude)
        // Show add-dog bottom sheet or navigate to add screen
    }
)
```

## Checklist for Map Features

1. [ ] API key configured in `local.properties` and `AndroidManifest.xml`
2. [ ] Location permissions declared and requested at runtime
3. [ ] `import com.dogmap.R` in any file referencing drawable markers
4. [ ] Camera position defaults to a sensible location
5. [ ] Markers wired to ViewModel dog list via `collectAsState()`
6. [ ] Handled empty state (no dogs = no markers, maybe show a message)
