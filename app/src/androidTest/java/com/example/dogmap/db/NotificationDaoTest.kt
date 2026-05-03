package com.example.dogmap.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dogmap.data.db.DogDatabase
import com.example.dogmap.data.db.NotificationDao
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.models.NotificationType
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
class NotificationDaoTest {

    private lateinit var database: DogDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DogDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.notificationDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun notif(
        id: String,
        ownerUid: String = "uid1",
        read: Boolean = false,
        createdAt: Long = System.currentTimeMillis(),
        type: NotificationType = NotificationType.SYSTEM
    ) = AppNotification(
        id = id,
        type = type,
        title = "Title $id",
        body = "Body $id",
        ownerUid = ownerUid,
        read = read,
        createdAt = createdAt
    )

    // ── insertAll / getAllForUser ───────────────────────────────────────────────

    @Test
    fun insertAll_and_getAllForUser_returnsSameNotifications() = runTest {
        val notifs = listOf(notif("n1"), notif("n2"), notif("n3"))
        dao.insertAll(notifs)
        val result = dao.getAllForUser("uid1").first()
        assertEquals(3, result.size)
    }

    @Test
    fun getAllForUser_onlyReturnsNotificationsForThatUser() = runTest {
        dao.insertAll(listOf(notif("n1", ownerUid = "uid1"), notif("n2", ownerUid = "uid2")))
        val uid1Notifs = dao.getAllForUser("uid1").first()
        assertEquals(1, uid1Notifs.size)
        assertEquals("uid1", uid1Notifs[0].ownerUid)
    }

    @Test
    fun getAllForUser_orderedByCreatedAtDescending() = runTest {
        dao.insertAll(listOf(
            notif("old", createdAt = 100L),
            notif("new", createdAt = 500L),
            notif("mid", createdAt = 300L)
        ))
        val result = dao.getAllForUser("uid1").first()
        assertEquals("new", result[0].id)
        assertEquals("mid", result[1].id)
        assertEquals("old", result[2].id)
    }

    @Test
    fun getAllForUser_emptyWhenNoneExist() = runTest {
        assertTrue(dao.getAllForUser("uid1").first().isEmpty())
    }

    @Test
    fun insertAll_replacesOnConflict() = runTest {
        dao.insertAll(listOf(notif("n1", read = false)))
        dao.insertAll(listOf(notif("n1", read = true)))
        val result = dao.getAllForUser("uid1").first()
        assertEquals(1, result.size)
        assertTrue(result[0].read)
    }

    // ── unreadCount ───────────────────────────────────────────────────────────

    @Test
    fun unreadCount_countsOnlyUnreadForUser() = runTest {
        dao.insertAll(listOf(
            notif("n1", ownerUid = "uid1", read = false),
            notif("n2", ownerUid = "uid1", read = false),
            notif("n3", ownerUid = "uid1", read = true),
            notif("n4", ownerUid = "uid2", read = false)
        ))
        val count = dao.unreadCount("uid1").first()
        assertEquals(2, count)
    }

    @Test
    fun unreadCount_isZeroWhenAllRead() = runTest {
        dao.insertAll(listOf(
            notif("n1", read = true),
            notif("n2", read = true)
        ))
        assertEquals(0, dao.unreadCount("uid1").first())
    }

    @Test
    fun unreadCount_isZeroWhenEmpty() = runTest {
        assertEquals(0, dao.unreadCount("uid1").first())
    }

    @Test
    fun unreadCount_updatesAfterMarkRead() = runTest {
        dao.insertAll(listOf(notif("n1", read = false), notif("n2", read = false)))
        assertEquals(2, dao.unreadCount("uid1").first())
        dao.markRead("n1")
        assertEquals(1, dao.unreadCount("uid1").first())
    }

    // ── markRead ──────────────────────────────────────────────────────────────

    @Test
    fun markRead_setsReadTrueForSpecificNotification() = runTest {
        dao.insertAll(listOf(notif("n1", read = false), notif("n2", read = false)))
        dao.markRead("n1")
        val result = dao.getAllForUser("uid1").first()
        val n1 = result.first { it.id == "n1" }
        val n2 = result.first { it.id == "n2" }
        assertTrue(n1.read)
        assertFalse(n2.read)
    }

    @Test
    fun markRead_withNonExistentId_doesNothing() = runTest {
        dao.insertAll(listOf(notif("n1", read = false)))
        dao.markRead("nonexistent")
        val result = dao.getAllForUser("uid1").first()
        assertFalse(result[0].read)
    }

    @Test
    fun markRead_isIdempotent() = runTest {
        dao.insertAll(listOf(notif("n1", read = false)))
        dao.markRead("n1")
        dao.markRead("n1")
        val result = dao.getAllForUser("uid1").first()
        assertTrue(result[0].read)
    }

    // ── markAllRead ───────────────────────────────────────────────────────────

    @Test
    fun markAllRead_setsReadTrueForAllNotificationsOfUser() = runTest {
        dao.insertAll(listOf(
            notif("n1", ownerUid = "uid1", read = false),
            notif("n2", ownerUid = "uid1", read = false),
            notif("n3", ownerUid = "uid1", read = false)
        ))
        dao.markAllRead("uid1")
        val result = dao.getAllForUser("uid1").first()
        assertTrue(result.all { it.read })
    }

    @Test
    fun markAllRead_doesNotAffectOtherUsers() = runTest {
        dao.insertAll(listOf(
            notif("n1", ownerUid = "uid1", read = false),
            notif("n2", ownerUid = "uid2", read = false)
        ))
        dao.markAllRead("uid1")
        val uid2Notifs = dao.getAllForUser("uid2").first()
        assertFalse(uid2Notifs[0].read)
    }

    @Test
    fun markAllRead_withNoNotifications_doesNothing() = runTest {
        dao.markAllRead("uid1")
        assertEquals(0, dao.unreadCount("uid1").first())
    }

    // ── deleteOlderThan ───────────────────────────────────────────────────────

    @Test
    fun deleteOlderThan_removesNotificationsBeforeTimestamp() = runTest {
        dao.insertAll(listOf(
            notif("old1", createdAt = 100L),
            notif("old2", createdAt = 200L),
            notif("new1", createdAt = 600L),
            notif("new2", createdAt = 700L)
        ))
        dao.deleteOlderThan(500L)
        val result = dao.getAllForUser("uid1").first()
        assertEquals(2, result.size)
        assertTrue(result.none { it.id == "old1" || it.id == "old2" })
    }

    @Test
    fun deleteOlderThan_keepsBoundaryTimestamp() = runTest {
        dao.insertAll(listOf(
            notif("boundary", createdAt = 500L),
            notif("after", createdAt = 600L)
        ))
        dao.deleteOlderThan(500L)
        val result = dao.getAllForUser("uid1").first()
        assertTrue(result.any { it.id == "boundary" })
        assertTrue(result.any { it.id == "after" })
    }

    @Test
    fun deleteOlderThan_deletesAllWhenAllAreOld() = runTest {
        dao.insertAll(listOf(notif("n1", createdAt = 100L), notif("n2", createdAt = 200L)))
        dao.deleteOlderThan(999L)
        assertTrue(dao.getAllForUser("uid1").first().isEmpty())
    }

    @Test
    fun deleteOlderThan_deletesNothingWhenAllAreNew() = runTest {
        dao.insertAll(listOf(notif("n1", createdAt = 1000L), notif("n2", createdAt = 2000L)))
        dao.deleteOlderThan(100L)
        assertEquals(2, dao.getAllForUser("uid1").first().size)
    }

    // ── NotificationType persistence ──────────────────────────────────────────

    @Test
    fun insertAll_persistsAllNotificationTypes() = runTest {
        val notifs = NotificationType.values().mapIndexed { i, type ->
            AppNotification(
                id = "n$i",
                type = type,
                title = "T",
                body = "B",
                ownerUid = "uid1"
            )
        }
        dao.insertAll(notifs)
        val result = dao.getAllForUser("uid1").first()
        assertEquals(NotificationType.values().size, result.size)
        val resultTypes = result.map { it.type }.toSet()
        for (type in NotificationType.values()) {
            assertTrue("Missing type $type", resultTypes.contains(type))
        }
    }
}
