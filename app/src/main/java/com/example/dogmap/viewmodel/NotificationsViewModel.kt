package com.example.dogmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.preferences.NotificationPreferences
import com.example.dogmap.data.preferences.NotificationSettings
import com.example.dogmap.data.repository.NotificationsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val repository: NotificationsRepository,
    private val preferences: NotificationPreferences
) : ViewModel() {

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val notifications: StateFlow<List<AppNotification>> = repository.notificationsFlow(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = repository.unreadCountFlow(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val settings: StateFlow<NotificationSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    fun onNotificationClick(id: String) {
        viewModelScope.launch { repository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { repository.markAllRead(uid) }
    }

    fun toggleGlobal(enabled: Boolean) {
        viewModelScope.launch { preferences.update { it.copy(globalEnabled = enabled) } }
    }

    fun toggleSocial(enabled: Boolean) {
        viewModelScope.launch { preferences.update { it.copy(socialEnabled = enabled) } }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch { preferences.update { it.copy(remindersEnabled = enabled) } }
    }

    fun setDnd(startHour: Int, endHour: Int) {
        viewModelScope.launch { preferences.update { it.copy(dndStartHour = startHour, dndEndHour = endHour) } }
    }
}
