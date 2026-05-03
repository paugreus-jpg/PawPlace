---
name: room-database-manager
description: >
  Manages Room database configuration, instantiation, dependency injection, and lifecycle for
  the Dogmap Android project. Use this skill when you need to: configure the Room database
  singleton, set up dependency injection for DAO/Repository/ViewModel, handle database creation
  in Application class, configure database builder options (fallback, pre-populate, callbacks),
  or debug database initialization issues. Trigger when anyone mentions "database setup",
  "Application class", "dependency injection", "singleton", "database instance", "ViewModelFactory",
  or "database initialization" in Dogmap. Also use when creating the app for the first time
  or restructuring how dependencies are provided.
---

# Room Database Manager — Dogmap

## Database Singleton

Room databases are expensive to create. Use a singleton pattern via the Application class:

```kotlin
package com.example.dogmap

import android.app.Application
import androidx.room.Room
import com.example.dogmap.data.repository.DogDatabase

class DogmapApplication : Application() {

    lateinit var database: DogDatabase
        private set

    val dogRepository by lazy {
        com.example.dogmap.data.repository.DogRepository(database.dogDao())
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            DogDatabase::class.java,
            "dogmap_database"
        )
        .fallbackToDestructiveMigration() // Only during development
        .build()
    }
}
```

Register in `AndroidManifest.xml`:
```xml
<application
    android:name=".DogmapApplication"
    ...>
```

## ViewModel Factory

Since the ViewModel needs the repository, provide it through a factory:

```kotlin
package com.example.dogmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dogmap.data.repository.DogRepository

class DogViewModelFactory(
    private val repository: DogRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

## Wiring in MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as DogmapApplication
        val viewModelFactory = DogViewModelFactory(app.dogRepository)
        val viewModel: DogViewModel by viewModels { viewModelFactory }

        setContent {
            DogmapTheme {
                val navController = rememberNavController()
                DogmapNavHost(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}
```

## Pre-populating the Database

To ship the app with sample dogs:

```kotlin
Room.databaseBuilder(applicationContext, DogDatabase::class.java, "dogmap_database")
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert sample data in a coroutine
            CoroutineScope(Dispatchers.IO).launch {
                database.dogDao().insertDog(
                    Dog(name = "Rex", breed = "Pastor Alemán", age = 3,
                        latitude = 43.3614, longitude = -5.8493)
                )
            }
        }
    })
    .build()
```

## Development vs. Production

| Setting | Development | Production |
|---------|------------|------------|
| `fallbackToDestructiveMigration()` | OK | NEVER — use migrations |
| `exportSchema` | `true` | `true` |
| Database name | `dogmap_database` | Same |

## Checklist

1. [ ] `DogmapApplication` registered in AndroidManifest `android:name`
2. [ ] Database built as singleton (lateinit + onCreate)
3. [ ] Repository created lazily from database DAO
4. [ ] ViewModelFactory provides repository to ViewModel
5. [ ] MainActivity uses `by viewModels { factory }` pattern
6. [ ] Development uses destructive migration; production uses proper migrations
