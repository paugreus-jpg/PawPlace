package com.example.dogmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.data.models.Dog
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.DogCard
import com.example.dogmap.ui.components.UserAvatar
import com.example.dogmap.viewmodel.DogViewModel
import com.example.dogmap.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    uid: String,
    onBackClick: () -> Unit,
    onDogClick: (Dog) -> Unit
) {
    val factory = LocalViewModelFactory.current
    val userVm: UserViewModel = viewModel(factory = factory, key = "public_$uid")
    val dogVm: DogViewModel = viewModel(factory = factory)

    val state by userVm.state.collectAsState()
    val currentUid by dogVm.currentUid.collectAsState()
    val favoriteIds by dogVm.favoriteIds.collectAsState()

    LaunchedEffect(uid) {
        userVm.loadProfile(uid, includeFavorites = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.user?.displayName?.ifBlank { stringResource(R.string.profile_screen) }
                        ?: stringResource(R.string.profile_screen))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UserAvatar(
                            displayName = state.user?.displayName ?: "?",
                            photoUrl = state.user?.photoUrl ?: "",
                            size = 96.dp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.user?.displayName ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!state.user?.city.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(text = state.user?.city ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (!state.user?.bio.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.user?.bio ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!state.user?.petName.isNullOrBlank()) {
                item {
                    PetCard(
                        petName = state.user?.petName ?: "",
                        petBreed = state.user?.petBreed ?: "",
                        petAge = state.user?.petAge ?: 0,
                        petPhotoUrl = state.user?.petPhotoUrl ?: "",
                        onAddPet = {}
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.stats_places, state.dogs.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (state.dogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_locations),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.dogs, key = { it.id.toString() + it.remoteId }) { dog ->
                    val isLiked by dogVm.likeStateFor(dog.remoteId).collectAsState()
                    DogCard(
                        dog = dog,
                        currentUid = currentUid,
                        isLiked = isLiked,
                        isFavorite = dog.remoteId in favoriteIds,
                        onCardClick = onDogClick,
                        onDeleteClick = { dogVm.deleteDog(dog) },
                        onLikeClick = { dogVm.toggleLike(dog) },
                        onFavoriteClick = { dogVm.toggleFavorite(dog) },
                        onAuthorClick = {}
                    )
                }
            }
        }
    }
}
