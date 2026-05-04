// File: app/src/main/java/com/example/dogmap/ui/screens/ProfileScreen.kt
// PawPlace profile — gradient avatar uses the same vocabulary as marker discs.
// Stat tiles, pet card and place cards all sit on glass surfaces.
// UserViewModel + DogViewModel logic preserved (edit sheet & delete dialog
// kept inline so the screen file remains self-contained).
package com.example.dogmap.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.dogmap.R
import com.example.dogmap.data.models.Dog
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.GlassCard
import com.example.dogmap.ui.components.MarkerGlyph
import com.example.dogmap.ui.components.PawPlaceBackground
import com.example.dogmap.ui.components.accentForType
import com.example.dogmap.ui.theme.BrandPrimary
import com.example.dogmap.viewmodel.DogViewModel
import com.example.dogmap.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onDogClick: (Dog) -> Unit,
    onPublicProfileClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val factory = LocalViewModelFactory.current
    val userVm: UserViewModel = viewModel(factory = factory)
    val dogVm: DogViewModel = viewModel(factory = factory)

    val state by userVm.state.collectAsState()
    val currentUid by dogVm.currentUid.collectAsState()
    val uid = currentUid ?: FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(uid) {
        if (uid != null) userVm.loadProfile(uid, includeFavorites = true)
    }

    var menuOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    PawPlaceBackground {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color(0xFFF2F6F8),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_label),
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = BrandPrimary.copy(alpha = 0.85f),
                    )
                    Text(
                        text = stringResource(R.string.my_profile),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2F6F8),
                        letterSpacing = (-0.4).sp,
                    )
                }
                Box {
                    GlassIconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color(0xFFF2F6F8))
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(Color(0xFF101A22)),
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title), color = Color(0xFFF2F6F8)) },
                            leadingIcon = { Icon(Icons.Default.Settings, null, tint = BrandPrimary) },
                            onClick = { menuOpen = false; onSettingsClick() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_profile), color = Color(0xFFF2F6F8)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = BrandPrimary) },
                            onClick = {
                                menuOpen = false
                                userVm.startEditing()
                                editOpen = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logout), color = Color(0xFFF2F6F8)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = BrandPrimary) },
                            onClick = { menuOpen = false; userVm.signOut(); onLogout() },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.delete_account),
                                    color = Color(0xFFFF9DB1),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    null,
                                    tint = Color(0xFFFF9DB1),
                                )
                            },
                            onClick = { menuOpen = false; deleteOpen = true },
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    ProfileHeader(
                        displayName = state.user?.displayName ?: "",
                        email = state.user?.email ?: "",
                        city = state.user?.city ?: "",
                        bio = state.user?.bio ?: "",
                        photoUrl = state.user?.photoUrl ?: "",
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatTile(
                            value = state.dogs.size.toString(),
                            label = stringResource(R.string.my_places),
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            value = state.favorites.size.toString(),
                            label = stringResource(R.string.favorites),
                            modifier = Modifier.weight(1f),
                            accent = Color(0xFF9FF0C8),
                        )
                    }
                }

                item {
                    PetCard(
                        petName = state.user?.petName ?: "",
                        petBreed = state.user?.petBreed ?: "",
                        petAge = state.user?.petAge ?: 0,
                        petPhotoUrl = state.user?.petPhotoUrl ?: "",
                        onAddPet = {
                            userVm.startEditing()
                            editOpen = true
                        },
                    )
                }

                item {
                    SegmentedTabs(
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                        labels = listOf(
                            stringResource(R.string.my_places),
                            stringResource(R.string.favorites),
                        ),
                    )
                }

                val list = if (selectedTab == 0) state.dogs else state.favorites
                if (list.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (selectedTab == 0)
                                    stringResource(R.string.no_locations)
                                else stringResource(R.string.no_favorites),
                                fontSize = 14.sp,
                                color = Color(0x99F2F6F8),
                            )
                        }
                    }
                } else {
                    items(list, key = { it.id.toString() + it.remoteId }) { dog ->
                        SmallPlaceCard(dog = dog, onClick = { onDogClick(dog) })
                    }
                }
            }
        }
    }

    if (editOpen) {
        ProfileEditSheet(
            userVm = userVm,
            onDismiss = { editOpen = false; userVm.cancelEditing() },
        )
    }
    if (deleteOpen) {
        DeleteAccountDialog(
            userVm = userVm,
            onDismiss = { deleteOpen = false; userVm.clearDeleteError() },
            onDeleted = onLogout,
        )
    }
}

/* ─── Header card with gradient avatar ─── */
@Composable
private fun ProfileHeader(
    displayName: String,
    email: String,
    city: String,
    bio: String,
    photoUrl: String,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(displayName = displayName.ifBlank { email }, photoUrl = photoUrl)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName.ifBlank { email.substringBefore("@") },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F6F8),
                )
                if (email.isNotBlank()) {
                    Text(
                        text = email,
                        fontSize = 12.sp,
                        color = Color(0x99F2F6F8),
                    )
                }
                if (city.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "📍  $city",
                        fontSize = 12.sp,
                        color = BrandPrimary,
                    )
                }
            }
        }
        if (bio.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = bio,
                fontSize = 13.sp,
                color = Color(0xCCF2F6F8),
                lineHeight = 19.sp,
            )
        }
    }
}

/* ─── Gradient avatar disc — same vocabulary as map markers ─── */
@Composable
private fun GradientAvatar(displayName: String, photoUrl: String, size: Int = 72) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        BrandPrimary.copy(alpha = 0.55f),
                        Color(0xFF5BC2EE).copy(alpha = 0.20f),
                    )
                )
            )
            .border(1.dp, BrandPrimary.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "·",
                fontSize = (size * 0.42f).sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
            )
        }
    }
}

/* ─── Stat tile ─── */
@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = BrandPrimary,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.10f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/* ─── Pet card ─── */
@Composable
internal fun PetCard(
    petName: String,
    petBreed: String,
    petAge: Int,
    petPhotoUrl: String,
    onAddPet: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Pets, null, tint = BrandPrimary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.my_pet),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
            )
        }

        Spacer(Modifier.height(14.dp))

        if (petName.isBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAddPet),
                shape = RoundedCornerShape(14.dp),
                color = Color(0x0AFFFFFF),
                border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.4f)),
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.add_pet),
                        fontSize = 13.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientAvatar(displayName = petName, photoUrl = petPhotoUrl, size = 56)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = petName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2F6F8),
                    )
                    if (petBreed.isNotBlank()) {
                        Text(
                            text = petBreed,
                            fontSize = 12.sp,
                            color = Color(0x99F2F6F8),
                        )
                    }
                    if (petAge > 0) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$petAge años",
                            fontSize = 11.sp,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

/* ─── Segmented tabs ─── */
@Composable
private fun SegmentedTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    labels: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            labels.forEachIndexed { i, label ->
                val on = selected == i
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { onSelect(i) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (on) BrandPrimary else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (on) Color(0xFF03161F) else Color(0xCCF2F6F8),
                        )
                    }
                }
            }
        }
    }
}

/* ─── Compact place card for the profile list ─── */
@Composable
private fun SmallPlaceCard(dog: Dog, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkerGlyph(type = dog.type, size = 36)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dog.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F6F8),
                )
                Text(
                    text = dog.type,
                    fontSize = 11.sp,
                    color = accentForType(dog.type),
                    fontWeight = FontWeight.Medium,
                )
            }
            if (dog.rating > 0f) {
                Text(
                    text = "★ ${dog.rating.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                )
            }
        }
    }
}

/* ─── Glass icon button (matches map's GlassFab shape) ─── */
@Composable
private fun GlassIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xA6101A22),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/* ───────────────────────────────────────────────────────────────────
   Edit profile sheet & delete account dialog — kept inline.
   ─────────────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditSheet(
    userVm: UserViewModel,
    onDismiss: () -> Unit,
) {
    val state by userVm.state.collectAsState()
    val draft = state.editDraft ?: return

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) userVm.updateDraft { it.copy(photoUrl = uri.toString()) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B141A),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.edit_profile),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
            )

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.15f))
                    .border(1.dp, BrandPrimary.copy(alpha = 0.45f), CircleShape)
                    .clickable { photoLauncher.launch("image/*") }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                if (draft.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = draft.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            GlassTextField(
                value = draft.displayName,
                onValueChange = { v -> userVm.updateDraft { it.copy(displayName = v) } },
                label = stringResource(R.string.display_name),
                isError = draft.displayName.isBlank(),
                errorText = if (draft.displayName.isBlank())
                    stringResource(R.string.error_display_name) else null,
            )
            GlassTextField(
                value = draft.city,
                onValueChange = { v -> userVm.updateDraft { it.copy(city = v) } },
                label = stringResource(R.string.city),
                isError = draft.city.isBlank(),
                errorText = if (draft.city.isBlank())
                    stringResource(R.string.enter_city) else null,
            )
            GlassTextField(
                value = draft.bio,
                onValueChange = { v -> userVm.updateDraft { it.copy(bio = v) } },
                label = stringResource(R.string.bio),
                singleLine = false,
            )

            Text(
                text = stringResource(R.string.my_pet),
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp),
            )

            GlassTextField(
                value = draft.petName,
                onValueChange = { v -> userVm.updateDraft { it.copy(petName = v) } },
                label = stringResource(R.string.pet_name),
            )
            GlassTextField(
                value = draft.petBreed,
                onValueChange = { v -> userVm.updateDraft { it.copy(petBreed = v) } },
                label = stringResource(R.string.pet_breed),
            )
            GlassTextField(
                value = if (draft.petAge == 0) "" else draft.petAge.toString(),
                onValueChange = { v ->
                    val parsed = v.filter(Char::isDigit).toIntOrNull() ?: 0
                    userVm.updateDraft { it.copy(petAge = parsed) }
                },
                label = stringResource(R.string.pet_age),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                ) { Text(stringResource(R.string.cancel), color = Color(0xFFF2F6F8)) }
                CyanButton(
                    onClick = { userVm.saveProfile(onDone = onDismiss) },
                    enabled = !state.isSaving && draft.displayName.isNotBlank() && draft.city.isNotBlank(),
                    isLoading = state.isSaving,
                    label = stringResource(R.string.save),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    userVm: UserViewModel,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by userVm.state.collectAsState()
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.isDeleting) onDismiss() },
        containerColor = Color(0xFF0B141A),
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9DB1),
            )
        },
        title = { Text(stringResource(R.string.delete_account_title), color = Color(0xFFF2F6F8)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.delete_account_message),
                    color = Color(0xCCF2F6F8),
                )
                GlassTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.delete_account_password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.deleteError != null,
                    errorText = state.deleteError,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) {
                Text(stringResource(R.string.cancel), color = Color(0xCCF2F6F8))
            }
        },
        confirmButton = {
            Button(
                onClick = { userVm.deleteAccount(password, onDeleted) },
                enabled = password.isNotBlank() && !state.isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9DB1)),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF03161F),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.delete_account_confirm),
                        color = Color(0xFF03161F),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    )
}
