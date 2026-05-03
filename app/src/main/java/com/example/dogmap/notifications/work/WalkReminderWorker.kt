package com.example.dogmap.notifications.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dogmap.DogMapApplication
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.models.NotificationType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.util.UUID

class WalkReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
        val dogName = inputData.getString(KEY_DOG_NAME) ?: "tu perro"

        val app = applicationContext as DogMapApplication
        val notification = AppNotification(
            id = UUID.randomUUID().toString(),
            type = NotificationType.WALK_REMINDER,
            title = "¡Hora del paseo!",
            body = "Es el momento de sacar a $dogName",
            createdAt = System.currentTimeMillis(),
            read = false,
            ownerUid = uid
        )

        app.notificationsRepository.pushLocal(notification)
        val settings = app.notificationPreferences.settingsFlow.first()
        app.notificationPoster.post(notification, settings)

        return Result.success()
    }

    companion object {
        const val KEY_DOG_NAME = "dogName"
    }
}
