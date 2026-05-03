package com.example.dogmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PublicOff
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dogmap.R
import com.example.dogmap.data.models.Dog

@Composable
fun DogCard(
    dog: Dog,
    currentUid: String?,
    isLiked: Boolean,
    isFavorite: Boolean,
    onCardClick: (Dog) -> Unit,
    onDeleteClick: (Dog) -> Unit,
    onLikeClick: (Dog) -> Unit,
    onFavoriteClick: (Dog) -> Unit,
    onAuthorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    showPublishAction: Boolean = false,
    onPublishClick: (Dog) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar lugar") },
            text = { Text("¿Seguro que quieres eliminar \"${dog.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(dog)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val isOwner = currentUid != null && dog.authorId == currentUid

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(dog) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        // Fila de autor
        if (dog.authorId.isNotBlank() && dog.authorName.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAuthorClick(dog.authorId) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    displayName = dog.authorName,
                    photoUrl = dog.authorPhotoUrl,
                    size = 28.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dog.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Box {
            if (dog.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = dog.imageUrl,
                    contentDescription = dog.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                            startY = 60f,
                            endY = 400f
                        )
                    )
            )

            Text(
                text = dog.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 10.dp, end = 48.dp),
                maxLines = 1
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f)
            ) {
                Text(
                    text = dog.type,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            RatingBar(rating = dog.rating)

            if (dog.description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dog.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Acciones sociales
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onLikeClick(dog) }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (isLiked) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dog.likesCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onFavoriteClick(dog) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(R.string.favorites),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                if (showPublishAction && isOwner) {
                    IconButton(onClick = { onPublishClick(dog) }) {
                        Icon(
                            imageVector = if (dog.isPublic) Icons.Default.Public else Icons.Outlined.PublicOff,
                            contentDescription = if (dog.isPublic) "Despublicar" else "Publicar en comunidad",
                            tint = if (dog.isPublic) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isOwner) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
