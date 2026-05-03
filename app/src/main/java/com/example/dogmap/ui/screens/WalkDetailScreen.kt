package com.example.dogmap.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun WalkDetailScreen(
    walkRemoteId: String,
    onBackClick: () -> Unit,
    onFollowClick: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val factory = LocalViewModelFactory.current
    val vm: WalkViewModel = viewModel(factory = factory)

    val walk by vm.observeWalk(walkRemoteId).collectAsState()
    val currentUid by vm.currentUid.collectAsState()
    val isLiked by vm.likeStateFor(walkRemoteId).collectAsState()
    val favoriteIds by vm.favoriteWalkIds.collectAsState()

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-0.3763, 39.4699))
            zoom(13.0)
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
                    .zoom(15.0)
                    .build()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.walk_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (w != null && w.authorId == currentUid) {
                        IconButton(onClick = {
                            vm.deleteWalk(w)
                            onDeleted()
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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
            }

            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (w == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                w.name.ifBlank { "Paseo" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (w.isPublic) {
                                Icon(
                                    Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (w.dogName.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(w.dogName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (w.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                w.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoChip(
                                stringResource(R.string.walk_distance_label),
                                String.format("%.2f km", w.distanceMeters / 1000.0)
                            )
                            InfoChip(
                                stringResource(R.string.walk_duration_label),
                                String.format("%d:%02d", w.durationSeconds / 60, w.durationSeconds % 60)
                            )
                            InfoChip(
                                stringResource(R.string.walk_points_label),
                                w.points.size.toString()
                            )
                        }

                        if (w.isPublic && w.remoteId.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { vm.toggleLike(w) },
                                    enabled = currentUid != null
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isLiked) MaterialTheme.colorScheme.error
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(w.likesCount.toString(), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { vm.toggleFavorite(w) },
                                    enabled = currentUid != null
                                ) {
                                    val isFav = w.remoteId in favoriteIds
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isFav) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { onFollowClick(walkRemoteId) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = w.points.size >= 2
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.walk_follow))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
