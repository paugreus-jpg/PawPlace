package com.example.dogmap.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dogmap.R
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.models.NotificationType

@Composable
fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit
) {
    val unreadBg = if (!notification.read) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(unreadBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconFor(notification.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!notification.read) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = relativeTime(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun iconFor(type: NotificationType) = when (type) {
    NotificationType.WALK_LIKE -> Icons.Default.FavoriteBorder
    NotificationType.WALK_TRENDING -> Icons.AutoMirrored.Filled.TrendingFlat
    NotificationType.NEW_FOLLOWER -> Icons.Default.Person
    NotificationType.LOCATION_COMMENT -> Icons.Default.NotificationsActive
    NotificationType.WALK_REMINDER -> Icons.Default.NotificationsActive
    NotificationType.SYSTEM -> Icons.Default.NotificationsActive
}

private fun relativeTime(createdAt: Long): String {
    val diff = System.currentTimeMillis() - createdAt
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "Ahora mismo"
        hours < 1 -> "Hace $minutes min"
        days < 1 -> "Hace $hours h"
        else -> "Hace $days d"
    }
}
