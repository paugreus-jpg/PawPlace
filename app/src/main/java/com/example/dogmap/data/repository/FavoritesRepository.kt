package com.example.dogmap.data.repository

import com.example.dogmap.data.models.Dog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FavoritesRepository {

    private val firestore = FirebaseFirestore.getInstance()

    private fun userFavoritesRef(uid: String) =
        firestore.collection("users").document(uid).collection("favorites")

    fun observeFavoriteIds(uid: String): Flow<Set<String>> {
        if (uid.isBlank()) return flowOf(emptySet())
        return callbackFlow {
            val reg = userFavoritesRef(uid).addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                trySend(ids)
            }
            awaitClose { reg.remove() }
        }
    }

    fun observeFavoriteDogs(uid: String): Flow<List<Dog>> {
        if (uid.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val reg = userFavoritesRef(uid).addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id } ?: emptyList()
                if (ids.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                launch(Dispatchers.IO) {
                    val dogs = mutableListOf<Dog>()
                    ids.chunked(10).forEach { chunk ->
                        try {
                            val result = firestore.collection("dogs")
                                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                                .get()
                                .await()
                            result.documents.forEach { d ->
                                d.toObject(Dog::class.java)?.copy(remoteId = d.id)?.let { dogs.add(it) }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    trySend(dogs.sortedByDescending { it.createdAt })
                }
            }
            awaitClose { reg.remove() }
        }
    }

    suspend fun deleteAllFavorites(uid: String) {
        if (uid.isBlank()) return
        try {
            val favs = userFavoritesRef(uid).get().await()
            for (doc in favs.documents) {
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}
    }

    suspend fun toggleFavorite(uid: String, dogRemoteId: String): Boolean {
        if (uid.isBlank() || dogRemoteId.isBlank()) return false
        return try {
            val docRef = userFavoritesRef(uid).document(dogRemoteId)
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
}
