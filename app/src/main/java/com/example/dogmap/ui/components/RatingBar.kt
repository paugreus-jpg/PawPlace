package com.example.dogmap.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RatingBar(
    rating: Float,
    onRatingChange: (Float) -> Unit = {},
    isEditable: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            IconButton(
                onClick = { if (isEditable) onRatingChange((index + 1).toFloat()) },
                enabled = isEditable
            ) {
                Icon(
                    imageVector = if (index < rating.toInt()) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Estrella ${index + 1}",
                    tint = if (index < rating.toInt()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
