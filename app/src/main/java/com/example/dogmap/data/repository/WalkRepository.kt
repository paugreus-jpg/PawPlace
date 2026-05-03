package com.example.dogmap.data.repository

import com.example.dogmap.data.db.WalkDao
import com.example.dogmap.data.models.Walk
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WalkRepository(private val walkDao: WalkDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val walksCollection = firestore.collection("walks")

    val allLocalWalks: Flow<List<Walk>> = walkDao.getAllWalks()

    fun walksByAuthor(authorId: String): Flow<List<Walk>> = walkDao.getWalksByAuthor(authorId)

    fun walksByDog(dogId: String): Flow<List<Walk>> = walkDao.getWalksByDog(dogId)

    suspend fun addWalk(walk: Walk): Walk {
        val localId = walkDao.insertWalk(walk)
        var saved = walk.copy(id = localId)
        if (walk.isPublic) {
            try {
                val docRef = walksCollection.add(saved).await()
                saved = saved.copy(remoteId = docRef.id)
                walkDao.updateWalk(saved)
            } catch (_: Exception) { /* queda guardado en Room */ }
        }
        return saved
    }

    suspend fun deleteWalk(walk: Walk) {
        walkDao.deleteWalk(walk)
        if (walk.remoteId.isNotBlank()) {
            try {
                walksCollection.document(walk.remoteId).delete().await()
            } catch (_: Exception) {}
        }
    }

    fun observePublicWalks(): Flow<List<Walk>> = callbackFlow {
        val reg = walksCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { d ->
                    d.toObject(Walk::class.java)?.copy(remoteId = d.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeWalksByAuthorRemote(authorId: String): Flow<List<Walk>> {
        if (authorId.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val reg = walksCollection
                .whereEqualTo("authorId", authorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    val list = snap?.documents?.mapNotNull { d ->
                        d.toObject(Walk::class.java)?.copy(remoteId = d.id)
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    fun observeWalkByRemoteId(remoteId: String): Flow<Walk?> {
        if (remoteId.isBlank()) return flowOf(null)
        return callbackFlow {
            val reg = walksCollection.document(remoteId)
                .addSnapshotListener { snap, _ ->
                    val walk = snap?.toObject(Walk::class.java)?.copy(remoteId = snap.id)
                    trySend(walk)
                }
            awaitClose { reg.remove() }
        }
    }

    suspend fun toggleLike(walkRemoteId: String, uid: String): Boolean {
        if (walkRemoteId.isBlank() || uid.isBlank()) return false
        val walkRef = walksCollection.document(walkRemoteId)
        val likeRef = walkRef.collection("likes").document(uid)
        val snap = likeRef.get().await()
        return if (snap.exists()) {
            likeRef.delete().await()
            walkRef.update("likesCount", FieldValue.increment(-1)).await()
            false
        } else {
            likeRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
            walkRef.update("likesCount", FieldValue.increment(1)).await()
            true
        }
    }

    fun observeUserLike(walkRemoteId: String, uid: String): Flow<Boolean> {
        if (walkRemoteId.isBlank() || uid.isBlank()) return flowOf(false)
        return callbackFlow {
            val reg = walksCollection.document(walkRemoteId)
                .collection("likes").document(uid)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.exists() == true)
                }
            awaitClose { reg.remove() }
        }
    }

    private fun userFavoriteWalksRef(uid: String) =
        firestore.collection("users").document(uid).collection("favoriteWalks")

    fun observeFavoriteWalkIds(uid: String): Flow<Set<String>> {
        if (uid.isBlank()) return flowOf(emptySet())
        return callbackFlow {
            val reg = userFavoriteWalksRef(uid).addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
            awaitClose { reg.remove() }
        }
    }

    fun observeFavoriteWalks(uid: String): Flow<List<Walk>> {
        if (uid.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val reg = userFavoriteWalksRef(uid).addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id } ?: emptyList()
                if (ids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                launch(Dispatchers.IO) {
                    val walks = mutableListOf<Walk>()
                    ids.chunked(10).forEach { chunk ->
                        try {
                            val result = walksCollection
                                .whereIn(FieldPath.documentId(), chunk)
                                .get()
                                .await()
                            result.documents.forEach { d ->
                                d.toObject(Walk::class.java)?.copy(remoteId = d.id)?.let(walks::add)
                            }
                        } catch (_: Exception) {}
                    }
                    trySend(walks.sortedByDescending { it.createdAt })
                }
            }
            awaitClose { reg.remove() }
        }
    }

    suspend fun toggleFavorite(uid: String, walkRemoteId: String): Boolean {
        if (uid.isBlank() || walkRemoteId.isBlank()) return false
        return try {
            val docRef = userFavoriteWalksRef(uid).document(walkRemoteId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.delete().await()
                false
            } else {
                docRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteAllWalksByAuthor(authorId: String) {
        if (authorId.isBlank()) return
        try {
            val docs = walksCollection.whereEqualTo("authorId", authorId).get().await()
            for (doc in docs.documents) {
                val likes = doc.reference.collection("likes").get().await()
                for (like in likes.documents) like.reference.delete().await()
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}
        walkDao.deleteByAuthor(authorId)
    }

    suspend fun removeAllUserWalkLikes(uid: String) {
        if (uid.isBlank()) return
        try {
            val all = walksCollection.get().await()
            for (doc in all.documents) {
                val likeRef = doc.reference.collection("likes").document(uid)
                if (likeRef.get().await().exists()) {
                    likeRef.delete().await()
                    doc.reference.update("likesCount", FieldValue.increment(-1)).await()
                }
            }
        } catch (_: Exception) {}
    }
}
