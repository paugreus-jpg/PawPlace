package com.example.dogmap.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dogmap.data.models.Walk
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {
    @Query("SELECT * FROM walks ORDER BY createdAt DESC")
    fun getAllWalks(): Flow<List<Walk>>

    @Query("SELECT * FROM walks WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getWalksByAuthor(authorId: String): Flow<List<Walk>>

    @Query("SELECT * FROM walks WHERE dogId = :dogId ORDER BY createdAt DESC")
    fun getWalksByDog(dogId: String): Flow<List<Walk>>

    @Query("SELECT * FROM walks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getWalkByRemoteId(remoteId: String): Walk?

    @Query("SELECT * FROM walks WHERE id = :id LIMIT 1")
    suspend fun getWalkById(id: Long): Walk?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalk(walk: Walk): Long

    @Update
    suspend fun updateWalk(walk: Walk)

    @Delete
    suspend fun deleteWalk(walk: Walk)

    @Query("DELETE FROM walks WHERE authorId = :authorId")
    suspend fun deleteByAuthor(authorId: String)
}
