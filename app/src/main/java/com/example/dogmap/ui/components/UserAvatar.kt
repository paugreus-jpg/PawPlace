package com.example.dogmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.abs

private val AVATAR_PALETTE = listOf(
    Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFF7986CB),
    Color(0xFF4FC3F7), Color(0xFF4DB6AC), Color(0xFF81C784),
    Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFA1887F)
)

@Composable
fun UserAvatar(
    displayName: String,
    photoUrl: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: "?"
    val color = AVATAR_PALETTE[abs(displayName.hashCode()) % AVATAR_PALETTE.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = displayName,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
