package com.example.dogmap.data.models

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val city: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val petName: String = "",
    val petBreed: String = "",
    val petAge: Int = 0,
    val petPhotoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val profileSetupCompleted: Boolean = false
)
