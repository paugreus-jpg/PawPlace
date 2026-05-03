// File: app/src/main/java/com/example/dogmap/ui/screens/ListScreen.kt
// PawPlace list — search pill mirrors the map's, type chips reuse map glyphs,
// place cards sit on glass surfaces with a typed glyph tile in the matching
// marker accent. ViewModel + filter logic preserved exactly.
package com.example.dogmap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.data.models.Dog
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.GlassCard
import com.example.dogmap.ui.components.GlassPill
import com.example.dogmap.ui.components.MarkerGlyph
import com.example.dogmap.ui.components.PawPlaceBackground
import com.example.dogmap.ui.components.accentForType
import com.example.dogmap.ui.theme.BrandPrimary
import com.example.dogmap.viewmodel.DogViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    onDogClick: (Dog) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val factory = LocalViewModelFactory.current
    val viewModel = viewModel<DogViewModel>(factory = factory)
    val allDogs by viewModel.allDogs.collectAsState(initial = emptyList())
    val publicDogs by viewModel.publicDogs.collectAsState()
    val favoriteDogs by viewModel.favoriteDogs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) }

    val typeFilters = listOf(
        stringResource(R.string.type_park),
        stringResource(R.string.type_beach),
        stringResource(R.string.type_restaurant),
        stringResource(R.string.type_vet),
        stringResource(R.string.type_cafe),
    )

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    fun filter(dogs: List<Dog>): List<Dog> = dogs.filter { d ->
        val matchesSearch = searchQuery.isBlank() ||
            d.name.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedType == null || d.type == selectedType
        matchesSearch && matchesType
    }

    PawPlaceBackground {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LUGARES",
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = BrandPrimary.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Tus lugares",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2F6F8),
                        letterSpacing = (-0.4).sp,
                    )
                }
            }

            // ── Search pill (mirrors map's) ─────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color(0x14FFFFFF),
                border = BorderStroke(1.dp, Color(0x14FFFFFF)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.Search, null, tint = BrandPrimary)
                    BasicSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = stringResource(R.string.search_placeholder),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Type chips ──────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    GlassPill(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = stringResource(R.string.filter_all),
                    )
                }
                items(typeFilters) { type ->
                    GlassPill(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = if (selectedType == type) null else type
                        },
                        accent = accentForType(type),
                        label = type,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Tabs (glass underline) ──────────────────────────────
            GlassTabRow(
                selectedIndex = pagerState.currentPage,
                onSelect = { i -> scope.launch { pagerState.animateScrollToPage(i) } },
                tabs = listOf(
                    stringResource(R.string.my_places),
                    stringResource(R.string.walks_tab_community),
                    stringResource(R.string.favorites),
                ),
            )

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val list = when (page) {
                    0 -> filter(allDogs)
                    1 -> filter(publicDogs)
                    else -> filter(favoriteDogs)
                }
                val emptyMessage = when (page) {
                    0 -> stringResource(R.string.empty_my_places)
                    1 -> stringResource(R.string.empty_community_places)
                    else -> stringResource(R.string.no_favorites)
                }

                if (list.isEmpty()) {
                    EmptyState(message = emptyMessage)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    ) {
                        items(list, key = { it.id.toString() + it.remoteId }) { dog ->
                            PlaceListCard(
                                dog = dog,
                                onClick = { onDogClick(dog) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ─── Place card (glass + typed glyph tile) ─── */
@Composable
private fun PlaceListCard(dog: Dog, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        cornerRadius = 20,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarkerGlyph(type = dog.type, size = 44)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dog.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F6F8),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dog.type,
                    fontSize = 12.sp,
                    color = accentForType(dog.type),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                )
                if (dog.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dog.description,
                        fontSize = 13.sp,
                        color = Color(0xB3F2F6F8),
                        maxLines = 2,
                    )
                }
            }
            if (dog.rating > 0f) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0x1FFFFFFF),
                    border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                ) {
                    Text(
                        text = "★ ${dog.rating.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTabRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    tabs: List<String>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val on = selectedIndex == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) Color(0xFFF2F6F8) else Color(0x99F2F6F8),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth(if (on) 0.6f else 0f)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandPrimary, Color(0xFF5BC2EE))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            BrandPrimary.copy(alpha = 0.25f),
                            BrandPrimary.copy(alpha = 0.05f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = Color(0xB3F2F6F8),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BasicSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color(0xFFF2F6F8),
            fontSize = 15.sp,
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(BrandPrimary),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0x80F2F6F8),
                        fontSize = 15.sp,
                    )
                }
                inner()
            }
        },
    )
}
