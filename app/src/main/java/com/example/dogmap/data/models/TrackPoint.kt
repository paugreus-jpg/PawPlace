package com.example.dogmap.data.models

import kotlinx.serialization.Serializable

@Serializable
data class TrackPoint(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long = 0L
)
