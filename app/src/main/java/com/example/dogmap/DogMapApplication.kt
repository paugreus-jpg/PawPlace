package com.example.dogmap

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dogmap.BuildConfig
import com.example.dogmap.data.db.DogDatabase
import com.example.dogmap.data.preferences.NotificationPreferences
import com.example.dogmap.data.repository.DogRepository
import com.example.dogmap.data.repository.FavoritesRepository
import com.example.dogmap.data.repository.NotificationsRepository
import com.example.dogmap.data.repository.UserRepository
import com.example.dogmap.data.repository.WalkRepository
import com.example.dogmap.notifications.NotificationPoster
import com.example.dogmap.viewmodel.DogViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.common.MapboxOptions

class DogMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Force Spanish as default on first launch regardless of device locale
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        }

        // Apply saved night mode before any Activity is created to avoid calling
        // setDefaultNightMode during Compose composition (which can trigger a spurious recreate)
        val prefs = getSharedPreferences("pawplace_settings", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", true)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        MapboxOptions.accessToken = BuildConfig.MAPBOX_PUBLIC_TOKEN

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) notificationsRepository.startSync(uid)
            else notificationsRepository.stopSync()
        }
    }

    private val database by lazy { DogDatabase.getDatabase(this) }

    val dogRepository by lazy { DogRepository(database.dogDao()) }
    val userRepository by lazy { UserRepository() }
    val favoritesRepository by lazy { FavoritesRepository() }
    val walkRepository by lazy { WalkRepository(database.walkDao()) }
    val notificationsRepository by lazy { NotificationsRepository(database.notificationDao()) }
    val notificationPreferences by lazy { NotificationPreferences(this) }
    val notificationPoster by lazy { NotificationPoster(this) }

    val viewModelFactory by lazy {
        DogViewModelFactory(
            this,
            dogRepository,
            userRepository,
            favoritesRepository,
            walkRepository,
            notificationsRepository,
            notificationPreferences
        )
    }
}
