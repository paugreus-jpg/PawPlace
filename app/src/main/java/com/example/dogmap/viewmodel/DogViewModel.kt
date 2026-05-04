package com.example.dogmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogmap.data.models.Comment
import com.example.dogmap.data.models.Dog
import com.example.dogmap.data.models.User
import com.example.dogmap.data.repository.DogRepository
import com.example.dogmap.data.repository.FavoritesRepository
import com.example.dogmap.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MapFilter { ALL, PERSONAL, COMMUNITY }

@OptIn(ExperimentalCoroutinesApi::class)
class DogViewModel(
    private val repository: DogRepository,
    private val userRepository: UserRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val allDogs: Flow<List<Dog>> = repository.allDogs

    val publicDogs: StateFlow<List<Dog>> = repository.observePublicDogs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mapFilter = MutableStateFlow(MapFilter.ALL)

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _currentUid = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            _currentUid.value = auth.currentUser?.uid
        }
    }

    val currentUser: StateFlow<User?> = _currentUid
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else userRepository.observeUser(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favoriteIds: StateFlow<Set<String>> = _currentUid
        .flatMapLatest { uid -> if (uid == null) flowOf(emptySet()) else favoritesRepository.observeFavoriteIds(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val myDogs: StateFlow<List<Dog>> = _currentUid
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeDogsByAuthor(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteDogs: StateFlow<List<Dog>> = _currentUid
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else favoritesRepository.observeFavoriteDogs(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val likeFlows = mutableMapOf<String, StateFlow<Boolean>>()
    private val dogFlows = mutableMapOf<String, StateFlow<Dog?>>()
    private val commentFlows = mutableMapOf<String, StateFlow<List<Comment>>>()

    fun likeStateFor(dogRemoteId: String): StateFlow<Boolean> {
        if (dogRemoteId.isBlank()) {
            return MutableStateFlow(false).asStateFlow()
        }
        return likeFlows.getOrPut(dogRemoteId) {
            _currentUid
                .flatMapLatest { uid ->
                    if (uid == null) flowOf(false)
                    else repository.observeUserLike(dogRemoteId, uid)
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)
        }
    }

    fun addDog(dog: Dog) {
        viewModelScope.launch {
            val user = currentUser.value
            val enriched = dog.copy(
                authorId = user?.uid ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                authorName = user?.displayName ?: "",
                authorPhotoUrl = user?.photoUrl ?: ""
            )
            repository.addDog(enriched)
        }
    }

    fun deleteDog(dog: Dog) {
        viewModelScope.launch {
            repository.deleteDog(dog)
        }
    }

    fun togglePublic(dog: Dog) {
        viewModelScope.launch {
            repository.togglePublic(dog, !dog.isPublic)
        }
    }

    fun toggleLike(dog: Dog) {
        val uid = _currentUid.value
        if (uid == null) {
            _snackbarMessage.tryEmit("Inicia sesión para dar me gusta")
            return
        }
        if (dog.remoteId.isBlank()) {
            _snackbarMessage.tryEmit("Solo puedes dar me gusta a ubicaciones de la comunidad")
            return
        }
        viewModelScope.launch {
            try {
                repository.toggleLike(dog.remoteId, uid)
            } catch (_: Exception) {
                _snackbarMessage.tryEmit("No se pudo procesar el me gusta")
            }
        }
    }

    fun toggleFavorite(dog: Dog) {
        val uid = _currentUid.value
        if (uid == null) {
            _snackbarMessage.tryEmit("Inicia sesión para guardar favoritos")
            return
        }
        if (dog.remoteId.isBlank()) {
            _snackbarMessage.tryEmit("Solo puedes guardar en favoritos ubicaciones de la comunidad")
            return
        }
        viewModelScope.launch {
            val ok = favoritesRepository.toggleFavorite(uid, dog.remoteId)
            if (!ok) _snackbarMessage.tryEmit("No se pudo actualizar favoritos")
        }
    }

    fun observeDog(remoteId: String): StateFlow<Dog?> {
        if (remoteId.isBlank()) return MutableStateFlow<Dog?>(null).asStateFlow()
        return dogFlows.getOrPut(remoteId) {
            val localId = remoteId.toLongOrNull()
            val flow = if (localId != null) {
                repository.observeDogByLocalId(localId)
            } else {
                repository.observeDogByRemoteId(remoteId)
            }
            flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
        }
    }

    fun observeComments(dogRemoteId: String): StateFlow<List<Comment>> {
        if (dogRemoteId.isBlank()) return MutableStateFlow<List<Comment>>(emptyList()).asStateFlow()
        return commentFlows.getOrPut(dogRemoteId) {
            repository.observeComments(dogRemoteId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }
    }

    fun addComment(dogRemoteId: String, text: String) {
        if (dogRemoteId.isBlank() || text.isBlank()) return
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val comment = Comment(
                    authorId = user.uid,
                    authorName = user.displayName.ifBlank { user.email?.substringBefore("@") ?: "User" },
                    authorPhotoUrl = user.photoUrl ?: "",
                    text = text.trim(),
                    createdAt = System.currentTimeMillis()
                )
                repository.addComment(dogRemoteId, comment)
            } catch (_: Exception) { }
        }
    }

    fun deleteComment(dogRemoteId: String, commentId: String) {
        if (dogRemoteId.isBlank() || commentId.isBlank()) return
        viewModelScope.launch {
            repository.deleteComment(dogRemoteId, commentId)
        }
    }
}
