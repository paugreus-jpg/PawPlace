package com.example.dogmap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import android.content.Context
import com.dogmap.R
import com.example.dogmap.data.models.Comment
import com.example.dogmap.data.models.Dog
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.RatingBar
import com.example.dogmap.ui.components.UserAvatar
import com.example.dogmap.viewmodel.DogViewModel

fun relativeTime(createdAt: Long, context: Context): String {
    val diff = System.currentTimeMillis() - createdAt
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> context.getString(R.string.just_now)
        minutes < 60 -> context.getString(R.string.minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.hours_ago, hours)
        else -> context.getString(R.string.days_ago, days)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    dogRemoteId: String,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onMapClick: (Double, Double) -> Unit
) {
    val factory = LocalViewModelFactory.current
    val viewModel: DogViewModel = viewModel(factory = factory)

    val context = LocalContext.current
    val dog by viewModel.observeDog(dogRemoteId).collectAsState()
    val comments by viewModel.observeComments(dogRemoteId).collectAsState()
    val currentUid by viewModel.currentUid.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isLiked by viewModel.likeStateFor(dogRemoteId).collectAsState()

    var commentText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isFavorite = dogRemoteId in favoriteIds
    val isOwner = currentUid != null && dog?.authorId == currentUid

    if (showDeleteDialog && dog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_place_title)) },
            text = { Text(stringResource(R.string.delete_place_confirm_msg, dog!!.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDog(dog!!)
                        showDeleteDialog = false
                        onBackClick()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dog?.name ?: stringResource(R.string.place_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (dog == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val d = dog!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (d.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = d.imageUrl,
                            contentDescription = d.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
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
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x99000000)),
                                    startY = 120f,
                                    endY = 600f
                                )
                            )
                    )

                    // Type badge
                    if (d.type.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f)
                        ) {
                            Text(
                                text = d.type,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Info section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Name + rating
                    Text(
                        text = d.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    RatingBar(rating = d.rating)

                    // Author row
                    if (d.authorId.isNotBlank() && d.authorName.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAuthorClick(d.authorId) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                displayName = d.authorName,
                                photoUrl = d.authorPhotoUrl,
                                size = 36.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = d.authorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = relativeTime(d.createdAt, context),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Description
                    if (d.description.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = d.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }
            }

            // Actions row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleLike(d) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Me gusta",
                            tint = if (isLiked) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = d.likesCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.toggleFavorite(d) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { onMapClick(d.latitude, d.longitude) }) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = stringResource(R.string.view_on_map),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
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

            // Divider
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            // Comments header
            item {
                Text(
                    text = stringResource(R.string.comments_title, comments.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Comment input
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.add_comment)) },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.addComment(dogRemoteId, commentText)
                            commentText = ""
                        },
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = stringResource(R.string.send_comment),
                            tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Comments list
            if (comments.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_comments),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(comments, key = { it.id }) { comment ->
                    CommentItem(
                        comment = comment,
                        isOwn = comment.authorId == currentUid,
                        onDelete = { viewModel.deleteComment(dogRemoteId, comment.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    isOwn: Boolean,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            displayName = comment.authorName,
            photoUrl = comment.authorPhotoUrl,
            size = 32.dp
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(comment.createdAt, context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (isOwn) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_comment),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
