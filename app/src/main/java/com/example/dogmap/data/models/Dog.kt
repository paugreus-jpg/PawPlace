package com.example.dogmap.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dogmap.data.db.DogTypeConverters
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "dogs")
@TypeConverters(DogTypeConverters::class)
data class Dog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val type: String = "",
    val rating: Float = 0f,
    val imageUrl: String = "",
    @get:PropertyName("isPublic")
    val isPublic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val remoteId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val likesCount: Int = 0,
    @get:PropertyName("isPlace")
    val isPlace: Boolean = false
)
