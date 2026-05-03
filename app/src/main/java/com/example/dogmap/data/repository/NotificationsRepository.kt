package com.example.dogmap.data.repository

import com.example.dogmap.data.db.NotificationDao
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.models.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationsRepository(private val dao: NotificationDao) {

    private var listenerRegistration: ListenerRegistration? = null

    fun notificationsFlow(uid: String): Flow<List<AppNotification>> = dao.getAllForUser(uid)

    fun unreadCountFlow(uid: String): Flow<Int> = dao.unreadCount(uid)

    fun startSync(uid: String) {
        listenerRegistration?.remove()
        val firestore = FirebaseFirestore.getInstance()
        listenerRegistration = firestore.collection("users/$uid/notifications")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener
                val notifications = snapshots.documents.mapNotNull { doc ->
                    runCatching {
                        AppNotification(
                            id = doc.id,
                            type = NotificationType.valueOf(
                                doc.getString("type") ?: NotificationType.SYSTEM.name
                            ),
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            payloadJson = doc.getString("payloadJson") ?: "",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            read = doc.getBoolean("read") ?: false,
                            ownerUid = uid
                        )
                    }.getOrNull()
                }
                CoroutineScope(Dispatchers.IO).launch {
                    dao.insertAll(notifications)
                }
            }
    }

    fun stopSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    suspend fun markRead(id: String) = dao.markRead(id)

    suspend fun markAllRead(uid: String) = dao.markAllRead(uid)

    suspend fun clearOld() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.deleteOlderThan(thirtyDaysAgo)
    }

    suspend fun pushLocal(notification: AppNotification) {
        dao.insertAll(listOf(notification))
    }

    suspend fun pushSocialEvent(
        targetUid: String,
        type: NotificationType,
        title: String,
        body: String,
        payloadJson: String = ""
    ) {
        val doc = hashMapOf(
            "type" to type.name,
            "title" to title,
            "body" to body,
            "payloadJson" to payloadJson,
            "createdAt" to System.currentTimeMillis(),
            "read" to false,
            "ownerUid" to targetUid
        )
        FirebaseFirestore.getInstance()
            .collection("users/$targetUid/notifications")
            .add(doc)
            .await()
    }
}
