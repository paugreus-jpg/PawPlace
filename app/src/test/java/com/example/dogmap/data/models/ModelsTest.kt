package com.example.dogmap.data.models

import org.junit.Assert.*
import org.junit.Test

// ── TrackPoint ────────────────────────────────────────────────────────────────

class TrackPointTest {

    @Test fun `default values are zero`() {
        val tp = TrackPoint()
        assertEquals(0.0, tp.lat, 0.0)
        assertEquals(0.0, tp.lon, 0.0)
        assertEquals(0L, tp.timestamp)
    }

    @Test fun `constructor stores provided values`() {
        val tp = TrackPoint(lat = 40.416775, lon = -3.703790, timestamp = 1_700_000_000L)
        assertEquals(40.416775, tp.lat, 0.000001)
        assertEquals(-3.703790, tp.lon, 0.000001)
        assertEquals(1_700_000_000L, tp.timestamp)
    }

    @Test fun `copy changes only specified fields`() {
        val original = TrackPoint(lat = 1.0, lon = 2.0, timestamp = 100L)
        val copied = original.copy(lat = 3.0)
        assertEquals(3.0, copied.lat, 0.0)
        assertEquals(2.0, copied.lon, 0.0)
        assertEquals(100L, copied.timestamp)
    }

    @Test fun `equality based on values`() {
        val a = TrackPoint(lat = 1.0, lon = 2.0, timestamp = 100L)
        val b = TrackPoint(lat = 1.0, lon = 2.0, timestamp = 100L)
        assertEquals(a, b)
    }

    @Test fun `inequality when values differ`() {
        val a = TrackPoint(lat = 1.0, lon = 2.0, timestamp = 100L)
        val b = TrackPoint(lat = 9.9, lon = 2.0, timestamp = 100L)
        assertNotEquals(a, b)
    }

    @Test fun `toString contains field values`() {
        val tp = TrackPoint(lat = 1.0, lon = 2.0, timestamp = 100L)
        val str = tp.toString()
        assertTrue(str.contains("1.0"))
        assertTrue(str.contains("2.0"))
        assertTrue(str.contains("100"))
    }

    @Test fun `hashCode consistent with equality`() {
        val a = TrackPoint(lat = 5.0, lon = 6.0, timestamp = 200L)
        val b = TrackPoint(lat = 5.0, lon = 6.0, timestamp = 200L)
        assertEquals(a.hashCode(), b.hashCode())
    }
}

// ── Dog ───────────────────────────────────────────────────────────────────────

class DogTest {

    @Test fun `default values`() {
        val dog = Dog()
        assertEquals(0L, dog.id)
        assertEquals("", dog.name)
        assertEquals("", dog.description)
        assertEquals(0.0, dog.latitude, 0.0)
        assertEquals(0.0, dog.longitude, 0.0)
        assertEquals("", dog.type)
        assertEquals(0f, dog.rating)
        assertEquals("", dog.imageUrl)
        assertFalse(dog.isPublic)
        assertEquals("", dog.remoteId)
        assertEquals("", dog.authorId)
        assertEquals("", dog.authorName)
        assertEquals("", dog.authorPhotoUrl)
        assertEquals(0, dog.likesCount)
        assertFalse(dog.isPlace)
    }

    @Test fun `constructor stores provided values`() {
        val dog = Dog(
            id = 1L,
            name = "Buddy",
            description = "Friendly dog",
            latitude = 40.0,
            longitude = -3.0,
            type = "Labrador",
            rating = 4.5f,
            imageUrl = "http://img.com/dog.jpg",
            isPublic = true,
            remoteId = "remote123",
            authorId = "author456",
            authorName = "Alice",
            authorPhotoUrl = "http://img.com/alice.jpg",
            likesCount = 10,
            isPlace = true
        )
        assertEquals(1L, dog.id)
        assertEquals("Buddy", dog.name)
        assertEquals("Friendly dog", dog.description)
        assertEquals(40.0, dog.latitude, 0.000001)
        assertEquals(-3.0, dog.longitude, 0.000001)
        assertEquals("Labrador", dog.type)
        assertEquals(4.5f, dog.rating)
        assertEquals("http://img.com/dog.jpg", dog.imageUrl)
        assertTrue(dog.isPublic)
        assertEquals("remote123", dog.remoteId)
        assertEquals("author456", dog.authorId)
        assertEquals("Alice", dog.authorName)
        assertEquals("http://img.com/alice.jpg", dog.authorPhotoUrl)
        assertEquals(10, dog.likesCount)
        assertTrue(dog.isPlace)
    }

    @Test fun `copy changes only specified fields`() {
        val original = Dog(name = "Rex", likesCount = 5)
        val copy = original.copy(name = "Max", likesCount = 10)
        assertEquals("Max", copy.name)
        assertEquals(10, copy.likesCount)
        assertEquals(original.authorId, copy.authorId)
    }

    @Test fun `equality based on all fields`() {
        val t = System.currentTimeMillis()
        val a = Dog(id = 1L, name = "A", createdAt = t)
        val b = Dog(id = 1L, name = "A", createdAt = t)
        assertEquals(a, b)
    }

    @Test fun `inequality when id differs`() {
        val t = System.currentTimeMillis()
        val a = Dog(id = 1L, createdAt = t)
        val b = Dog(id = 2L, createdAt = t)
        assertNotEquals(a, b)
    }
}

// ── Walk ──────────────────────────────────────────────────────────────────────

class WalkTest {

    @Test fun `default values`() {
        val walk = Walk()
        assertEquals(0L, walk.id)
        assertEquals("", walk.remoteId)
        assertEquals("", walk.authorId)
        assertEquals("", walk.authorName)
        assertEquals("", walk.authorPhotoUrl)
        assertEquals("", walk.name)
        assertEquals("", walk.description)
        assertEquals("", walk.dogId)
        assertEquals("", walk.dogName)
        assertFalse(walk.isPublic)
        assertEquals(0, walk.likesCount)
        assertEquals(0.0, walk.distanceMeters, 0.0)
        assertEquals(0L, walk.durationSeconds)
        assertTrue(walk.points.isEmpty())
    }

    @Test fun `constructor stores provided values`() {
        val pts = listOf(TrackPoint(1.0, 2.0, 100L))
        val walk = Walk(
            id = 5L,
            remoteId = "r1",
            authorId = "a1",
            authorName = "Bob",
            authorPhotoUrl = "http://img/bob.jpg",
            name = "Morning walk",
            description = "Nice",
            dogId = "dog123",
            dogName = "Fido",
            isPublic = true,
            likesCount = 3,
            distanceMeters = 1500.0,
            durationSeconds = 900L,
            points = pts
        )
        assertEquals(5L, walk.id)
        assertEquals("r1", walk.remoteId)
        assertEquals("a1", walk.authorId)
        assertEquals("Bob", walk.authorName)
        assertEquals("Morning walk", walk.name)
        assertTrue(walk.isPublic)
        assertEquals(3, walk.likesCount)
        assertEquals(1500.0, walk.distanceMeters, 0.001)
        assertEquals(900L, walk.durationSeconds)
        assertEquals(1, walk.points.size)
    }

    @Test fun `copy changes only specified field`() {
        val original = Walk(name = "Evening", distanceMeters = 500.0)
        val copy = original.copy(distanceMeters = 1000.0)
        assertEquals("Evening", copy.name)
        assertEquals(1000.0, copy.distanceMeters, 0.001)
    }

    @Test fun `equality based on values`() {
        val t = System.currentTimeMillis()
        val a = Walk(id = 1L, name = "A", createdAt = t)
        val b = Walk(id = 1L, name = "A", createdAt = t)
        assertEquals(a, b)
    }
}

// ── User ──────────────────────────────────────────────────────────────────────

class UserTest {

    @Test fun `default values`() {
        val user = User()
        assertEquals("", user.uid)
        assertEquals("", user.email)
        assertEquals("", user.displayName)
        assertEquals("", user.city)
        assertEquals("", user.photoUrl)
        assertEquals("", user.bio)
        assertEquals("", user.petName)
        assertEquals("", user.petBreed)
        assertEquals(0, user.petAge)
        assertEquals("", user.petPhotoUrl)
        assertFalse(user.profileSetupCompleted)
    }

    @Test fun `constructor stores provided values`() {
        val user = User(
            uid = "uid1",
            email = "a@b.com",
            displayName = "Alice",
            city = "Madrid",
            photoUrl = "http://img/a.jpg",
            bio = "Dog lover",
            petName = "Rex",
            petBreed = "Labrador",
            petAge = 3,
            petPhotoUrl = "http://img/rex.jpg",
            profileSetupCompleted = true
        )
        assertEquals("uid1", user.uid)
        assertEquals("a@b.com", user.email)
        assertEquals("Alice", user.displayName)
        assertEquals("Madrid", user.city)
        assertEquals("Dog lover", user.bio)
        assertEquals("Rex", user.petName)
        assertEquals("Labrador", user.petBreed)
        assertEquals(3, user.petAge)
        assertTrue(user.profileSetupCompleted)
    }

    @Test fun `copy changes only specified field`() {
        val original = User(uid = "u1", displayName = "Alice")
        val copy = original.copy(displayName = "Bob")
        assertEquals("u1", copy.uid)
        assertEquals("Bob", copy.displayName)
    }

    @Test fun `equality based on values`() {
        val t = System.currentTimeMillis()
        val a = User(uid = "u1", email = "a@b.com", createdAt = t)
        val b = User(uid = "u1", email = "a@b.com", createdAt = t)
        assertEquals(a, b)
    }
}

// ── Comment ───────────────────────────────────────────────────────────────────

class CommentTest {

    @Test fun `default values`() {
        val comment = Comment()
        assertEquals("", comment.id)
        assertEquals("", comment.authorId)
        assertEquals("", comment.authorName)
        assertEquals("", comment.authorPhotoUrl)
        assertEquals("", comment.text)
    }

    @Test fun `constructor stores provided values`() {
        val comment = Comment(
            id = "c1",
            authorId = "a1",
            authorName = "Alice",
            authorPhotoUrl = "http://img/a.jpg",
            text = "Great dog!",
            createdAt = 1000L
        )
        assertEquals("c1", comment.id)
        assertEquals("a1", comment.authorId)
        assertEquals("Alice", comment.authorName)
        assertEquals("http://img/a.jpg", comment.authorPhotoUrl)
        assertEquals("Great dog!", comment.text)
        assertEquals(1000L, comment.createdAt)
    }

    @Test fun `copy changes only specified field`() {
        val original = Comment(id = "c1", text = "Hello")
        val copy = original.copy(text = "Updated")
        assertEquals("c1", copy.id)
        assertEquals("Updated", copy.text)
    }

    @Test fun `equality based on values`() {
        val a = Comment(id = "c1", text = "Hi", createdAt = 999L)
        val b = Comment(id = "c1", text = "Hi", createdAt = 999L)
        assertEquals(a, b)
    }

    @Test fun `inequality when text differs`() {
        val a = Comment(id = "c1", text = "Hi", createdAt = 999L)
        val b = Comment(id = "c1", text = "Bye", createdAt = 999L)
        assertNotEquals(a, b)
    }
}

// ── AppNotification ───────────────────────────────────────────────────────────

class AppNotificationTest {

    @Test fun `constructor stores provided values`() {
        val notif = AppNotification(
            id = "n1",
            type = NotificationType.WALK_LIKE,
            title = "New like",
            body = "Someone liked your walk",
            payloadJson = "{}",
            createdAt = 2000L,
            read = false,
            ownerUid = "uid1"
        )
        assertEquals("n1", notif.id)
        assertEquals(NotificationType.WALK_LIKE, notif.type)
        assertEquals("New like", notif.title)
        assertEquals("Someone liked your walk", notif.body)
        assertEquals("{}", notif.payloadJson)
        assertEquals(2000L, notif.createdAt)
        assertFalse(notif.read)
        assertEquals("uid1", notif.ownerUid)
    }

    @Test fun `default payloadJson is empty string`() {
        val notif = AppNotification(
            id = "n1",
            type = NotificationType.SYSTEM,
            title = "T",
            body = "B",
            ownerUid = "u1"
        )
        assertEquals("", notif.payloadJson)
    }

    @Test fun `default read is false`() {
        val notif = AppNotification(
            id = "n1", type = NotificationType.SYSTEM, title = "T", body = "B", ownerUid = "u1"
        )
        assertFalse(notif.read)
    }

    @Test fun `copy changes only specified field`() {
        val original = AppNotification(
            id = "n1", type = NotificationType.NEW_FOLLOWER, title = "T", body = "B",
            read = false, ownerUid = "uid1"
        )
        val copy = original.copy(read = true)
        assertEquals("n1", copy.id)
        assertTrue(copy.read)
        assertEquals(NotificationType.NEW_FOLLOWER, copy.type)
    }

    @Test fun `equality based on values`() {
        val a = AppNotification("n1", NotificationType.SYSTEM, "T", "B", "", 100L, false, "u1")
        val b = AppNotification("n1", NotificationType.SYSTEM, "T", "B", "", 100L, false, "u1")
        assertEquals(a, b)
    }
}

// ── Favorite ──────────────────────────────────────────────────────────────────

class FavoriteTest {

    @Test fun `default values`() {
        val fav = Favorite()
        assertEquals("", fav.dogRemoteId)
        assertEquals(0L, fav.createdAt)
    }

    @Test fun `constructor stores provided values`() {
        val fav = Favorite(dogRemoteId = "dog123", createdAt = 5000L)
        assertEquals("dog123", fav.dogRemoteId)
        assertEquals(5000L, fav.createdAt)
    }

    @Test fun `copy changes specified field`() {
        val original = Favorite(dogRemoteId = "d1", createdAt = 100L)
        val copy = original.copy(createdAt = 200L)
        assertEquals("d1", copy.dogRemoteId)
        assertEquals(200L, copy.createdAt)
    }

    @Test fun `equality based on values`() {
        val a = Favorite(dogRemoteId = "d1", createdAt = 100L)
        val b = Favorite(dogRemoteId = "d1", createdAt = 100L)
        assertEquals(a, b)
    }

    @Test fun `inequality when dogRemoteId differs`() {
        val a = Favorite(dogRemoteId = "d1", createdAt = 100L)
        val b = Favorite(dogRemoteId = "d2", createdAt = 100L)
        assertNotEquals(a, b)
    }
}

// ── NotificationType ──────────────────────────────────────────────────────────

class NotificationTypeTest {

    @Test fun `enum has six values`() {
        assertEquals(6, NotificationType.values().size)
    }

    @Test fun `WALK_LIKE ordinal is zero`() {
        assertEquals(0, NotificationType.WALK_LIKE.ordinal)
    }

    @Test fun `WALK_TRENDING ordinal is one`() {
        assertEquals(1, NotificationType.WALK_TRENDING.ordinal)
    }

    @Test fun `LOCATION_COMMENT ordinal is two`() {
        assertEquals(2, NotificationType.LOCATION_COMMENT.ordinal)
    }

    @Test fun `NEW_FOLLOWER ordinal is three`() {
        assertEquals(3, NotificationType.NEW_FOLLOWER.ordinal)
    }

    @Test fun `WALK_REMINDER ordinal is four`() {
        assertEquals(4, NotificationType.WALK_REMINDER.ordinal)
    }

    @Test fun `SYSTEM ordinal is five`() {
        assertEquals(5, NotificationType.SYSTEM.ordinal)
    }

    @Test fun `valueOf returns correct enum for each name`() {
        for (type in NotificationType.values()) {
            assertEquals(type, NotificationType.valueOf(type.name))
        }
    }

    @Test fun `name matches expected string`() {
        assertEquals("WALK_LIKE", NotificationType.WALK_LIKE.name)
        assertEquals("SYSTEM", NotificationType.SYSTEM.name)
    }
}
