package com.example.dogmap.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.data.models.Dog
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.GlassPlaceSheet
import com.example.dogmap.viewmodel.DogViewModel
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

private const val PAWPLACE_DARK_STYLE_URL      = "mapbox://styles/mapbox/dark-v11"
private const val PAWPLACE_LIGHT_STYLE_URL     = "mapbox://styles/mapbox/light-v11"
private const val PAWPLACE_SATELLITE_URL       = "mapbox://styles/mapbox/satellite-streets-v12"

private fun iconResForType(type: String): Int = when {
    type.equals("parque", ignoreCase = true) || type.equals("park", ignoreCase = true) ->
        R.drawable.ic_marker_park
    type.equals("playa", ignoreCase = true) || type.equals("beach", ignoreCase = true) ->
        R.drawable.ic_marker_beach
    type.equals("restaurante", ignoreCase = true) || type.equals("restaurant", ignoreCase = true) ->
        R.drawable.ic_marker_restaurant
    type.equals("veterinario", ignoreCase = true) || type.equals("vet", ignoreCase = true) ->
        R.drawable.ic_marker_vet
    else -> R.drawable.ic_pawplace_marker
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun MapScreen(
    focusLat: Double? = null,
    focusLon: Double? = null,
    onBackClick: (() -> Unit)? = null,
    onAddClick: (Double, Double) -> Unit = { _, _ -> },
    onDogDetailClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val factory = LocalViewModelFactory.current
    val viewModel: DogViewModel = viewModel(factory = factory)
    val allDogs by viewModel.allDogs.collectAsState(initial = emptyList())
    val publicDogs by viewModel.publicDogs.collectAsState()
    val mapFilter by viewModel.mapFilter.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isDark = isSystemInDarkTheme()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val visibleDogs = remember(mapFilter, allDogs, publicDogs) {
        when (mapFilter) {
            com.example.dogmap.viewmodel.MapFilter.PERSONAL -> allDogs
            com.example.dogmap.viewmodel.MapFilter.COMMUNITY -> publicDogs
            com.example.dogmap.viewmodel.MapFilter.ALL ->
                (allDogs + publicDogs).distinctBy { it.remoteId.ifBlank { it.id.toString() } }
        }
    }

    var isSatellite by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<Point?>(null) }
    var selectedDog by remember { mutableStateOf<Dog?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val markerIcon = rememberIconImage(
        key = R.drawable.ic_pawplace_marker,
        painter = painterResource(R.drawable.ic_pawplace_marker)
    )
    val markerActiveIcon = rememberIconImage(
        key = R.drawable.ic_pawplace_marker_active,
        painter = painterResource(R.drawable.ic_pawplace_marker_active)
    )
    val newPinIcon = rememberIconImage(
        key = R.drawable.ic_pawplace_new_pin,
        painter = painterResource(R.drawable.ic_pawplace_new_pin)
    )
    val markerParkIcon = rememberIconImage(
        key = R.drawable.ic_marker_park,
        painter = painterResource(R.drawable.ic_marker_park)
    )
    val markerBeachIcon = rememberIconImage(
        key = R.drawable.ic_marker_beach,
        painter = painterResource(R.drawable.ic_marker_beach)
    )
    val markerRestaurantIcon = rememberIconImage(
        key = R.drawable.ic_marker_restaurant,
        painter = painterResource(R.drawable.ic_marker_restaurant)
    )
    val markerVetIcon = rememberIconImage(
        key = R.drawable.ic_marker_vet,
        painter = painterResource(R.drawable.ic_marker_vet)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloRadius by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing)),
        label = "halo_radius"
    )
    val haloOpacity by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing)),
        label = "halo_opacity"
    )

    val viewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(-0.3763, 39.4699))
            zoom(13.0)
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(focusLat, focusLon) {
        if (focusLat != null && focusLon != null) {
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(focusLon, focusLat))
                    .zoom(16.0).pitch(35.0).build()
            )
        }
    }

    LaunchedEffect(selectedDog) {
        if (selectedDog != null) {
            viewportState.flyTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(selectedDog!!.longitude, selectedDog!!.latitude))
                    .pitch(35.0).zoom(15.5).build()
            )
        } else if (focusLat == null && focusLon == null) {
            viewportState.flyTo(
                CameraOptions.Builder().pitch(0.0).build()
            )
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewportState,
            style = {
                MapStyle(
                    style = when {
                        isSatellite -> PAWPLACE_SATELLITE_URL
                        isDark -> PAWPLACE_DARK_STYLE_URL
                        else -> PAWPLACE_LIGHT_STYLE_URL
                    }
                )
            },
            onMapClickListener = { point ->
                val zoom = viewportState.cameraState?.zoom ?: 13.0
                val threshold = (0.002 * Math.pow(2.0, 13.0 - zoom)).coerceIn(0.00025, 0.01)
                val latCos = kotlin.math.cos(Math.toRadians(point.latitude()))
                val tappedDog = visibleDogs.minByOrNull { dog ->
                    val dlat = dog.latitude - point.latitude()
                    val dlon = (dog.longitude - point.longitude()) * latCos
                    dlat * dlat + dlon * dlon
                }?.takeIf { dog ->
                    val dlat = dog.latitude - point.latitude()
                    val dlon = (dog.longitude - point.longitude()) * latCos
                    kotlin.math.sqrt(dlat * dlat + dlon * dlon) < threshold
                }
                if (tappedDog != null) {
                    selectedDog = tappedDog; selectedLocation = null
                } else if (selectedDog == null) {
                    selectedLocation = point
                }
                true
            }
        ) {
            val shouldFlyToUser = focusLat == null
            MapEffect(Unit) { mapView ->
                var hasFlownToUser = false
                mapView.location.apply {
                    enabled = true
                    pulsingEnabled = true
                    pulsingColor = android.graphics.Color.parseColor("#FF7DD8FF")
                    locationPuck = createDefault2DPuck(withBearing = false)
                    if (shouldFlyToUser) {
                        addOnIndicatorPositionChangedListener { p ->
                            if (!hasFlownToUser) {
                                hasFlownToUser = true
                                mapView.camera.flyTo(
                                    CameraOptions.Builder().center(p).zoom(15.0).build()
                                )
                            }
                        }
                    }
                }
            }
            MapEffect(isSatellite) { mapView ->
                fun applySpanish() {
                    val style = mapView.mapboxMap.style ?: return
                    val spanishExpr = Value.fromJson(
                        """["coalesce",["get","name:es"],["get","name"]]"""
                    ).value ?: return
                    style.styleLayers
                        .filter { it.type == "symbol" }
                        .forEach { layer ->
                            val existing = style.getStyleLayerProperty(layer.id, "text-field")
                            if (existing.value.toString() != "null") {
                                style.setStyleLayerProperty(layer.id, "text-field", spanishExpr)
                            }
                        }
                }
                applySpanish()
                mapView.mapboxMap.subscribeStyleLoaded { applySpanish() }
            }

            selectedLocation?.let { p ->
                CircleAnnotation(point = p) {
                    circleRadius = haloRadius.toDouble()
                    circleColor = Color(0xFFF5C77E)
                    circleOpacity = haloOpacity.toDouble()
                    circleStrokeWidth = 0.0
                }
            }
            selectedLocation?.let { p ->
                PointAnnotation(point = p) { iconImage = newPinIcon }
            }

            visibleDogs.forEach { dog ->
                val isActive = selectedDog?.remoteId?.let { it == dog.remoteId } == true
                val typeIcon = when {
                    isActive -> markerActiveIcon
                    else -> when (iconResForType(dog.type)) {
                        R.drawable.ic_marker_park -> markerParkIcon
                        R.drawable.ic_marker_beach -> markerBeachIcon
                        R.drawable.ic_marker_restaurant -> markerRestaurantIcon
                        R.drawable.ic_marker_vet -> markerVetIcon
                        else -> markerIcon
                    }
                }
                PointAnnotation(
                    point = Point.fromLngLat(dog.longitude, dog.latitude),
                    onClick = {
                        selectedDog = dog; selectedLocation = null; true
                    }
                ) {
                    iconImage = typeIcon
                }
            }
        }

        // ─── Filter chips ──────────────────────────────────────────────
        if (focusLat == null) {
            val chipBg = if (isDark) Color(0x14FFFFFF) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            val chipBorder = if (isDark) Color(0x14FFFFFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            val chipTextUnsel = if (isDark) Color(0xCCF2F6F8) else MaterialTheme.colorScheme.onSurface

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    com.example.dogmap.viewmodel.MapFilter.ALL to stringResource(R.string.filter_all),
                    com.example.dogmap.viewmodel.MapFilter.PERSONAL to stringResource(R.string.filter_mine),
                    com.example.dogmap.viewmodel.MapFilter.COMMUNITY to stringResource(R.string.filter_community)
                ).forEach { (filter, label) ->
                    val on = mapFilter == filter
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (on) com.example.dogmap.ui.theme.BrandPrimary else chipBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (on) Color(0x66FFFFFF) else chipBorder
                        ),
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickableNoRipple { viewModel.mapFilter.value = filter }
                    ) {
                        Box(
                            Modifier.fillMaxHeight().padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (on) Color(0xFF0A1218) else chipTextUnsel,
                            )
                        }
                    }
                }
            }
        }

        // ─── Right side: floating glass controls ───────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassFab(onClick = { isSatellite = !isSatellite }) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = stringResource(R.string.satellite),
                    tint = if (isSatellite) com.example.dogmap.ui.theme.BrandPrimary else Color(0xCCF2F6F8)
                )
            }
            GlassFab(onClick = {
                viewportState.flyTo(
                    CameraOptions.Builder().zoom(15.0).build()
                )
            }) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = com.example.dogmap.ui.theme.BrandPrimary
                )
            }
        }

        // ─── Back button (focus mode) ──────────────────────────────────
        if (focusLat != null && focusLon != null && onBackClick != null) {
            GlassFab(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFFF2F6F8)
                )
            }
        }

        // ─── "Add location" extended FAB ───────────────────────────────
        AnimatedVisibility(
            visible = selectedLocation != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedLocation?.let { p -> onAddClick(p.latitude(), p.longitude()) }
                },
                icon = { Icon(Icons.Default.AddLocation, contentDescription = null) },
                text = { Text(stringResource(R.string.add_location)) },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFFF5C77E),
                contentColor = Color(0xFF03161F),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        )
    }

    // ─── Bottom sheet for selected dog ─────────────────────────────────
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLiked by viewModel.likeStateFor(selectedDog?.remoteId ?: "").collectAsState()

    if (selectedDog != null) {
        val dog = selectedDog!!
        val isFavorite by remember(dog.remoteId, favoriteIds) {
            derivedStateOf { dog.remoteId in favoriteIds }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedDog = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            scrimColor = Color(0x66000000),
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))
                )
            }
        ) {
            GlassPlaceSheet(
                dog = dog,
                isLiked = isLiked,
                isFavorite = isFavorite,
                onLike = { viewModel.toggleLike(dog) },
                onFavorite = { viewModel.toggleFavorite(dog) },
                onAuthorClick = { id ->
                    onAuthorClick(id); selectedDog = null
                },
                onViewDetail = {
                    val id = if (dog.remoteId.isNotBlank()) dog.remoteId else dog.id.toString()
                    onDogDetailClick(id)
                    selectedDog = null
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, buildString {
                            append(dog.name)
                            if (dog.description.isNotBlank()) append("\n${dog.description}")
                            append("\nhttps://maps.google.com/?q=${dog.latitude},${dog.longitude}")
                        })
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
            )
        }
    }
}

@Composable
private fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.size(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xA6101A22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x14FFFFFF)),
        shadowElevation = 12.dp,
        onClick = onClick
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.background(Color.Transparent)
            .pointerInput(onClick) {
                detectTapGestures { onClick() }
            }
    )
