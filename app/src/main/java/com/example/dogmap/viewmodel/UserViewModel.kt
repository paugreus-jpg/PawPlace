package com.example.dogmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogmap.data.models.Dog
import com.example.dogmap.data.models.User
import com.example.dogmap.data.repository.DogRepository
import com.example.dogmap.data.repository.FavoritesRepository
import com.example.dogmap.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val dogs: List<Dog> = emptyList(),
    val favorites: List<Dog> = emptyList(),
    val editDraft: User? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null
)

class UserViewModel(
    private val userRepository: UserRepository,
    private val dogRepository: DogRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var userJob: Job? = null
    private var dogsJob: Job? = null
    private var favsJob: Job? = null

    fun loadProfile(uid: String, includeFavorites: Boolean) {
        userJob?.cancel()
        dogsJob?.cancel()
        favsJob?.cancel()

        userJob = viewModelScope.launch {
            userRepository.observeUser(uid).collect { user ->
                _state.update { it.copy(user = user) }
            }
        }
        dogsJob = viewModelScope.launch {
            dogRepository.observeDogsByAuthor(uid).collect { list ->
                _state.update { it.copy(dogs = list) }
            }
        }
        if (includeFavorites) {
            favsJob = viewModelScope.launch {
                favoritesRepository.observeFavoriteDogs(uid).collect { list ->
                    _state.update { it.copy(favorites = list) }
                }
            }
        }
    }

    fun startEditing() {
        val current = _state.value.user ?: return
        _state.update { it.copy(editDraft = current, saveError = null) }
    }

    fun cancelEditing() {
        _state.update { it.copy(editDraft = null, saveError = null) }
    }

    fun updateDraft(transform: (User) -> User) {
        _state.update { s ->
            val base = s.editDraft ?: s.user ?: return@update s
            s.copy(editDraft = transform(base))
        }
    }

    fun saveProfile(onDone: () -> Unit) {
        val draft = _state.value.editDraft ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                userRepository.updateProfile(draft)
                _state.update { it.copy(isSaving = false, editDraft = null) }
                onDone()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSaving = false, saveError = e.localizedMessage ?: "Error al guardar")
                }
            }
        }
    }

    fun checkProfileSetupAndNavigate(uid: String, onSetupCompleted: () -> Unit, onSetupNeeded: () -> Unit) {
        viewModelScope.launch {
            val completed = userRepository.isProfileSetupCompleted(uid)
            if (completed) onSetupCompleted() else onSetupNeeded()
        }
    }

    fun saveProfileSetup(uid: String, draft: com.example.dogmap.data.models.User, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                userRepository.updateProfile(draft.copy(profileSetupCompleted = true))
                _state.update { it.copy(isSaving = false, editDraft = null) }
                onDone()
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false) }
                onDone()
            }
        }
    }

    fun skipProfileSetup(uid: String, onDone: () -> Unit) {
        viewModelScope.launch {
            userRepository.markProfileSetupCompleted(uid)
            onDone()
        }
    }

    fun signOut() {
        userRepository.signOut()
        _state.value = ProfileUiState()
    }

    fun clearDeleteError() {
        _state.update { it.copy(deleteError = null) }
    }

    fun deleteAccount(password: String, onSuccess: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteError = null) }
            val result = userRepository.deleteAccount(uid, password, dogRepository, favoritesRepository)
            result.fold(
                onSuccess = {
                    _state.value = ProfileUiState()
                    onSuccess()
                },
                onFailure = { e ->
                    val message = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Contraseña incorrecta"
                        else -> e.localizedMessage ?: "Error al eliminar la cuenta"
                    }
                    _state.update { it.copy(isDeleting = false, deleteError = message) }
                }
            )
        }
    }
}
