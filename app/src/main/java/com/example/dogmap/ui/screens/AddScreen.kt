// File: app/src/main/java/com/example/dogmap/ui/screens/AddScreen.kt
// PawPlace add place — type chips show the actual map glyph the user is choosing.
// Custom glass switch matches the map's floating controls.
// DogViewModel + persistence preserved exactly.
package com.example.dogmap.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
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
import com.example.dogmap.ui.components.RatingBar
import com.example.dogmap.ui.components.accentForType
import com.example.dogmap.ui.components.typeKey
import com.example.dogmap.ui.theme.BrandPrimary
import com.example.dogmap.viewmodel.DogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    lat: Double,
    lon: Double,
) {
    val factory = LocalViewModelFactory.current
    val viewModel = viewModel<DogViewModel>(factory = factory)

    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedTypeIndex by rememberSaveable { mutableStateOf(0) }
    var rating by rememberSaveable { mutableStateOf(0f) }
    var isPublic by rememberSaveable { mutableStateOf(false) }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showError by rememberSaveable { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) imageUri = uri }

    val typeKeys = listOf("park", "beach", "restaurant", "vet", "cafe")
    val typeOptions = listOf(
        stringResource(R.string.type_park),
        stringResource(R.string.type_beach),
        stringResource(R.string.type_restaurant),
        stringResource(R.string.type_vet),
        stringResource(R.string.type_cafe),
    )

    PawPlaceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xA6101A22),
                    border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                    modifier = Modifier.size(44.dp),
                    onClick = onBackClick,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color(0xFFF2F6F8),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NUEVO",
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = BrandPrimary.copy(alpha = 0.85f),
                    )
                    Text(
                        text = stringResource(R.string.add_place),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2F6F8),
                        letterSpacing = (-0.4).sp,
                    )
                }
            }

            // ── Coords pill (gives location context) ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                ) {
                    Text(
                        text = "📍  ${"%.4f".format(lat)},  ${"%.4f".format(lon)}",
                        fontSize = 12.sp,
                        color = Color(0xCCF2F6F8),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Basic info card ────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.basic_info))

                    GlassTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) showError = false
                        },
                        label = stringResource(R.string.name),
                        isError = showError && name.isBlank(),
                        errorText = if (showError && name.isBlank())
                            stringResource(R.string.name_required) else null,
                    )

                    Spacer(Modifier.height(10.dp))

                    GlassTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = stringResource(R.string.description),
                        singleLine = false,
                    )

                    Spacer(Modifier.height(14.dp))

                    SectionLabel(stringResource(R.string.type))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(typeOptions.indices.toList()) { i ->
                            TypeChip(
                                type = typeOptions[i],
                                selected = selectedTypeIndex == i,
                                onClick = { selectedTypeIndex = i },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Photo card ─────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.image_optional))

                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = stringResource(R.string.image_selected),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GhostButton(
                                onClick = { imageLauncher.launch("image/*") },
                                label = stringResource(R.string.change_image),
                                modifier = Modifier.weight(1f),
                            )
                            GhostButton(
                                onClick = { imageUri = null },
                                label = stringResource(R.string.delete),
                                modifier = Modifier.weight(1f),
                                accent = Color(0xFFFF9DB1),
                            )
                        }
                    } else {
                        PhotoDropzone(onClick = { imageLauncher.launch("image/*") })
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Rating card ────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.rating))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBar(rating, { rating = it }, true)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.rating_format, rating.toInt()),
                            fontSize = 13.sp,
                            color = BrandPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Visibility card with glass switch ──────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.publish_to_community),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF2F6F8),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.publish_community_desc),
                                fontSize = 12.sp,
                                color = Color(0x99F2F6F8),
                            )
                        }
                        GlassSwitch(checked = isPublic, onChange = { isPublic = it })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Save CTA ───────────────────────────────────────────
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addDog(
                                Dog(
                                    name = name,
                                    description = description,
                                    latitude = lat,
                                    longitude = lon,
                                    type = typeKeys[selectedTypeIndex],
                                    rating = rating,
                                    imageUrl = imageUri?.toString() ?: "",
                                    isPublic = isPublic,
                                    isPlace = true,
                                )
                            )
                            onSaveClick()
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(BrandPrimary, Color(0xFF5BC2EE))
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF03161F),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.save_location),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ─── Section label ─── */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        color = BrandPrimary.copy(alpha = 0.85f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

/* ─── Type chip with marker glyph ─── */
@Composable
private fun TypeChip(type: String, selected: Boolean, onClick: () -> Unit) {
    val accent = accentForType(type)
    val bg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.18f) else Color(0x14FFFFFF),
        label = "type_chip"
    )
    val border = if (selected) accent.copy(alpha = 0.6f) else Color(0x14FFFFFF)

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarkerGlyph(type = type, size = 26)
            Text(
                text = type,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) Color(0xFFF2F6F8) else Color(0xCCF2F6F8),
            )
        }
    }
}

/* ─── Glass switch (matches map's glass FABs) ─── */
@Composable
private fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val w = 52.dp
    val h = 30.dp
    val track by animateColorAsState(
        targetValue = if (checked) BrandPrimary.copy(alpha = 0.85f) else Color(0x14FFFFFF),
        label = "switch_track",
    )
    val border = if (checked) Color(0x66FFFFFF) else Color(0x14FFFFFF)

    Surface(
        modifier = Modifier
            .size(width = w, height = h)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onChange(!checked) },
        shape = RoundedCornerShape(999.dp),
        color = track,
        border = BorderStroke(1.dp, border),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(24.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color(0xFFF2F6F8)),
            )
        }
    }
}

/* ─── Photo dropzone ─── */
@Composable
private fun PhotoDropzone(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0AFFFFFF))
            .border(
                width = 1.dp,
                color = BrandPrimary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = stringResource(R.string.add_image),
                    tint = BrandPrimary,
                )
            }
            Text(
                text = stringResource(R.string.tap_to_add_photo),
                fontSize = 13.sp,
                color = Color(0xCCF2F6F8),
            )
        }
    }
}

/* ─── Ghost button ─── */
@Composable
private fun GhostButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFF2F6F8),
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0x0AFFFFFF),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
            )
        }
    }
}
