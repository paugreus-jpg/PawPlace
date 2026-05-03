package com.example.dogmap.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dogmap.data.db.DogDatabase
import com.example.dogmap.data.db.DogDao
import com.example.dogmap.data.models.Dog
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
class DogDaoTest {

    private lateinit var database: DogDatabase
    private lateinit var dao: DogDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DogDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.dogDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    // ── insertDog / getAllDogs ─────────────────────────────────────────────────

    @Test
    fun insertDog_and_getAllDogs_returnsInsertedDog() = runTest {
        val dog = Dog(name = "Buddy", authorId = "author1")
        dao.insertDog(dog)
        val all = dao.getAllDogs().first()
        assertEquals(1, all.size)
        assertEquals("Buddy", all[0].name)
    }

    @Test
    fun insertDog_returnsAssignedId() = runTest {
        val dog = Dog(name = "Rex")
        val id = dao.insertDog(dog)
        assertTrue(id > 0)
    }

    @Test
    fun insertMultipleDogs_allReturnedByGetAllDogs() = runTest {
        dao.insertDog(Dog(name = "A", createdAt = 100L))
        dao.insertDog(Dog(name = "B", createdAt = 200L))
        dao.insertDog(Dog(name = "C", createdAt = 300L))
        val all = dao.getAllDogs().first()
        assertEquals(3, all.size)
    }

    @Test
    fun getAllDogs_orderedByCreatedAtDescending() = runTest {
        dao.insertDog(Dog(name = "Old", createdAt = 100L))
        dao.insertDog(Dog(name = "New", createdAt = 300L))
        dao.insertDog(Dog(name = "Mid", createdAt = 200L))
        val all = dao.getAllDogs().first()
        assertEquals("New", all[0].name)
        assertEquals("Mid", all[1].name)
        assertEquals("Old", all[2].name)
    }

    @Test
    fun insertDog_replacesOnConflict() = runTest {
        val id = dao.insertDog(Dog(name = "Original"))
        val retrieved = dao.getDogById(id)
        val replacement = retrieved!!.copy(name = "Replaced")
        dao.insertDog(replacement)
        val all = dao.getAllDogs().first()
        assertEquals(1, all.size)
        assertEquals("Replaced", all[0].name)
    }

    // ── getDogById ────────────────────────────────────────────────────────────

    @Test
    fun getDogById_returnsCorrectDog() = runTest {
        val id = dao.insertDog(Dog(name = "FindMe"))
        val dog = dao.getDogById(id)
        assertNotNull(dog)
        assertEquals("FindMe", dog!!.name)
    }

    @Test
    fun getDogById_returnsNull_whenNotFound() = runTest {
        val dog = dao.getDogById(9999L)
        assertNull(dog)
    }

    // ── updateDog ─────────────────────────────────────────────────────────────

    @Test
    fun updateDog_changesStoredValues() = runTest {
        val id = dao.insertDog(Dog(name = "Before"))
        val retrieved = dao.getDogById(id)!!
        dao.updateDog(retrieved.copy(name = "After", rating = 4.5f))
        val updated = dao.getDogById(id)!!
        assertEquals("After", updated.name)
        assertEquals(4.5f, updated.rating)
    }

    @Test
    fun updateDog_doesNotAffectOtherDogs() = runTest {
        val id1 = dao.insertDog(Dog(name = "Dog1"))
        val id2 = dao.insertDog(Dog(name = "Dog2"))
        val dog1 = dao.getDogById(id1)!!
        dao.updateDog(dog1.copy(name = "UpdatedDog1"))
        val dog2 = dao.getDogById(id2)!!
        assertEquals("Dog2", dog2.name)
    }

    // ── deleteDog ─────────────────────────────────────────────────────────────

    @Test
    fun deleteDog_removesItFromDb() = runTest {
        val id = dao.insertDog(Dog(name = "ToDelete"))
        val dog = dao.getDogById(id)!!
        dao.deleteDog(dog)
        assertNull(dao.getDogById(id))
    }

    @Test
    fun deleteDog_doesNotAffectOtherDogs() = runTest {
        val id1 = dao.insertDog(Dog(name = "Keep"))
        val id2 = dao.insertDog(Dog(name = "Remove"))
        val dog2 = dao.getDogById(id2)!!
        dao.deleteDog(dog2)
        assertNotNull(dao.getDogById(id1))
        val all = dao.getAllDogs().first()
        assertEquals(1, all.size)
        assertEquals("Keep", all[0].name)
    }

    // ── deleteByAuthor ────────────────────────────────────────────────────────

    @Test
    fun deleteByAuthor_removesAllDogsForThatAuthor() = runTest {
        dao.insertDog(Dog(name = "A", authorId = "author1"))
        dao.insertDog(Dog(name = "B", authorId = "author1"))
        dao.insertDog(Dog(name = "C", authorId = "author2"))
        dao.deleteByAuthor("author1")
        val all = dao.getAllDogs().first()
        assertEquals(1, all.size)
        assertEquals("C", all[0].name)
    }

    @Test
    fun deleteByAuthor_withNoMatchingAuthor_doesNothing() = runTest {
        dao.insertDog(Dog(name = "Dog", authorId = "author1"))
        dao.deleteByAuthor("nonexistent")
        assertEquals(1, dao.getAllDogs().first().size)
    }

    // ── getPublicDogs ─────────────────────────────────────────────────────────

    @Test
    fun getPublicDogs_returnsOnlyPublicDogs() = runTest {
        dao.insertDog(Dog(name = "Public1", isPublic = true, createdAt = 100L))
        dao.insertDog(Dog(name = "Private", isPublic = false, createdAt = 200L))
        dao.insertDog(Dog(name = "Public2", isPublic = true, createdAt = 300L))
        val public = dao.getPublicDogs().first()
        assertEquals(2, public.size)
        assertTrue(public.all { it.isPublic })
    }

    @Test
    fun getPublicDogs_orderedByCreatedAtDescending() = runTest {
        dao.insertDog(Dog(name = "OldPublic", isPublic = true, createdAt = 100L))
        dao.insertDog(Dog(name = "NewPublic", isPublic = true, createdAt = 500L))
        val public = dao.getPublicDogs().first()
        assertEquals("NewPublic", public[0].name)
        assertEquals("OldPublic", public[1].name)
    }

    @Test
    fun getPublicDogs_emptyWhenNoneArePublic() = runTest {
        dao.insertDog(Dog(name = "Private1", isPublic = false))
        dao.insertDog(Dog(name = "Private2", isPublic = false))
        val public = dao.getPublicDogs().first()
        assertTrue(public.isEmpty())
    }

    @Test
    fun getPublicDogs_emptyWhenDatabaseIsEmpty() = runTest {
        val public = dao.getPublicDogs().first()
        assertTrue(public.isEmpty())
    }

    // ── getAllDogs on empty db ─────────────────────────────────────────────────

    @Test
    fun getAllDogs_emptyWhenDatabaseIsEmpty() = runTest {
        val all = dao.getAllDogs().first()
        assertTrue(all.isEmpty())
    }
}
