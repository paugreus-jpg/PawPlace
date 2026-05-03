package com.example.dogmap.data.repository

import com.example.dogmap.data.models.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun ensureUserDocument(uid: String, email: String, displayName: String): User {
        val doc = usersCollection.document(uid).get().await()
        return if (doc.exists()) {
            doc.toObject(User::class.java) ?: User(uid = uid, email = email, displayName = displayName)
        } else {
            val user = User(
                uid = uid,
                email = email,
                displayName = displayName,
                createdAt = System.currentTimeMillis()
            )
            usersCollection.document(uid).set(user).await()
            user
        }
    }

    suspend fun getUser(uid: String): User? {
        if (uid.isBlank()) return null
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        if (uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = usersCollection.document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(User::class.java))
        }
        awaitClose { registration.remove() }
    }

    suspend fun updateProfile(user: User) {
        if (user.uid.isBlank()) return
        usersCollection.document(user.uid).set(user).await()
    }

    suspend fun isProfileSetupCompleted(uid: String): Boolean {
        if (uid.isBlank()) return false
        return try {
            val doc = usersCollection.document(uid).get().await()
            doc.getBoolean("profileSetupCompleted") ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun markProfileSetupCompleted(uid: String) {
        if (uid.isBlank()) return
        try {
            usersCollection.document(uid).update("profileSetupCompleted", true).await()
        } catch (_: Exception) {}
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    suspend fun deleteAccount(
        uid: String,
        password: String,
        dogRepository: DogRepository,
        favoritesRepository: FavoritesRepository
    ): Result<Unit> {
        return try {
            val authUser = FirebaseAuth.getInstance().currentUser
                ?: return Result.failure(Exception("No hay sesión activa"))

            // Re-authenticate before deletion (Firebase requirement)
            val email = authUser.email
                ?: return Result.failure(Exception("No se puede obtener el email"))
            val credential = EmailAuthProvider.getCredential(email, password)
            authUser.reauthenticate(credential).await()

            // 1. Delete user's authored dogs (including their likes subcollections) + Room
            dogRepository.deleteAllDogsByAuthor(uid)

            // 2. Delete user's likes on other dogs + decrement counters
            dogRepository.removeAllUserLikes(uid)

            // 3. Delete favorites subcollection
            favoritesRepository.deleteAllFavorites(uid)

            // 4. Delete user document
            usersCollection.document(uid).delete().await()

            // 5. Delete Firebase Auth account
            authUser.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
