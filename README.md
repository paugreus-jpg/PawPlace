# PawPlace

An Android app for registering, geolocating, and sharing dogs and dog-friendly places. Users can pin dog locations on an interactive map, record walks with GPS tracking, and connect with a social layer built on Firebase.

## Features

- **Interactive Map** — Mapbox-powered map with dog markers, user location, and geofenced areas
- **Dog & Place Registry** — Add, edit, and browse dogs and dog-friendly spots
- **Walk Tracking** — Record GPS walks as a foreground service, replay them, and follow others in real time
- **Social Layer** — Likes, comments, favorites, and public profiles
- **Notifications** — In-app inbox with per-type toggles and Do Not Disturb settings
- **Offline-first** — Room cache with Firestore sync; works without a connection
- **Auth** — Firebase Authentication (email/password)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.8.4 |
| Maps | Mapbox Maps SDK 11.8.0 + Compose extension |
| Database | Room 2.6.1 |
| Backend | Firebase Auth, Firestore, Storage |
| State | StateFlow / MVVM |
| Images | Coil 2.7.0 |
| Background | WorkManager + Foreground Service |
| Min SDK | 24 (Android 7.0) |

## Architecture

Clean Architecture + MVVM with unidirectional data flow:

```
Firestore / Room  →  Repository  →  ViewModel (StateFlow)  →  Compose UI
                                          ↑
                                      User events
```

Repositories are the single source of truth: they stream Firestore snapshots into a local Room cache and expose `Flow` to ViewModels.

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- A Mapbox account with a public token (`pk.xxx`) and a secret downloads token (`sk.xxx`)
- A Firebase project with Auth, Firestore, and Storage enabled

### Configuration

1. Clone the repo:
   ```bash
   git clone https://github.com/paugreus-jpg/PawPlace.git
   cd PawPlace
   ```

2. Add your Mapbox tokens to `local.properties` (not committed):
   ```properties
   MAPBOX_PUBLIC_TOKEN=pk.xxxxx
   MAPBOX_DOWNLOADS_TOKEN=sk.xxxxx
   ```

3. Place your `google-services.json` from the Firebase console into `app/`.

4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

## Project Structure

```
app/src/main/java/com/example/dogmap/
├── data/
│   ├── db/          # Room entities, DAOs, database singleton
│   └── repository/  # DogRepository, WalkRepository, UserRepository, …
├── ui/
│   ├── screens/     # All Compose screens
│   ├── components/  # Reusable composables
│   └── theme/       # Material 3 colors, typography, shapes
├── viewmodel/       # ViewModels + UiState data classes
├── service/         # WalkTrackingService (foreground GPS)
└── DogMapApplication.kt
```

> **Note on namespace:** The Gradle namespace is `com.dogmap` while the source package is `com.example.dogmap`. All files that reference `R` must import `com.dogmap.R`.

## License

MIT — see [LICENSE](LICENSE).
