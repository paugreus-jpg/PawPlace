package com.example.dogmap.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.viewmodel.WalkViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.bindgen.Value
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun WalkFollowScreen(
    walkRemoteId: String,
    onBackClick: () -> Unit
) {
    val factory = LocalViewModelFactory.current
    val vm: WalkViewModel = viewModel(factory = factory)
    val walk by vm.observeWalk(walkRemoteId).collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Mapbox interno */ }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-0.3763, 39.4699))
            zoom(15.0)
        }
    }

    val startIcon = rememberIconImage(
        key = R.drawable.ic_walk_start,
        painter = painterResource(R.drawable.ic_walk_start)
    )
    val endIcon = rememberIconImage(
        key = R.drawable.ic_walk_end,
        painter = painterResource(R.drawable.ic_walk_end)
    )

    val w = walk
    LaunchedEffect(w?.points) {
        val pts = w?.points
        if (!pts.isNullOrEmpty()) {
            val first = pts.first()
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(first.lon, first.lat))
                    .zoom(16.0)
                    .build()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.walk_follow_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewportState,
                style = { MapStyle(style = Style.STANDARD) }
            ) {
                MapEffect(Unit) { mapView ->
                    mapView.mapboxMap.subscribeStyleLoaded {
                        mapView.mapboxMap.setStyleImportConfigProperty(
                            "basemap", "language", Value.valueOf("es")
                        )
                    }
                }
                MapEffect(Unit) { mapView ->
                    var hasFlown = false
                    mapView.location.apply {
                        enabled = true
                        pulsingEnabled = true
                        locationPuck = createDefault2DPuck(withBearing = true)
                        addOnIndicatorPositionChangedListener { p ->
                            if (!hasFlown && (w?.points?.isEmpty() == true)) {
                                hasFlown = true
                                mapView.camera.flyTo(
                                    CameraOptions.Builder().center(p).zoom(16.0).build()
                                )
                            }
                        }
                    }
                }

                val pts = w?.points.orEmpty()
                if (pts.size >= 2) {
                    PolylineAnnotation(
                        points = pts.map { Point.fromLngLat(it.lon, it.lat) }
                    ) {
                        lineColor = Color(0xFF2E7D6B)
                        lineWidth = 6.0
                    }
                }
                pts.firstOrNull()?.let {
                    PointAnnotation(point = Point.fromLngLat(it.lon, it.lat)) {
                        iconImage = startIcon
                    }
                }
                pts.lastOrNull()?.takeIf { pts.size >= 2 }?.let {
                    PointAnnotation(point = Point.fromLngLat(it.lon, it.lat)) {
                        iconImage = endIcon
                    }
                }
            }

            if (w != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            w.name.ifBlank { "Paseo" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            String.format(
                                "%.2f km · %d:%02d",
                                w.distanceMeters / 1000.0,
                                w.durationSeconds / 60,
                                w.durationSeconds % 60
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
