package com.example.dogmap.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val payloadJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val ownerUid: String
)
