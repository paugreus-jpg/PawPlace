package com.example.dogmap.data.db

import com.example.dogmap.data.models.NotificationType
import com.example.dogmap.data.models.TrackPoint
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DogTypeConvertersTest {

    private lateinit var converters: DogTypeConverters

    @Before
    fun setUp() {
        converters = DogTypeConverters()
    }

    // ── fromList / toList ──────────────────────────────────────────────────────

    @Test
    fun `fromList joins list elements with comma`() {
        assertEquals("a,b,c", converters.fromList(listOf("a", "b", "c")))
    }

    @Test
    fun `fromList with single element returns element without comma`() {
        assertEquals("only", converters.fromList(listOf("only")))
    }

    @Test
    fun `fromList with empty list returns empty string`() {
        assertEquals("", converters.fromList(emptyList()))
    }

    @Test
    fun `toList splits comma-separated string into list`() {
        assertEquals(listOf("a", "b", "c"), converters.toList("a,b,c"))
    }

    @Test
    fun `toList with single token returns single-element list`() {
        assertEquals(listOf("only"), converters.toList("only"))
    }

    @Test
    fun `toList with empty string returns empty list`() {
        assertEquals(emptyList<String>(), converters.toList(""))
    }

    @Test
    fun `toList with blank whitespace string returns empty list`() {
        assertEquals(emptyList<String>(), converters.toList("   "))
    }

    @Test
    fun `fromList and toList are inverse operations`() {
        val original = listOf("dog", "cat", "bird")
        assertEquals(original, converters.toList(converters.fromList(original)))
    }

    // ── fromTrackPoints / toTrackPoints ────────────────────────────────────────

    @Test
    fun `fromTrackPoints serializes list to JSON string`() {
        val points = listOf(TrackPoint(lat = 1.5, lon = 2.5, timestamp = 1000L))
        val json = converters.fromTrackPoints(points)
        assertTrue(json.contains("1.5"))
        assertTrue(json.contains("2.5"))
        assertTrue(json.contains("1000"))
    }

    @Test
    fun `fromTrackPoints with empty list returns JSON empty array`() {
        assertEquals("[]", converters.fromTrackPoints(emptyList()))
    }

    @Test
    fun `fromTrackPoints with multiple points serializes all`() {
        val points = listOf(
            TrackPoint(lat = 40.0, lon = -3.0, timestamp = 100L),
            TrackPoint(lat = 41.0, lon = -4.0, timestamp = 200L)
        )
        val json = converters.fromTrackPoints(points)
        assertTrue(json.contains("40.0"))
        assertTrue(json.contains("41.0"))
    }

    @Test
    fun `toTrackPoints deserializes JSON string to list`() {
        val json = """[{"lat":1.5,"lon":2.5,"timestamp":1000}]"""
        val points = converters.toTrackPoints(json)
        assertEquals(1, points.size)
        assertEquals(1.5, points[0].lat, 0.0001)
        assertEquals(2.5, points[0].lon, 0.0001)
        assertEquals(1000L, points[0].timestamp)
    }

    @Test
    fun `toTrackPoints with blank string returns empty list`() {
        assertEquals(emptyList<TrackPoint>(), converters.toTrackPoints(""))
    }

    @Test
    fun `toTrackPoints with whitespace string returns empty list`() {
        assertEquals(emptyList<TrackPoint>(), converters.toTrackPoints("   "))
    }

    @Test
    fun `fromTrackPoints and toTrackPoints are inverse operations`() {
        val original = listOf(
            TrackPoint(lat = 10.0, lon = 20.0, timestamp = 500L),
            TrackPoint(lat = 11.0, lon = 21.0, timestamp = 600L)
        )
        val roundTripped = converters.toTrackPoints(converters.fromTrackPoints(original))
        assertEquals(original.size, roundTripped.size)
        assertEquals(original[0].lat, roundTripped[0].lat, 0.0001)
        assertEquals(original[0].lon, roundTripped[0].lon, 0.0001)
        assertEquals(original[0].timestamp, roundTripped[0].timestamp)
        assertEquals(original[1].lat, roundTripped[1].lat, 0.0001)
    }

    // ── fromNotificationType / toNotificationType ──────────────────────────────

    @Test
    fun `fromNotificationType returns enum name for WALK_LIKE`() {
        assertEquals("WALK_LIKE", converters.fromNotificationType(NotificationType.WALK_LIKE))
    }

    @Test
    fun `fromNotificationType returns enum name for WALK_TRENDING`() {
        assertEquals("WALK_TRENDING", converters.fromNotificationType(NotificationType.WALK_TRENDING))
    }

    @Test
    fun `fromNotificationType returns enum name for LOCATION_COMMENT`() {
        assertEquals("LOCATION_COMMENT", converters.fromNotificationType(NotificationType.LOCATION_COMMENT))
    }

    @Test
    fun `fromNotificationType returns enum name for NEW_FOLLOWER`() {
        assertEquals("NEW_FOLLOWER", converters.fromNotificationType(NotificationType.NEW_FOLLOWER))
    }

    @Test
    fun `fromNotificationType returns enum name for WALK_REMINDER`() {
        assertEquals("WALK_REMINDER", converters.fromNotificationType(NotificationType.WALK_REMINDER))
    }

    @Test
    fun `fromNotificationType returns enum name for SYSTEM`() {
        assertEquals("SYSTEM", converters.fromNotificationType(NotificationType.SYSTEM))
    }

    @Test
    fun `toNotificationType converts valid string to WALK_LIKE`() {
        assertEquals(NotificationType.WALK_LIKE, converters.toNotificationType("WALK_LIKE"))
    }

    @Test
    fun `toNotificationType converts valid string to WALK_TRENDING`() {
        assertEquals(NotificationType.WALK_TRENDING, converters.toNotificationType("WALK_TRENDING"))
    }

    @Test
    fun `toNotificationType converts valid string to LOCATION_COMMENT`() {
        assertEquals(NotificationType.LOCATION_COMMENT, converters.toNotificationType("LOCATION_COMMENT"))
    }

    @Test
    fun `toNotificationType converts valid string to NEW_FOLLOWER`() {
        assertEquals(NotificationType.NEW_FOLLOWER, converters.toNotificationType("NEW_FOLLOWER"))
    }

    @Test
    fun `toNotificationType converts valid string to WALK_REMINDER`() {
        assertEquals(NotificationType.WALK_REMINDER, converters.toNotificationType("WALK_REMINDER"))
    }

    @Test
    fun `toNotificationType converts valid string to SYSTEM`() {
        assertEquals(NotificationType.SYSTEM, converters.toNotificationType("SYSTEM"))
    }

    @Test
    fun `toNotificationType returns SYSTEM for unknown value`() {
        assertEquals(NotificationType.SYSTEM, converters.toNotificationType("UNKNOWN_VALUE"))
    }

    @Test
    fun `toNotificationType returns SYSTEM for empty string`() {
        assertEquals(NotificationType.SYSTEM, converters.toNotificationType(""))
    }

    @Test
    fun `toNotificationType returns SYSTEM for lowercase string`() {
        assertEquals(NotificationType.SYSTEM, converters.toNotificationType("walk_like"))
    }

    @Test
    fun `fromNotificationType and toNotificationType are inverse for all types`() {
        for (type in NotificationType.values()) {
            assertEquals(type, converters.toNotificationType(converters.fromNotificationType(type)))
        }
    }
}
