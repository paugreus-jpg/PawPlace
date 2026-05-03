package com.example.dogmap.data.db

import androidx.room.TypeConverter
import com.example.dogmap.data.models.NotificationType
import com.example.dogmap.data.models.TrackPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DogTypeConverters {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(",")

    @TypeConverter
    fun fromTrackPoints(points: List<TrackPoint>): String =
        Json.encodeToString(points)

    @TypeConverter
    fun toTrackPoints(data: String): List<TrackPoint> =
        if (data.isBlank()) emptyList() else Json.decodeFromString(data)

    @TypeConverter
    fun fromNotificationType(type: NotificationType): String = type.name

    @TypeConverter
    fun toNotificationType(value: String): NotificationType =
        runCatching { NotificationType.valueOf(value) }.getOrDefault(NotificationType.SYSTEM)
}
