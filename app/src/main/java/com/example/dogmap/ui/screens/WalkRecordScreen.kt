package com.example.dogmap.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.data.models.Dog
import com.example.dogmap.data.models.TrackPoint
import com.example.dogmap.service.WalkTrackingService
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.viewmodel.DogViewModel
import com.example.dogmap.viewmodel.WalkViewModel
import com.mapbox.geojson.Point
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
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, MapboxExperimental::class)
@Composable
fun WalkRecordScreen(
    onBackClick: () -> Unit,
    onWalkSaved: (String) -> Unit
) {
    val factory = LocalViewModelFactory.current
    val walkVm: WalkViewModel = viewModel(factory = factory)
    val dogVm: DogViewModel = viewModel(factory = factory)
    val context = LocalContext.current

    val state by walkVm.recordingState.collectAsState()
    val livePoints by walkVm.liveTrackPoints.collectAsState()
    val distanceMeters by walkVm.liveDistanceMeters.collectAsState()
    val durationSeconds by walkVm.liveDurationSeconds.collectAsState()
    val allDogsRaw by dogVm.allDogs.collectAsState(initial = emptyList())
    val dogs = remember(allDogsRaw) { allDogsRaw.filter { !it.isPlace } }

    var showSaveSheet by remember { mutableStateOf(false) }
    var saveSnapshot by remember {
        mutableStateOf<Triple<List<TrackPoint>, Double, Long>?>(null)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* el sevicio comprueba el permiso al iniciar */ }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* opcional para Android 13+ */ }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.walk_record_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state == WalkTrackingService.State.Idle) onBackClick()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                    mapView.location.apply {
                        enabled = true
                        pulsingEnabled = true
                        locationPuck = createDefault2DPuck(withBearing = false)
                    }
                }

                if (livePoints.size >= 2) {
                    PolylineAnnotation(
                        points = livePoints.map { Point.fromLngLat(it.lon, it.lat) }
                    ) {
                        lineColor = Color(0xFF2E7D6B)
                        lineWidth = 6.0
                    }
                }

                livePoints.firstOrNull()?.let { tp ->
                    PointAnnotation(point = Point.fromLngLat(tp.lon, tp.lat)) {
                        iconImage = startIcon
                    }
                }
            }

            // Overlay con stats
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format("%.2f", distanceMeters / 1000.0),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("km", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        Text(
                            String.format("%d:%02d", mins, secs),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("tiempo", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            livePoints.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("puntos", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Botones de control
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    WalkTrackingService.State.Idle -> {
                        ExtendedFloatingActionButton(
                            onClick = { walkVm.startRecording(context) },
                            icon = { Icon(Icons.Default.PlayArrow, null) },
                            text = { Text(stringResource(R.string.walk_start)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    WalkTrackingService.State.Recording -> {
                        FloatingActionButton(
                            onClick = { walkVm.pauseRecording(context) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.walk_pause))
                        }
                        ExtendedFloatingActionButton(
                            onClick = {
                                saveSnapshot = WalkTrackingService.snapshot()
                                walkVm.stopRecording(context)
                                showSaveSheet = true
                            },
                            icon = { Icon(Icons.Default.Stop, null) },
                            text = { Text(stringResource(R.string.walk_stop)) },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                    WalkTrackingService.State.Paused -> {
                        ExtendedFloatingActionButton(
                            onClick = { walkVm.resumeRecording(context) },
                            icon = { Icon(Icons.Default.PlayArrow, null) },
                            text = { Text(stringResource(R.string.walk_resume)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                saveSnapshot = WalkTrackingService.snapshot()
                                walkVm.stopRecording(context)
                                showSaveSheet = true
                            },
                            icon = { Icon(Icons.Default.Stop, null) },
                            text = { Text(stringResource(R.string.walk_stop)) },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }

    if (showSaveSheet) {
        val snap = saveSnapshot
        if (snap != null) {
            SaveWalkSheet(
                points = snap.first,
                distanceMeters = snap.second,
                durationSeconds = snap.third,
                userDogs = dogs,
                onDismiss = {
                    showSaveSheet = false
                    walkVm.resetRecording()
                    onBackClick()
                },
                onSave = { name, description, dogId, dogName, isPublic ->
                    walkVm.saveRecordedWalk(
                        name = name,
                        description = description,
                        dogId = dogId,
                        dogName = dogName,
                        isPublic = isPublic,
                        points = snap.first,
                        distanceMeters = snap.second,
                        durationSeconds = snap.third,
                        onSaved = { saved ->
                            showSaveSheet = false
                            onWalkSaved(saved.remoteId.ifBlank { saved.id.toString() })
                        }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveWalkSheet(
    points: List<TrackPoint>,
    distanceMeters: Double,
    durationSeconds: Long,
    userDogs: List<Dog>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, dogId: String, dogName: String, isPublic: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var selectedDog by remember(userDogs) { mutableStateOf(userDogs.firstOrNull()) }
    var dropdownOpen by remember { mutableStateOf(false) }
    var dogNameText by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.walk_save_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Stats resumen
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip("Distancia", String.format("%.2f km", distanceMeters / 1000.0))
                StatChip(
                    "Duración",
                    String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60)
                )
                StatChip("Puntos", points.size.toString())
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.walk_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.walk_description)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Dropdown de perro
            if (userDogs.isEmpty()) {
                OutlinedTextField(
                    value = dogNameText,
                    onValueChange = { dogNameText = it },
                    label = { Text(stringResource(R.string.walk_select_dog)) },
                    placeholder = { Text("Opcional") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = dropdownOpen,
                    onExpandedChange = { dropdownOpen = !dropdownOpen }
                ) {
                    OutlinedTextField(
                        value = selectedDog?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.walk_select_dog)) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownOpen,
                        onDismissRequest = { dropdownOpen = false }
                    ) {
                        userDogs.forEach { dog ->
                            DropdownMenuItem(
                                text = { Text(dog.name) },
                                onClick = {
                                    selectedDog = dog
                                    dropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.walk_visibility),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.walk_visibility_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.walk_discard)) }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val dog = selectedDog
                            onSave(
                                name,
                                description,
                                dog?.remoteId?.ifBlank { dog.id.toString() } ?: "",
                                dog?.name ?: dogNameText.trim(),
                                isPublic
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank()
                ) { Text(stringResource(R.string.walk_save)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
