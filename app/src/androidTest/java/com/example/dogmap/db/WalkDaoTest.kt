package com.example.dogmap.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dogmap.data.db.DogDatabase
import com.example.dogmap.data.db.WalkDao
import com.example.dogmap.data.models.TrackPoint
import com.example.dogmap.data.models.Walk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WalkDaoTest {

    private lateinit var database: DogDatabase
    private lateinit var dao: WalkDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DogDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.walkDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    // ── insertWalk / getAllWalks ───────────────────────────────────────────────

    @Test
    fun insertWalk_and_getAllWalks_returnsInsertedWalk() = runTest {
        val walk = Walk(name = "Morning jog", authorId = "user1")
        dao.insertWalk(walk)
        val all = dao.getAllWalks().first()
        assertEquals(1, all.size)
        assertEquals("Morning jog", all[0].name)
    }

    @Test
    fun insertWalk_returnsAssignedId() = runTest {
        val id = dao.insertWalk(Walk(name = "Test walk"))
        assertTrue(id > 0)
    }

    @Test
    fun getAllWalks_orderedByCreatedAtDescending() = runTest {
        dao.insertWalk(Walk(name = "Old", createdAt = 100L))
        dao.insertWalk(Walk(name = "New", createdAt = 500L))
        dao.insertWalk(Walk(name = "Mid", createdAt = 300L))
        val all = dao.getAllWalks().first()
        assertEquals("New", all[0].name)
        assertEquals("Mid", all[1].name)
        assertEquals("Old", all[2].name)
    }

    @Test
    fun getAllWalks_emptyWhenDatabaseIsEmpty() = runTest {
        assertTrue(dao.getAllWalks().first().isEmpty())
    }

    @Test
    fun insertWalk_replacesOnConflict() = runTest {
        val id = dao.insertWalk(Walk(name = "Original"))
        val retrieved = dao.getWalkById(id)!!
        dao.insertWalk(retrieved.copy(name = "Replaced"))
        val all = dao.getAllWalks().first()
        assertEquals(1, all.size)
        assertEquals("Replaced", all[0].name)
    }

    // ── getWalkById ───────────────────────────────────────────────────────────

    @Test
    fun getWalkById_returnsCorrectWalk() = runTest {
        val id = dao.insertWalk(Walk(name = "FindMe"))
        val walk = dao.getWalkById(id)
        assertNotNull(walk)
        assertEquals("FindMe", walk!!.name)
    }

    @Test
    fun getWalkById_returnsNull_whenNotFound() = runTest {
        assertNull(dao.getWalkById(9999L))
    }

    // ── getWalkByRemoteId ─────────────────────────────────────────────────────

    @Test
    fun getWalkByRemoteId_returnsCorrectWalk() = runTest {
        dao.insertWalk(Walk(name = "Remote walk", remoteId = "remote123"))
        val walk = dao.getWalkByRemoteId("remote123")
        assertNotNull(walk)
        assertEquals("Remote walk", walk!!.name)
        assertEquals("remote123", walk.remoteId)
    }

    @Test
    fun getWalkByRemoteId_returnsNull_whenNotFound() = runTest {
        assertNull(dao.getWalkByRemoteId("nonexistent"))
    }

    @Test
    fun getWalkByRemoteId_returnsFirstWhenMultipleMatch() = runTest {
        dao.insertWalk(Walk(name = "Walk1", remoteId = "dup"))
        dao.insertWalk(Walk(name = "Walk2", remoteId = "dup"))
        val walk = dao.getWalkByRemoteId("dup")
        assertNotNull(walk)
    }

    // ── getWalksByAuthor ──────────────────────────────────────────────────────

    @Test
    fun getWalksByAuthor_returnsOnlyWalksForThatAuthor() = runTest {
        dao.insertWalk(Walk(name = "A1", authorId = "alice", createdAt = 100L))
        dao.insertWalk(Walk(name = "A2", authorId = "alice", createdAt = 200L))
        dao.insertWalk(Walk(name = "B1", authorId = "bob", createdAt = 300L))
        val aliceWalks = dao.getWalksByAuthor("alice").first()
        assertEquals(2, aliceWalks.size)
        assertTrue(aliceWalks.all { it.authorId == "alice" })
    }

    @Test
    fun getWalksByAuthor_orderedByCreatedAtDescending() = runTest {
        dao.insertWalk(Walk(name = "First", authorId = "alice", createdAt = 100L))
        dao.insertWalk(Walk(name = "Second", authorId = "alice", createdAt = 200L))
        val walks = dao.getWalksByAuthor("alice").first()
        assertEquals("Second", walks[0].name)
        assertEquals("First", walks[1].name)
    }

    @Test
    fun getWalksByAuthor_emptyWhenNoWalksForAuthor() = runTest {
        dao.insertWalk(Walk(name = "Walk", authorId = "alice"))
        val walks = dao.getWalksByAuthor("bob").first()
        assertTrue(walks.isEmpty())
    }

    // ── getWalksByDog ─────────────────────────────────────────────────────────

    @Test
    fun getWalksByDog_returnsOnlyWalksForThatDog() = runTest {
        dao.insertWalk(Walk(name = "W1", dogId = "dog1", createdAt = 100L))
        dao.insertWalk(Walk(name = "W2", dogId = "dog1", createdAt = 200L))
        dao.insertWalk(Walk(name = "W3", dogId = "dog2", createdAt = 300L))
        val dog1Walks = dao.getWalksByDog("dog1").first()
        assertEquals(2, dog1Walks.size)
        assertTrue(dog1Walks.all { it.dogId == "dog1" })
    }

    @Test
    fun getWalksByDog_orderedByCreatedAtDescending() = runTest {
        dao.insertWalk(Walk(name = "OldDog", dogId = "dog1", createdAt = 100L))
        dao.insertWalk(Walk(name = "NewDog", dogId = "dog1", createdAt = 400L))
        val walks = dao.getWalksByDog("dog1").first()
        assertEquals("NewDog", walks[0].name)
        assertEquals("OldDog", walks[1].name)
    }

    @Test
    fun getWalksByDog_emptyWhenNoneExist() = runTest {
        assertTrue(dao.getWalksByDog("nonexistent").first().isEmpty())
    }

    // ── updateWalk ────────────────────────────────────────────────────────────

    @Test
    fun updateWalk_changesStoredValues() = runTest {
        val id = dao.insertWalk(Walk(name = "Before", distanceMeters = 100.0))
        val retrieved = dao.getWalkById(id)!!
        dao.updateWalk(retrieved.copy(name = "After", distanceMeters = 500.0))
        val updated = dao.getWalkById(id)!!
        assertEquals("After", updated.name)
        assertEquals(500.0, updated.distanceMeters, 0.001)
    }

    @Test
    fun updateWalk_preservesTrackPoints() = runTest {
        val points = listOf(TrackPoint(lat = 1.0, lon = 2.0, timestamp = 100L))
        val id = dao.insertWalk(Walk(name = "WithPoints", points = points))
        val retrieved = dao.getWalkById(id)!!
        dao.updateWalk(retrieved.copy(name = "UpdatedName"))
        val updated = dao.getWalkById(id)!!
        assertEquals(1, updated.points.size)
        assertEquals(1.0, updated.points[0].lat, 0.0001)
    }

    // ── deleteWalk ────────────────────────────────────────────────────────────

    @Test
    fun deleteWalk_removesItFromDb() = runTest {
        val id = dao.insertWalk(Walk(name = "ToDelete"))
        val walk = dao.getWalkById(id)!!
        dao.deleteWalk(walk)
        assertNull(dao.getWalkById(id))
    }

    @Test
    fun deleteWalk_doesNotAffectOtherWalks() = runTest {
        val id1 = dao.insertWalk(Walk(name = "Keep"))
        val id2 = dao.insertWalk(Walk(name = "Delete"))
        val walk2 = dao.getWalkById(id2)!!
        dao.deleteWalk(walk2)
        assertNotNull(dao.getWalkById(id1))
        assertEquals(1, dao.getAllWalks().first().size)
    }

    // ── deleteByAuthor ────────────────────────────────────────────────────────

    @Test
    fun deleteByAuthor_removesAllWalksForThatAuthor() = runTest {
        dao.insertWalk(Walk(name = "A1", authorId = "alice"))
        dao.insertWalk(Walk(name = "A2", authorId = "alice"))
        dao.insertWalk(Walk(name = "B1", authorId = "bob"))
        dao.deleteByAuthor("alice")
        val remaining = dao.getAllWalks().first()
        assertEquals(1, remaining.size)
        assertEquals("B1", remaining[0].name)
    }

    @Test
    fun deleteByAuthor_noMatchDoesNothing() = runTest {
        dao.insertWalk(Walk(name = "Walk", authorId = "alice"))
        dao.deleteByAuthor("nobody")
        assertEquals(1, dao.getAllWalks().first().size)
    }

    // ── TrackPoints serialization round-trip ──────────────────────────────────

    @Test
    fun insertWalk_withTrackPoints_persistsAndReadsCorrectly() = runTest {
        val points = listOf(
            TrackPoint(lat = 40.4, lon = -3.7, timestamp = 1000L),
            TrackPoint(lat = 40.5, lon = -3.8, timestamp = 2000L)
        )
        val id = dao.insertWalk(Walk(name = "GPS Walk", points = points))
        val retrieved = dao.getWalkById(id)!!
        assertEquals(2, retrieved.points.size)
        assertEquals(40.4, retrieved.points[0].lat, 0.0001)
        assertEquals(-3.7, retrieved.points[0].lon, 0.0001)
        assertEquals(1000L, retrieved.points[0].timestamp)
        assertEquals(40.5, retrieved.points[1].lat, 0.0001)
    }

    @Test
    fun insertWalk_withEmptyTrackPoints_persistsEmptyList() = runTest {
        val id = dao.insertWalk(Walk(name = "No GPS", points = emptyList()))
        val retrieved = dao.getWalkById(id)!!
        assertTrue(retrieved.points.isEmpty())
    }
}
