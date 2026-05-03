---
name: data-architect
description: >
  Manages the Room database layer for the Dogmap Android project, including entities, DAOs,
  the repository, and database migrations. Use this skill whenever you need to: add or modify
  fields in the Dog entity, create new database entities or tables, write or update DAO queries,
  modify the DogRepository, handle database migrations or version bumps, add new CRUD operations,
  debug Room compilation errors, or work with TypeConverters. Trigger when anyone mentions
  "database", "Room", "entity", "DAO", "migration", "table", "query", "insert", "delete",
  "update", "repository", or "Dog.kt" in the context of Dogmap. Also trigger when a new feature
  requires persisting data locally.
---

# Data Architect — Dogmap

## Room Stack Overview

```
Dog.kt (Entity) → DogDao.kt (Queries) → DogRepository.kt (Abstraction) → DogViewModel.kt (Consumer)
```

Every change to the data layer must propagate through this entire chain. If you add a field to `Dog.kt`, you MUST also check and update DAO, Repository, and ViewModel.

## Entity Pattern — `Dog.kt`

Located at `data/models/Dog.kt`:

```kotlin
package com.example.dogmap.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dogs")
data class Dog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val breed: String,
    val age: Int,
    val latitude: Double,
    val longitude: Double,
    val imageUri: String? = null,
    val description: String? = null
    // When adding new fields, always provide a default value
)
```

### Rules for Modifying Entities

1. **New nullable fields** should default to `null` — this avoids breaking existing data.
2. **New non-null fields** require a database migration (see Migrations section).
3. **Never rename a column** without a migration — Room maps by column name.
4. **Complex types** (List, Date, custom objects) need a `@TypeConverter`. See the TypeConverters section below.

## DAO Pattern — `DogDao.kt`

Located at `data/repository/DogDao.kt` (or `data/dao/`):

```kotlin
package com.example.dogmap.data.repository

import androidx.room.*
import com.example.dogmap.data.models.Dog
import kotlinx.coroutines.flow.Flow

@Dao
interface DogDao {
    @Query("SELECT * FROM dogs ORDER BY name ASC")
    fun getAllDogs(): Flow<List<Dog>>

    @Query("SELECT * FROM dogs WHERE id = :id")
    suspend fun getDogById(id: Int): Dog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDog(dog: Dog): Long

    @Update
    suspend fun updateDog(dog: Dog)

    @Delete
    suspend fun deleteDog(dog: Dog)

    @Query("SELECT COUNT(*) FROM dogs")
    fun getDogCount(): Flow<Int>
}
```

### DAO Conventions

- **Read queries** that the UI observes → return `Flow<T>` (not suspend).
- **Read queries** for one-shot lookups → `suspend fun` returning nullable type.
- **Write operations** (insert, update, delete) → always `suspend fun`.
- Use `OnConflictStrategy.REPLACE` for upsert behavior on inserts.
- Geo queries — for proximity searches use:
  ```kotlin
  @Query("""
      SELECT * FROM dogs 
      WHERE latitude BETWEEN :minLat AND :maxLat 
      AND longitude BETWEEN :minLng AND :maxLng
  """)
  fun getDogsInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<Dog>>
  ```

## Repository Pattern — `DogRepository.kt`

The repository is a thin wrapper providing a clean API and the single source of truth for data operations:

```kotlin
package com.example.dogmap.data.repository

import com.example.dogmap.data.models.Dog
import kotlinx.coroutines.flow.Flow

class DogRepository(private val dogDao: DogDao) {

    fun getAllDogs(): Flow<List<Dog>> = dogDao.getAllDogs()

    suspend fun getDogById(id: Int): Dog? = dogDao.getDogById(id)

    suspend fun insertDog(dog: Dog): Long = dogDao.insertDog(dog)

    suspend fun updateDog(dog: Dog) = dogDao.updateDog(dog)

    suspend fun deleteDog(dog: Dog) = dogDao.deleteDog(dog)

    fun getDogCount(): Flow<Int> = dogDao.getDogCount()
}
```

When adding a new query, the flow is always: DAO method → Repository method → ViewModel function.

## Database Class

```kotlin
@Database(entities = [Dog::class], version = 1, exportSchema = true)
abstract class DogDatabase : RoomDatabase() {
    abstract fun dogDao(): DogDao
}
```

Set `exportSchema = true` so Room generates JSON schema files — useful for migration validation.

## Migrations

When you add a non-null field without a default, or rename/remove columns, you need a migration:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE dogs ADD COLUMN weight REAL NOT NULL DEFAULT 0.0")
    }
}
```

Register it when building the database:
```kotlin
Room.databaseBuilder(context, DogDatabase::class.java, "dog_database")
    .addMigrations(MIGRATION_1_2)
    .build()
```

**Tip:** For nullable fields with a default of `null`, you can sometimes avoid a migration by using `fallbackToDestructiveMigration()` during development — but never in production.

## TypeConverters

For complex types stored in Room:

```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromStringList(value: String?): List<String> =
        value?.split(",")?.map { it.trim() } ?: emptyList()

    @TypeConverter
    fun toStringList(list: List<String>): String = list.joinToString(",")
}
```

Register with `@TypeConverters(Converters::class)` on the Database class.

## Propagation Checklist

When modifying the data layer:

1. [ ] Updated `Dog.kt` entity (new field with default)
2. [ ] Added/updated DAO query in `DogDao.kt`
3. [ ] Mirrored the new method in `DogRepository.kt`
4. [ ] Added the corresponding function in `DogViewModel.kt` (see mvvm-flow-handler skill)
5. [ ] If new non-null field without default → created migration
6. [ ] If complex type → added TypeConverter
7. [ ] Ran `./gradlew :app:compileDebugKotlin` to verify Room annotation processing
