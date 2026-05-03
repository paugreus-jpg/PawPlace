package com.example.dogmap.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dogmap.data.models.AppNotification
import com.example.dogmap.data.models.Dog
import com.example.dogmap.data.models.Walk

@Database(entities = [Dog::class, Walk::class, AppNotification::class], version = 6, exportSchema = false)
@TypeConverters(DogTypeConverters::class)
abstract class DogDatabase : RoomDatabase() {
    abstract fun dogDao(): DogDao
    abstract fun walkDao(): WalkDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: DogDatabase? = null

        fun getDatabase(context: Context): DogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DogDatabase::class.java,
                    "dog_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
