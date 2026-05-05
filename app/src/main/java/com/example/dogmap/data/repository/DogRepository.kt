package com.example.dogmap.data.repository

import com.example.dogmap.data.db.DogDao
import com.example.dogmap.data.models.Comment
import com.example.dogmap.data.models.Dog
import com.example.dogmap.data.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class DogRepository(private val dogDao: DogDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val dogsCollection = firestore.collection("dogs")

    val allDogs: Flow<List<Dog>> = dogDao.getAllDogs()

    suspend fun addDog(dog: Dog) {
        val localId = dogDao.insertDog(dog)
        try {
            val docRef = dogsCollection.add(dog.copy(id = localId)).await()
            dogDao.updateDog(dog.copy(id = localId, remoteId = docRef.id))
        } catch (e: Exception) {
            // El perro queda guardado localmente aunque falle Firestore
        }
    }

    suspend fun deleteDog(dog: Dog) {
        dogDao.deleteDog(dog)
        if (dog.remoteId.isNotBlank()) {
            try {
                dogsCollection.document(dog.remoteId).delete().await()
            } catch (e: Exception) {
                // El borrado local ya se realizó
            }
        }
    }

    fun observeDogsByAuthor(authorId: String): Flow<List<Dog>> {
        if (authorId.isBlank()) return flowOf(emptyList())
        return dogDao.getDogsByAuthor(authorId)
    }

    suspend fun toggleLike(dogRemoteId: String, uid: String): Boolean {
        if (dogRemoteId.isBlank() || uid.isBlank()) return false
        val dogRef = dogsCollection.document(dogRemoteId)
        val likeRef = dogRef.collection("likes").document(uid)
        val snap = likeRef.get().await()
        return if (snap.exists()) {
            likeRef.delete().await()
            try {
                dogRef.update("likesCount", FieldValue.increment(-1)).await()
                dogDao.updateLikesCount(dogRemoteId, -1)
            } catch (_: Exception) {}
            false
        } else {
            likeRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
            try {
                dogRef.update("likesCount", FieldValue.increment(1)).await()
                dogDao.updateLikesCount(dogRemoteId, 1)
            } catch (_: Exception) {}
            try {
                val dogSnap = dogRef.get().await()
                val authorId = dogSnap.getString("authorId").orEmpty()
                val placeName = dogSnap.getString("name").orEmpty()
                val likerName = FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()
                if (authorId.isNotBlank() && authorId != uid) {
                    val body = if (likerName.isNotBlank()) "$likerName le ha dado me gusta a $placeName"
                               else "Alguien le ha dado me gusta a $placeName"
                    pushNotification(authorId, NotificationType.WALK_LIKE,
                        "¡A alguien le gusta tu lugar!", body,
                        """{"dogRemoteId":"$dogRemoteId"}""")
                }
            } catch (_: Exception) {}
            true
        }
    }

    suspend fun deleteAllDogsByAuthor(authorId: String) {
        if (authorId.isBlank()) return
        try {
            val docs = dogsCollection.whereEqualTo("authorId", authorId).get().await()
            for (doc in docs.documents) {
                // Delete all likes in the subcollection
                val likes = doc.reference.collection("likes").get().await()
                for (like in likes.documents) {
                    like.reference.delete().await()
                }
                // Delete the dog document
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}
        dogDao.deleteByAuthor(authorId)
    }

    suspend fun deleteAllDogsCompletely() {
        try {
            val allDocs = dogsCollection.get().await()
            for (doc in allDocs.documents) {
                val likes = doc.reference.collection("likes").get().await()
                for (like in likes.documents) like.reference.delete().await()
                val comments = doc.reference.collection("comments").get().await()
                for (comment in comments.documents) comment.reference.delete().await()
                doc.reference.delete().await()
            }
        } catch (_: Exception) {}
        dogDao.deleteAllDogs()
    }

    suspend fun removeAllUserLikes(uid: String) {
        if (uid.isBlank()) return
        try {
            val allDogs = dogsCollection.get().await()
            for (doc in allDogs.documents) {
                val likeRef = doc.reference.collection("likes").document(uid)
                val likeSnap = likeRef.get().await()
                if (likeSnap.exists()) {
                    likeRef.delete().await()
                    doc.reference.update("likesCount", FieldValue.increment(-1)).await()
                }
            }
        } catch (_: Exception) {}
    }

    fun observeDogByLocalId(id: Long): Flow<Dog?> = dogDao.getDogByIdAsFlow(id)

    fun observeDogByRemoteId(remoteId: String): Flow<Dog?> {
        if (remoteId.isBlank()) return flowOf(null)
        return callbackFlow {
            val reg = dogsCollection.document(remoteId)
                .addSnapshotListener { snap, _ ->
                    val dog = snap?.toObject(Dog::class.java)?.copy(remoteId = snap.id)
                    trySend(dog)
                }
            awaitClose { reg.remove() }
        }
    }

    fun observeComments(dogRemoteId: String): Flow<List<Comment>> {
        if (dogRemoteId.isBlank()) return flowOf(emptyList())
        return callbackFlow {
            val reg = dogsCollection.document(dogRemoteId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snap, _ ->
                    val list = snap?.documents?.mapNotNull { d ->
                        d.toObject(Comment::class.java)?.copy(id = d.id)
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { reg.remove() }
        }
    }

    suspend fun addComment(dogRemoteId: String, comment: Comment) {
        if (dogRemoteId.isBlank()) return
        dogsCollection.document(dogRemoteId)
            .collection("comments")
            .add(comment)
            .await()
        try {
            val dogSnap = dogsCollection.document(dogRemoteId).get().await()
            val authorId = dogSnap.getString("authorId").orEmpty()
            val placeName = dogSnap.getString("name").orEmpty()
            if (authorId.isNotBlank() && authorId != comment.authorId) {
                val body = if (comment.authorName.isNotBlank()) "${comment.authorName} ha comentado en $placeName"
                           else "Alguien ha comentado en $placeName"
                pushNotification(authorId, NotificationType.LOCATION_COMMENT,
                    "¡Nuevo comentario en tu lugar!", body,
                    """{"dogRemoteId":"$dogRemoteId"}""")
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteComment(dogRemoteId: String, commentId: String) {
        if (dogRemoteId.isBlank() || commentId.isBlank()) return
        dogsCollection.document(dogRemoteId)
            .collection("comments")
            .document(commentId)
            .delete()
            .await()
    }

    fun observePublicDogs(): Flow<List<Dog>> = callbackFlow {
        val reg = dogsCollection
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap.documents.mapNotNull { d ->
                    d.toObject(Dog::class.java)?.copy(remoteId = d.id)
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun togglePublic(dog: Dog, isPublic: Boolean) {
        val updated = dog.copy(isPublic = isPublic)
        dogDao.updateDog(updated)
        if (dog.remoteId.isNotBlank()) {
            try {
                dogsCollection.document(dog.remoteId).update("isPublic", isPublic).await()
            } catch (_: Exception) {}
        }
    }

    fun observeUserLike(dogRemoteId: String, uid: String): Flow<Boolean> {
        if (dogRemoteId.isBlank() || uid.isBlank()) return flowOf(false)
        return callbackFlow {
            val reg = dogsCollection.document(dogRemoteId)
                .collection("likes").document(uid)
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.exists() == true)
                }
            awaitClose { reg.remove() }
        }
    }

    private suspend fun pushNotification(
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
        firestore.collection("users/$targetUid/notifications")
            .add(doc)
            .await()
    }
}
