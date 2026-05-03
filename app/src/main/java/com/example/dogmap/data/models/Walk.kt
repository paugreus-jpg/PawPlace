package com.example.dogmap.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dogmap.data.db.DogTypeConverters

@Entity(tableName = "walks")
@TypeConverters(DogTypeConverters::class)
data class Walk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val name: String = "",
    val description: String = "",
    val dogId: String = "",
    val dogName: String = "",
    val isPublic: Boolean = false,
    val likesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0L,
    val points: List<TrackPoint> = emptyList()
)
