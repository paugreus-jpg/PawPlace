# Dogmap — Project Documentation (Abstract)

## Abstract

**Dogmap** is a native Android social application that allows dog owners to register, geolocate, and share information about dogs and dog-friendly places, as well as record and publish their walks in real time. The application combines an interactive map (Mapbox), an offline-first local database (Room), and a cloud backend (Firebase) to provide a hybrid local–remote experience with synchronization, social interaction, and push-style in-app notifications. It is built entirely with Jetpack Compose and Kotlin, following a Clean Architecture + MVVM pattern with unidirectional data flow and reactive state management based on Kotlin Coroutines and `StateFlow`.

The aim of the project is to demonstrate the end-to-end design and implementation of a modern Android product: from authentication and persistence to geolocation services, foreground tracking, image storage, social features, and a notification subsystem — all delivered through a cohesive Material 3 UI.

---

## 1. Project Overview

| Item | Value |
|------|-------|
| Platform | Android (native) |
| Language | Kotlin |
| UI Toolkit | Jetpack Compose + Material 3 |
| Min / Target / Compile SDK | per `build.gradle.kts` (`compileSdk` = 36) |
| Architecture | Clean Architecture + MVVM (UDF) |
| Local DB | Room 2.6.1 (version 6, destructive migration) |
| Remote backend | Firebase (Auth, Firestore, Storage) |
| Map SDK | Mapbox Maps SDK 11.8.0 + Maps Compose 11.8.0 |
| Image loading | Coil 2.7.0 |
| Async / Reactive | Kotlin Coroutines + Flow / StateFlow |
| Background work | WorkManager 2.9.1, Foreground Service |
| Persistence (settings) | DataStore Preferences |

---

## 2. Functional Scope

The application implements the following user-facing features:

### 2.1 Authentication and User Management
- Email/password sign-up and sign-in via **Firebase Authentication**.
- **Google Sign-In** via `play-services-auth`, integrated with Firebase Auth.
- First-launch profile setup flow (display name, city, pet info, photo).
- Editable user profile, account deletion, and persistent session.

### 2.2 Dog & Place Registration
- Create geolocated dog/place entries by tapping the map or filling a form.
- Each entry includes name, type, description, rating, photo, and a public/private flag.
- Edit and delete entries owned by the current user.
- Photo upload to **Firebase Storage**, displayed via Coil.

### 2.3 Interactive Map
- Real-time rendering of dog locations as Mapbox `PointAnnotation` markers.
- Custom camera, zoom, and tilt control; user-location overlay using `FusedLocationProviderClient`.
- Geofence circles (`CircleAnnotation`) and layer toggles.
- On-map filtering: **All / Personal / Community**.

### 2.4 Public List and Discovery
- Browse, search and filter publicly visible dogs.
- Tap-through navigation to detailed entries and to the author's public profile.

### 2.5 Social Layer
- **Likes** with live counts.
- **Comments** thread per dog/place.
- **Favorites** stored as a Firestore subcollection per user.
- **Public profiles** showing other users' dogs and favorite places.

### 2.6 Walk Recording
- Live GPS tracking via a **foreground service** (`WalkTrackingService`).
- Recording of distance, duration, and route as a list of `TrackPoint`s (lat, lon, timestamp), serialized into Room via a TypeConverter.
- Public walks can be browsed, replayed, and "followed" in real time by other users.
- Like and delete operations on walks.

### 2.7 Notification Subsystem
- In-app notification inbox synchronized from Firestore to Room.
- Notification types: `WALK_LIKE`, `WALK_TRENDING`, `LOCATION_COMMENT`, `NEW_FOLLOWER`, `WALK_REMINDER`, `SYSTEM`.
- Per-type and global toggles, with a Do-Not-Disturb time window stored in DataStore.
- System-level notifications via `NotificationManager`; walk reminders scheduled via WorkManager.

### 2.8 App Settings
- Dark / light theme toggle.
- Language selection (Spanish by default, forced on first launch via locale override).
- Notification preferences screen.

---

## 3. Architecture

The project follows **Clean Architecture** with three logical layers wired by **MVVM** and unidirectional data flow:

```
┌────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose)                                      │
│  Screens ──▶ collect StateFlow ──▶ render Material 3       │
│      │                                                     │
│      ▼ events / intents                                    │
│  ViewModel ──▶ exposes UiState (StateFlow)                 │
│      │                                                     │
│      ▼                                                     │
│  Repository (single source of truth)                       │
│      │                                                     │
│      ├──▶ Room DAO (local cache, offline-first)            │
│      └──▶ Firestore / Auth / Storage (remote sync)         │
└────────────────────────────────────────────────────────────┘
```

Data flows downward (DB/remote → repo → VM → UI) and events flow upward (UI → VM → repo). Repositories are the single source of truth and reconcile local Room state with Firestore listeners.

### 3.1 Critical Namespace Rule
The Gradle namespace (`com.dogmap`) and the source package (`com.example.dogmap`) intentionally diverge. Therefore every Kotlin file using generated resources must `import com.dogmap.R`. This is enforced at review time and documented in `CLAUDE.md`.

---

## 4. Module and Package Layout

```
app/src/main/java/com/example/dogmap/
├── DogMapApplication.kt          # Application class, DI bootstrapping
├── MainActivity.kt
├── data/
│   ├── db/                       # DogDatabase.kt, DogDao, WalkDao, NotificationDao, DogTypeConverters
│   ├── models/                   # Dog, Walk, TrackPoint, AppNotification, User, Comment, Favorite, NotificationType
│   ├── preferences/              # NotificationPreferences (DataStore)
│   └── repository/               # DogRepository, WalkRepository, NotificationsRepository, UserRepository, FavoritesRepository
├── notifications/
│   ├── NotificationPoster.kt
│   └── work/                     # WalkReminderWorker, WalkReminderScheduler
├── service/WalkTrackingService.kt
├── ui/
│   ├── components/               # DogCard, UserAvatar, GlassUi, GlassPlaceSheet, RatingBar, PawPlaceLogo
│   ├── map/                      # EmojiMarkerFactory
│   ├── navigation/               # Route.kt, NavGraph.kt
│   ├── notifications/            # NotificationsScreen, NotificationsSettingsScreen, NotificationItem
│   ├── screens/                  # All Composable screens (18+)
│   └── theme/                    # Color.kt, Typography.kt, Theme.kt
└── viewmodel/                    # DogViewModel, WalkViewModel, UserViewModel, AuthViewModel,
                                  # NotificationsViewModel, SettingsViewModel, DogViewModelFactory, ViewModelProvider
```

---

## 5. Data Model (Room)

### 5.1 Entities

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| `Dog` | Geolocated dog/place | `id (PK)`, `name`, `description`, `type`, `rating`, `latitude`, `longitude`, `imageUrl`, `isPublic`, `isPlace`, `remoteId`, `authorId`, `authorName`, `authorPhotoUrl`, `likesCount`, `createdAt` |
| `Walk` | Recorded walk | `id (PK)`, `name`, `description`, `dogId`, `dogName`, `authorId`, `authorName`, `authorPhotoUrl`, `isPublic`, `likesCount`, `distanceMeters`, `durationSeconds`, `points: List<TrackPoint>`, `remoteId`, `createdAt` |
| `AppNotification` | Local notification mirror | `id (PK)`, `type`, `title`, `body`, `payloadJson`, `createdAt`, `read`, `ownerUid` |
| `TrackPoint` | GPS sample (embedded) | `lat`, `lon`, `timestamp` |

Firestore-only models: `User`, `Comment`, `Favorite`, and the `NotificationType` enum.

### 5.2 DAOs (selection)
- **DogDao** — `getAllDogs(): Flow<List<Dog>>`, `insertDog`, `updateDog`, `deleteDog`, `updateLikesCount`, `getPublicDogs`, `getDogsByAuthor`.
- **WalkDao** — `getAllWalks`, `getWalksByAuthor`, `getWalkByRemoteId`, CRUD operations.
- **NotificationDao** — `getAllForUser`, `unreadCount: Flow<Int>`, `markRead`, `markAllRead`, `deleteOlderThan`.

### 5.3 Repositories
`DogRepository`, `WalkRepository`, `NotificationsRepository`, `UserRepository`, and `FavoritesRepository` orchestrate Room ↔ Firestore synchronization, expose `Flow`/`StateFlow` streams, and contain all business rules (likes, public toggles, comments, favorites).

---

## 6. Presentation Layer

### 6.1 ViewModels

| ViewModel | Responsibility |
|-----------|----------------|
| `DogViewModel` | All dog-related state (lists, filters, likes, comments, current user). |
| `WalkViewModel` | Public/personal walks, walk detail, like/delete. |
| `UserViewModel` | Profile state (`ProfileUiState`), edit/save/delete account. |
| `AuthViewModel` | Login/registration form state and validation. |
| `NotificationsViewModel` | Notifications list, unread count, settings (DnD, toggles). |
| `SettingsViewModel` | Theme and language preferences (SharedPreferences). |

ViewModels are instantiated via a centralized `DogViewModelFactory` constructed in the `Application` class and exposed through a `CompositionLocal`.

### 6.2 Navigation

The app uses **Navigation Compose 2.8.4** with a single `NavHost` defined in `ui/navigation/NavGraph.kt`. Routes are declared as type-safe constants in `Route.kt`. Highlights:

- Auth flow: `splash → login → register → profileSetup`.
- Main shell: `map`, `list`, `walks` (bottom navigation).
- Detail and creation: `add/{lat}/{lon}`, `placeDetail/{remoteId}`, `mapFocus/{lat}/{lon}`.
- Walks: `walkRecord`, `walkDetail/{remoteId}`, `walkFollow/{remoteId}`.
- Profile: `profile`, `publicProfile/{uid}`, `settings`.
- Notifications: `notifications`, `notifications/settings`.

### 6.3 UI Screens
The application contains 18+ Compose screens organized under `ui/screens/` and `ui/notifications/`, all themed via Material 3 and built with Composables for `Scaffold`, `TopAppBar`, `BottomBar`, lists, dialogs and bottom sheets. Image loading is delegated to Coil.

---

## 7. Map and Geolocation

- **Mapbox Maps SDK 11.8.0** with the official Compose extension (`com.mapbox.extension:maps-compose`) is used for all map rendering.
- **Public token** is read from `local.properties` (`MAPBOX_PUBLIC_TOKEN`), exposed through `BuildConfig`, and applied at startup with `MapboxOptions.accessToken` inside `DogMapApplication.onCreate()`.
- **Downloads token** (`MAPBOX_DOWNLOADS_TOKEN`, scope `DOWNLOADS:READ`) is required only at build time to fetch the SDK from Mapbox's Maven repo and is **not** versioned.
- **User location** is obtained via `FusedLocationProviderClient` from Google Play Services Location 21.3.0.
- **Walk tracking** runs in `WalkTrackingService`, a foreground service of type `location`, which periodically samples the user's position and persists points in memory until the walk is saved.

---

## 8. Backend Integration (Firebase)

- **Authentication** — email/password provider; auth state is observed application-wide to start/stop notification synchronization.
- **Firestore collections** — `dogs`, `walks`, `users`, plus `users/{uid}/favorites` and `users/{uid}/notifications` subcollections.
- **Storage** — buckets for dog photos and user avatars; URLs are persisted in the corresponding entities.
- **Sync strategy** — `snapshotListener`s in repositories stream remote changes into Room so the UI keeps reading from a single offline-capable source.

---

## 9. Permissions

Declared in `AndroidManifest.xml`:

- `INTERNET`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`
- `POST_NOTIFICATIONS`

Runtime requests are handled via Accompanist Permissions on the relevant screens (Map, Walk recording, Notifications on Android 13+).

---

## 10. Build, Tooling, and Conventions

- Build system: **Gradle Kotlin DSL**.
- Versions are pinned in `app/build.gradle.kts` and must not be changed without explicit reason (see `CLAUDE.md`).
- Pre-commit checks:
  1. `./gradlew :app:compileDebugKotlin` must succeed.
  2. No file may import `com.example.dogmap.R`; only `com.dogmap.R`.
  3. New screens must be wired into `NavGraph`.
  4. New DB fields must propagate through DAO → Repository → ViewModel → UI.
- Testing stack: JUnit, Mockk, Espresso, Room Testing, Coroutines Test.

---

## 11. Project Status

The application is feature-complete for delivery. All core flows — authentication, dog/place registration, map visualization, walk tracking, social interaction, and notifications — are implemented end to end. Remaining items are minor polish tasks documented in `plans/`. The codebase is organized for maintainability, with a documented skill/agent model (`.claude/skills/`) describing the specialized responsibilities used during development.

---

## 13. Conclusions

Dogmap demonstrates that a fully-featured, production-grade Android application can be built entirely on the modern Jetpack + Firebase + Mapbox stack without compromising on architecture or maintainability.

The key engineering decisions that shaped the project:

- **Offline-first via Room** ensures the app remains functional with no network connection, while Firestore `snapshotListener`s keep data eventually consistent without any manual polling logic.
- **Single source of truth in the Repository layer** means ViewModels never talk directly to Firebase or Room — all data access is funnelled through a consistent API, making each layer independently testable.
- **Foreground service for GPS tracking** was the only viable approach for continuous walk recording on modern Android; the trade-off (a persistent system notification) is an Android platform constraint, not a design choice.
- **Mapbox over Google Maps** was chosen for its richer annotation API and the ability to render custom emoji markers via `EmojiMarkerFactory` without relying on bitmap pre-rendering.
- **The namespace split (`com.dogmap` vs `com.example.dogmap`)** is an intentional Gradle configuration that separates the application identity from the source package, requiring the explicit `import com.dogmap.R` rule enforced at every commit.
- **WorkManager for reminders** provides guaranteed execution even after process death, which is essential for walk reminder notifications that must fire at a scheduled time regardless of app state.

The result is a codebase that scales cleanly: adding a new feature means adding a model, a DAO query, a repository method, a ViewModel state property, and a Compose screen — each step isolated and independently verifiable. The documented skill model (`.claude/skills/`) reflects this separation, assigning each concern to a specialized agent so that AI-assisted development remains precise and does not accidentally cross architectural boundaries.

---

## 12. Glossary

- **Dog / Place** — A geolocated point of interest associated with a dog or dog-friendly location.
- **Walk** — A recorded GPS route with metadata (distance, duration, author).
- **TrackPoint** — A single `(lat, lon, timestamp)` sample inside a walk.
- **Public / Private** — Visibility flag controlling whether other users can see an entry.
- **Foreground Service** — Android service running with a persistent notification, required for continuous GPS sampling.
- **UDF** — Unidirectional Data Flow: state goes down, events go up.
