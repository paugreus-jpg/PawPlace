package com.example.dogmap.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogmap.data.models.TrackPoint
import com.example.dogmap.data.models.User
import com.example.dogmap.data.models.Walk
import com.example.dogmap.data.repository.UserRepository
import com.example.dogmap.data.repository.WalkRepository
import com.example.dogmap.service.WalkTrackingService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WalkViewModel(
    private val walkRepository: WalkRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUid = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            _currentUid.value = auth.currentUser?.uid
        }
    }

    private val currentUser: StateFlow<User?> = _currentUid
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else userRepository.observeUser(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val publicWalks: StateFlow<List<Walk>> = walkRepository.observePublicWalks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val myWalks: StateFlow<List<Walk>> = _currentUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else walkRepository.observeWalksByAuthorRemote(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteWalkIds: StateFlow<Set<String>> = _currentUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptySet())
            else walkRepository.observeFavoriteWalkIds(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val favoriteWalks: StateFlow<List<Walk>> = _currentUid
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList())
            else walkRepository.observeFavoriteWalks(uid)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val likeFlows = mutableMapOf<String, StateFlow<Boolean>>()
    private val walkFlows = mutableMapOf<String, StateFlow<Walk?>>()

    fun likeStateFor(walkRemoteId: String): StateFlow<Boolean> {
        if (walkRemoteId.isBlank()) return MutableStateFlow(false).asStateFlow()
        return likeFlows.getOrPut(walkRemoteId) {
            _currentUid
                .flatMapLatest { uid ->
                    if (uid == null) flowOf(false)
                    else walkRepository.observeUserLike(walkRemoteId, uid)
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)
        }
    }

    fun observeWalk(remoteId: String): StateFlow<Walk?> {
        if (remoteId.isBlank()) return MutableStateFlow<Walk?>(null).asStateFlow()
        return walkFlows.getOrPut(remoteId) {
            walkRepository.observeWalkByRemoteId(remoteId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        }
    }

    fun toggleLike(walk: Walk) {
        val uid = _currentUid.value ?: return
        if (walk.remoteId.isBlank()) return
        viewModelScope.launch { walkRepository.toggleLike(walk.remoteId, uid) }
    }

    fun toggleFavorite(walk: Walk) {
        val uid = _currentUid.value ?: return
        if (walk.remoteId.isBlank()) return
        viewModelScope.launch { walkRepository.toggleFavorite(uid, walk.remoteId) }
    }

    fun deleteWalk(walk: Walk) {
        viewModelScope.launch { walkRepository.deleteWalk(walk) }
    }

    // ----- Recording lifecycle (delegates to WalkTrackingService) -----

    val recordingState: StateFlow<WalkTrackingService.State> = WalkTrackingService.state
    val liveTrackPoints: StateFlow<List<TrackPoint>> = WalkTrackingService.points
    val liveDistanceMeters: StateFlow<Double> = WalkTrackingService.distanceMeters
    val liveDurationSeconds: StateFlow<Long> = WalkTrackingService.durationSeconds

    fun startRecording(context: Context) = WalkTrackingService.start(context)
    fun pauseRecording(context: Context) = WalkTrackingService.pause(context)
    fun resumeRecording(context: Context) = WalkTrackingService.resume(context)
    fun stopRecording(context: Context) = WalkTrackingService.stop(context)
    fun resetRecording() = WalkTrackingService.resetSession()

    fun saveRecordedWalk(
        name: String,
        description: String,
        dogId: String,
        dogName: String,
        isPublic: Boolean,
        points: List<TrackPoint>,
        distanceMeters: Double,
        durationSeconds: Long,
        onSaved: (Walk) -> Unit = {}
    ) {
        val user = currentUser.value
        val uid = _currentUid.value ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        viewModelScope.launch {
            val walk = Walk(
                authorId = uid,
                authorName = user?.displayName ?: "",
                authorPhotoUrl = user?.photoUrl ?: "",
                name = name.trim(),
                description = description.trim(),
                dogId = dogId,
                dogName = dogName,
                isPublic = isPublic,
                createdAt = System.currentTimeMillis(),
                distanceMeters = distanceMeters,
                durationSeconds = durationSeconds,
                points = points
            )
            val saved = walkRepository.addWalk(walk)
            resetRecording()
            onSaved(saved)
        }
    }
}
