package com.example.dogmap.notifications.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WalkReminderScheduler {

    fun schedule(context: Context, dogId: String, dogName: String, intervalHours: Long = 24) {
        val data = Data.Builder()
            .putString(WalkReminderWorker.KEY_DOG_NAME, dogName)
            .build()

        val request = PeriodicWorkRequestBuilder<WalkReminderWorker>(intervalHours, TimeUnit.HOURS)
            .setInputData(data)
            .addTag(tagFor(dogId))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            tagFor(dogId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context, dogId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(tagFor(dogId))
    }

    private fun tagFor(dogId: String) = "walk_reminder_$dogId"
}
