package com.example.dogmap.data.db

import androidx.room.*
import com.example.dogmap.data.models.Dog
import kotlinx.coroutines.flow.Flow

@Dao
interface DogDao {
    @Query("SELECT * FROM dogs ORDER BY createdAt DESC")
    fun getAllDogs(): Flow<List<Dog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDog(dog: Dog): Long

    @Delete
    suspend fun deleteDog(dog: Dog)

    @Update
    suspend fun updateDog(dog: Dog)

    @Query("SELECT * FROM dogs WHERE id = :id")
    suspend fun getDogById(id: Long): Dog?

    @Query("SELECT * FROM dogs WHERE id = :id")
    fun getDogByIdAsFlow(id: Long): Flow<Dog?>

    @Query("UPDATE dogs SET likesCount = likesCount + :delta WHERE remoteId = :remoteId")
    suspend fun updateLikesCount(remoteId: String, delta: Int)

    @Query("DELETE FROM dogs WHERE authorId = :authorId")
    suspend fun deleteByAuthor(authorId: String)

    @Query("SELECT * FROM dogs WHERE isPublic = 1 ORDER BY createdAt DESC")
    fun getPublicDogs(): Flow<List<Dog>>

    @Query("SELECT * FROM dogs WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getDogsByAuthor(authorId: String): Flow<List<Dog>>
}
