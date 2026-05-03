package com.example.dogmap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dogmap.data.models.AppNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE ownerUid = :uid ORDER BY createdAt DESC")
    fun getAllForUser(uid: String): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE ownerUid = :uid AND read = 0")
    fun unreadCount(uid: String): Flow<Int>

    @Query("UPDATE notifications SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE notifications SET read = 1 WHERE ownerUid = :uid")
    suspend fun markAllRead(uid: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<AppNotification>)

    @Query("DELETE FROM notifications WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
