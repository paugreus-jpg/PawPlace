package com.example.dogmap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.data.models.Walk
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.viewmodel.DogViewModel
import com.example.dogmap.viewmodel.WalkViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkListScreen(
    onBackClick: () -> Unit,
    onWalkClick: (String) -> Unit,
    onRecordClick: () -> Unit
) {
    val factory = LocalViewModelFactory.current
    val vm: WalkViewModel = viewModel(factory = factory)
    val dogVm: DogViewModel = viewModel(factory = factory)

    val publicWalks by vm.publicWalks.collectAsState()
    val myWalks by vm.myWalks.collectAsState()
    val favoriteWalks by vm.favoriteWalks.collectAsState()
    val favoriteIds by vm.favoriteWalkIds.collectAsState()
    val currentUid by vm.currentUid.collectAsState()
    val userDogs by dogVm.myDogs.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.walks_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val hasDogs = userDogs.isNotEmpty()
            ExtendedFloatingActionButton(
                onClick = {
                    if (hasDogs) {
                        onRecordClick()
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Añade una mascota antes de grabar un paseo"
                            )
                        }
                    }
                },
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = if (hasDogs) LocalContentColor.current
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.walk_record_title),
                        color = if (hasDogs) LocalContentColor.current
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                },
                containerColor = if (hasDogs) FloatingActionButtonDefaults.containerColor
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.walks_tab_community)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.walks_tab_mine)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.walks_tab_favorites)) }
                )
            }

            val list = when (selectedTab) {
                0 -> publicWalks
                1 -> myWalks
                else -> favoriteWalks
            }
            val emptyText = when (selectedTab) {
                0 -> stringResource(R.string.walks_empty_community)
                1 -> stringResource(R.string.walks_empty_mine)
                else -> stringResource(R.string.walks_empty_favorites)
            }

            if (list.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        emptyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.remoteId.ifBlank { it.id.toString() } }) { walk ->
                        val isLiked by vm.likeStateFor(walk.remoteId).collectAsState()
                        val isFavorite = walk.remoteId in favoriteIds
                        WalkCard(
                            walk = walk,
                            isLiked = isLiked,
                            isFavorite = isFavorite,
                            canInteract = currentUid != null && walk.remoteId.isNotBlank(),
                            onClick = { if (walk.remoteId.isNotBlank()) onWalkClick(walk.remoteId) },
                            onLikeClick = { vm.toggleLike(walk) },
                            onFavoriteClick = { vm.toggleFavorite(walk) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalkCard(
    walk: Walk,
    isLiked: Boolean,
    isFavorite: Boolean,
    canInteract: Boolean,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        walk.name.ifBlank { "Paseo sin nombre" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (walk.dogName.isNotBlank()) {
                        Text(
                            walk.dogName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (walk.isPublic) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    String.format("%.2f km", walk.distanceMeters / 1000.0),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(16.dp))
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                val mins = walk.durationSeconds / 60
                val secs = walk.durationSeconds % 60
                Text(
                    String.format("%d:%02d", mins, secs),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (walk.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    walk.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (walk.isPublic) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLikeClick,
                        enabled = canInteract
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        walk.likesCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = onFavoriteClick,
                        enabled = canInteract
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (walk.authorName.isNotBlank()) {
                        Text(
                            stringResource(R.string.by_user, walk.authorName),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
