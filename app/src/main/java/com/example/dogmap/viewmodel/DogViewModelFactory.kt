package com.example.dogmap.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dogmap.data.preferences.NotificationPreferences
import com.example.dogmap.data.repository.DogRepository
import com.example.dogmap.data.repository.FavoritesRepository
import com.example.dogmap.data.repository.NotificationsRepository
import com.example.dogmap.data.repository.UserRepository
import com.example.dogmap.data.repository.WalkRepository

class DogViewModelFactory(
    private val application: Application,
    private val dogRepository: DogRepository,
    private val userRepository: UserRepository,
    private val favoritesRepository: FavoritesRepository,
    private val walkRepository: WalkRepository,
    private val notificationsRepository: NotificationsRepository,
    private val notificationPreferences: NotificationPreferences
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DogViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                DogViewModel(dogRepository, userRepository, favoritesRepository) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                AuthViewModel(userRepository) as T
            }
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                UserViewModel(userRepository, dogRepository, favoritesRepository) as T
            }
            modelClass.isAssignableFrom(WalkViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                WalkViewModel(walkRepository, userRepository) as T
            }
            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                NotificationsViewModel(notificationsRepository, notificationPreferences) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                @Suppress("UNCHECKED_CAST")
                SettingsViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
