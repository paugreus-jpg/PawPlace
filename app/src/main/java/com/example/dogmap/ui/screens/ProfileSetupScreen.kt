package com.example.dogmap.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dogmap.R
import com.example.dogmap.data.models.User
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit
) {
    val factory = LocalViewModelFactory.current
    val viewModel: UserViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentUser = FirebaseAuth.getInstance().currentUser

    var displayName by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var bio by rememberSaveable { mutableStateOf("") }
    var petName by rememberSaveable { mutableStateOf("") }
    var petBreed by rememberSaveable { mutableStateOf("") }
    var petAge by rememberSaveable { mutableStateOf("") }
    var petPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) photoUri = uri
    }
    val petPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) petPhotoUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "¡Bienvenido a PawPlace!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Configura tu perfil para empezar",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // --- Foto de perfil ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Foto de perfil",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.display_name) + " *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showErrors && displayName.isBlank(),
                        supportingText = if (showErrors && displayName.isBlank()) {
                            { Text(stringResource(R.string.error_display_name)) }
                        } else null
                    )

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text(stringResource(R.string.city) + " *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = showErrors && city.isBlank(),
                        supportingText = if (showErrors && city.isBlank()) {
                            { Text(stringResource(R.string.enter_city)) }
                        } else null
                    )

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { photoLauncher.launch("image/*") }
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Añadir foto",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        if (photoUri != null) "Toca para cambiar" else "Toca para añadir foto de perfil",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text(stringResource(R.string.bio)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Mascota ---
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.add_pet),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = petName,
                        onValueChange = { petName = it },
                        label = { Text(stringResource(R.string.pet_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = petBreed,
                        onValueChange = { petBreed = it },
                        label = { Text(stringResource(R.string.pet_breed)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = petAge,
                        onValueChange = { petAge = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.pet_age)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (petPhotoUri != null) {
                        AsyncImage(
                            model = petPhotoUri,
                            contentDescription = "Foto de mascota",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        OutlinedButton(
                            onClick = { petPhotoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text(stringResource(R.string.change_pet_photo)) }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                .clickable { petPhotoLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Añadir foto",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Añadir foto de mascota",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (displayName.isBlank() || city.isBlank()) {
                        showErrors = true
                        return@Button
                    }
                    val draft = User(
                        uid = uid,
                        email = currentUser?.email ?: "",
                        displayName = displayName.trim(),
                        city = city.trim(),
                        photoUrl = photoUri?.toString() ?: "",
                        bio = bio.trim(),
                        petName = petName.trim(),
                        petBreed = petBreed.trim(),
                        petAge = petAge.toIntOrNull() ?: 0,
                        petPhotoUrl = petPhotoUri?.toString() ?: ""
                    )
                    viewModel.saveProfileSetup(uid, draft) { onSetupComplete() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !state.isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Continuar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.skipProfileSetup(uid) { onSetupComplete() } },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Omitir por ahora",
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}
