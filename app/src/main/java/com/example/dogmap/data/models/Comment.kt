package com.example.dogmap.data.models

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
