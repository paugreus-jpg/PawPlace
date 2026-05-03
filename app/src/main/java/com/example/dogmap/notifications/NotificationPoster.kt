package com.example.dogmap.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.models.NotificationType
import com.example.dogmap.data.preferences.NotificationSettings
import java.util.Calendar

class NotificationPoster(private val context: Context) {

    companion object {
        const val CHANNEL_SOCIAL = "channel_social"
        const val CHANNEL_REMINDERS = "channel_reminders"
        const val CHANNEL_SYSTEM = "channel_system"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(CHANNEL_SOCIAL, "Social", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_REMINDERS, "Recordatorios de paseo", NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_SYSTEM, "Sistema", NotificationManager.IMPORTANCE_LOW)
        ).forEach { manager.createNotificationChannel(it) }
    }

    fun post(notification: AppNotification, settings: NotificationSettings) {
        if (!settings.globalEnabled) return

        val isSocial = notification.type in listOf(
            NotificationType.WALK_LIKE,
            NotificationType.WALK_TRENDING,
            NotificationType.LOCATION_COMMENT,
            NotificationType.NEW_FOLLOWER
        )
        val isReminder = notification.type == NotificationType.WALK_REMINDER

        if (isSocial && !settings.socialEnabled) return
        if (isReminder && !settings.remindersEnabled) return

        if (settings.dndStartHour >= 0 && settings.dndEndHour >= 0) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (isInDnd(hour, settings.dndStartHour, settings.dndEndHour)) return
        }

        val channelId = when {
            isSocial -> CHANNEL_SOCIAL
            isReminder -> CHANNEL_REMINDERS
            else -> CHANNEL_SYSTEM
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setAutoCancel(true)

        val canPost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canPost) {
            NotificationManagerCompat.from(context).notify(notification.id.hashCode(), builder.build())
        }
    }

    private fun isInDnd(currentHour: Int, start: Int, end: Int): Boolean =
        if (start <= end) currentHour in start until end
        else currentHour >= start || currentHour < end
}
