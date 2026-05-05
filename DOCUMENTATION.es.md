# Dogmap — Documentación del Proyecto (Resumen)

## Resumen

**Dogmap** es una aplicación Android nativa de carácter social que permite a los dueños de perros registrar, geolocalizar y compartir información sobre perros y lugares dog-friendly, así como grabar y publicar sus paseos en tiempo real. La aplicación combina un mapa interactivo (Mapbox), una base de datos local offline-first (Room) y un backend en la nube (Firebase) para ofrecer una experiencia híbrida local–remota con sincronización, interacción social y notificaciones in-app de tipo push. Está construida íntegramente con Jetpack Compose y Kotlin, siguiendo un patrón Clean Architecture + MVVM con flujo de datos unidireccional y gestión reactiva del estado basada en Kotlin Coroutines y `StateFlow`.

El objetivo del proyecto es demostrar el diseño e implementación end-to-end de un producto Android moderno: desde la autenticación y la persistencia hasta los servicios de geolocalización, el seguimiento en primer plano, el almacenamiento de imágenes, las funciones sociales y un subsistema de notificaciones, todo ello entregado a través de una UI cohesionada con Material 3.

---

## 1. Visión General del Proyecto

| Elemento | Valor |
|----------|-------|
| Plataforma | Android (nativa) |
| Lenguaje | Kotlin |
| UI Toolkit | Jetpack Compose + Material 3 |
| Min / Target / Compile SDK | según `build.gradle.kts` (`compileSdk` = 36) |
| Arquitectura | Clean Architecture + MVVM (UDF) |
| BD local | Room 2.6.1 (versión 6, migración destructiva) |
| Backend remoto | Firebase (Auth, Firestore, Storage) |
| SDK de mapas | Mapbox Maps SDK 11.8.0 + Maps Compose 11.8.0 |
| Carga de imágenes | Coil 2.7.0 |
| Async / Reactivo | Kotlin Coroutines + Flow / StateFlow |
| Trabajo en background | WorkManager 2.9.1, Foreground Service |
| Persistencia (ajustes) | DataStore Preferences |

---

## 2. Alcance Funcional

La aplicación implementa las siguientes funcionalidades orientadas al usuario:

### 2.1 Autenticación y Gestión de Usuarios
- Registro e inicio de sesión con email/contraseña mediante **Firebase Authentication**.
- **Google Sign-In** via `play-services-auth`, integrado con Firebase Auth.
- Flujo de configuración de perfil en el primer inicio (nombre, ciudad, info de mascota, foto).
- Perfil de usuario editable, eliminación de cuenta y sesión persistente.

### 2.2 Registro de Perros y Lugares
- Crear entradas geolocalizadas de perros/lugares tocando el mapa o rellenando un formulario.
- Cada entrada incluye nombre, tipo, descripción, valoración, foto y flag público/privado.
- Editar y eliminar entradas propias del usuario.
- Subida de fotos a **Firebase Storage**, mostradas mediante Coil.

### 2.3 Mapa Interactivo
- Renderizado en tiempo real de ubicaciones de perros como marcadores `PointAnnotation` de Mapbox.
- Control personalizado de cámara, zoom e inclinación; overlay de ubicación del usuario mediante `FusedLocationProviderClient`.
- Círculos de geovalla (`CircleAnnotation`) y toggles de capas.
- Filtrado en el mapa: **Todos / Personales / Comunidad**.

### 2.4 Lista Pública y Descubrimiento
- Explorar, buscar y filtrar perros visibles públicamente.
- Navegación a entradas detalladas y al perfil público del autor.

### 2.5 Capa Social
- **Likes** con contadores en tiempo real.
- Hilo de **comentarios** por perro/lugar.
- **Favoritos** almacenados como subcolección de Firestore por usuario.
- **Perfiles públicos** que muestran los perros y lugares favoritos de otros usuarios.

### 2.6 Grabación de Paseos
- Seguimiento GPS en vivo mediante un **servicio en primer plano** (`WalkTrackingService`).
- Grabación de distancia, duración y ruta como lista de `TrackPoint`s (lat, lon, timestamp), serializados en Room mediante un TypeConverter.
- Los paseos públicos pueden explorarse, reproducirse y ser "seguidos" en tiempo real por otros usuarios.
- Operaciones de like y eliminación en paseos.

### 2.7 Subsistema de Notificaciones
- Bandeja de notificaciones in-app sincronizada de Firestore a Room.
- Tipos de notificación: `WALK_LIKE`, `WALK_TRENDING`, `LOCATION_COMMENT`, `NEW_FOLLOWER`, `WALK_REMINDER`, `SYSTEM`.
- Toggles globales y por tipo, con ventana de No Molestar almacenada en DataStore.
- Notificaciones a nivel de sistema mediante `NotificationManager`; recordatorios de paseo programados con WorkManager.

### 2.8 Ajustes de la App
- Toggle de tema oscuro / claro.
- Selección de idioma (español por defecto, forzado en el primer inicio mediante override de locale).
- Pantalla de preferencias de notificaciones.

---

## 3. Arquitectura

El proyecto sigue **Clean Architecture** con tres capas lógicas conectadas por **MVVM** y flujo de datos unidireccional:

```
┌────────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose)                                      │
│  Screens ──▶ recolectan StateFlow ──▶ renderizan Material 3│
│      │                                                     │
│      ▼ eventos / intents                                   │
│  ViewModel ──▶ expone UiState (StateFlow)                  │
│      │                                                     │
│      ▼                                                     │
│  Repository (fuente única de verdad)                       │
│      │                                                     │
│      ├──▶ Room DAO (caché local, offline-first)            │
│      └──▶ Firestore / Auth / Storage (sync remota)         │
└────────────────────────────────────────────────────────────┘
```

Los datos fluyen hacia abajo (BD/remoto → repo → VM → UI) y los eventos fluyen hacia arriba (UI → VM → repo). Los repositorios son la fuente única de verdad y reconcilian el estado local de Room con los listeners de Firestore.

### 3.1 Regla Crítica del Namespace
El namespace de Gradle (`com.dogmap`) y el paquete fuente (`com.example.dogmap`) divergen intencionadamente. Por ello, cada archivo Kotlin que use recursos generados debe hacer `import com.dogmap.R`. Esto se verifica en la revisión y está documentado en `CLAUDE.md`.

---

## 4. Estructura de Módulos y Paquetes

```
app/src/main/java/com/example/dogmap/
├── DogMapApplication.kt          # Clase Application, bootstrapping de DI
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
│   ├── screens/                  # Todas las pantallas Composable (18+)
│   └── theme/                    # Color.kt, Typography.kt, Theme.kt
└── viewmodel/                    # DogViewModel, WalkViewModel, UserViewModel, AuthViewModel,
                                  # NotificationsViewModel, SettingsViewModel, DogViewModelFactory, ViewModelProvider
```

---

## 5. Modelo de Datos (Room)

### 5.1 Entidades

| Entidad | Propósito | Campos Clave |
|---------|-----------|--------------|
| `Dog` | Perro/lugar geolocalizado | `id (PK)`, `name`, `description`, `type`, `rating`, `latitude`, `longitude`, `imageUrl`, `isPublic`, `isPlace`, `remoteId`, `authorId`, `authorName`, `authorPhotoUrl`, `likesCount`, `createdAt` |
| `Walk` | Paseo grabado | `id (PK)`, `name`, `description`, `dogId`, `dogName`, `authorId`, `authorName`, `authorPhotoUrl`, `isPublic`, `likesCount`, `distanceMeters`, `durationSeconds`, `points: List<TrackPoint>`, `remoteId`, `createdAt` |
| `AppNotification` | Espejo local de notificación | `id (PK)`, `type`, `title`, `body`, `payloadJson`, `createdAt`, `read`, `ownerUid` |
| `TrackPoint` | Muestra GPS (embebida) | `lat`, `lon`, `timestamp` |

Modelos solo en Firestore: `User`, `Comment`, `Favorite` y el enum `NotificationType`.

### 5.2 DAOs (selección)
- **DogDao** — `getAllDogs(): Flow<List<Dog>>`, `insertDog`, `updateDog`, `deleteDog`, `updateLikesCount`, `getPublicDogs`, `getDogsByAuthor`.
- **WalkDao** — `getAllWalks`, `getWalksByAuthor`, `getWalkByRemoteId`, operaciones CRUD.
- **NotificationDao** — `getAllForUser`, `unreadCount: Flow<Int>`, `markRead`, `markAllRead`, `deleteOlderThan`.

### 5.3 Repositorios
`DogRepository`, `WalkRepository`, `NotificationsRepository`, `UserRepository` y `FavoritesRepository` orquestan la sincronización Room ↔ Firestore, exponen streams `Flow`/`StateFlow` y contienen todas las reglas de negocio (likes, toggles de visibilidad, comentarios, favoritos).

---

## 6. Capa de Presentación

### 6.1 ViewModels

| ViewModel | Responsabilidad |
|-----------|-----------------|
| `DogViewModel` | Todo el estado relacionado con perros (listas, filtros, likes, comentarios, usuario actual). |
| `WalkViewModel` | Paseos públicos/personales, detalle de paseo, like/eliminar. |
| `UserViewModel` | Estado del perfil (`ProfileUiState`), editar/guardar/eliminar cuenta. |
| `AuthViewModel` | Estado del formulario de login/registro y validación. |
| `NotificationsViewModel` | Lista de notificaciones, contador de no leídas, ajustes (No Molestar, toggles). |
| `SettingsViewModel` | Preferencias de tema e idioma (SharedPreferences). |

Los ViewModels se instancian mediante un `DogViewModelFactory` centralizado construido en la clase `Application` y expuesto a través de un `CompositionLocal`.

### 6.2 Navegación

La app usa **Navigation Compose 2.8.4** con un único `NavHost` definido en `ui/navigation/NavGraph.kt`. Las rutas se declaran como constantes type-safe en `Route.kt`. Puntos destacados:

- Flujo de auth: `splash → login → register → profileSetup`.
- Shell principal: `map`, `list`, `walks` (navegación inferior).
- Detalle y creación: `add/{lat}/{lon}`, `placeDetail/{remoteId}`, `mapFocus/{lat}/{lon}`.
- Paseos: `walkRecord`, `walkDetail/{remoteId}`, `walkFollow/{remoteId}`.
- Perfil: `profile`, `publicProfile/{uid}`, `settings`.
- Notificaciones: `notifications`, `notifications/settings`.

### 6.3 Pantallas UI
La aplicación contiene más de 18 pantallas Compose organizadas bajo `ui/screens/` y `ui/notifications/`, todas temadas con Material 3 y construidas con Composables para `Scaffold`, `TopAppBar`, `BottomBar`, listas, diálogos y bottom sheets. La carga de imágenes se delega a Coil.

---

## 7. Mapa y Geolocalización

- **Mapbox Maps SDK 11.8.0** con la extensión oficial de Compose (`com.mapbox.extension:maps-compose`) se usa para todo el renderizado del mapa.
- El **token público** se lee de `local.properties` (`MAPBOX_PUBLIC_TOKEN`), se expone mediante `BuildConfig` y se aplica al inicio con `MapboxOptions.accessToken` dentro de `DogMapApplication.onCreate()`.
- El **token de descarga** (`MAPBOX_DOWNLOADS_TOKEN`, scope `DOWNLOADS:READ`) solo es necesario en tiempo de build para descargar el SDK del repositorio Maven de Mapbox y **no** se versiona.
- La **ubicación del usuario** se obtiene mediante `FusedLocationProviderClient` de Google Play Services Location 21.3.0.
- El **seguimiento de paseos** se ejecuta en `WalkTrackingService`, un servicio en primer plano de tipo `location` que muestrea periódicamente la posición del usuario y persiste los puntos en memoria hasta que se guarda el paseo.

---

## 8. Integración con el Backend (Firebase)

- **Authentication** — proveedor de email/contraseña y Google Sign-In; el estado de auth se observa a nivel de aplicación para iniciar/detener la sincronización de notificaciones.
- **Colecciones de Firestore** — `dogs`, `walks`, `users`, más las subcolecciones `users/{uid}/favorites` y `users/{uid}/notifications`.
- **Storage** — buckets para fotos de perros y avatares de usuario; las URLs se persisten en las entidades correspondientes.
- **Estrategia de sincronización** — los `snapshotListener`s en los repositorios transmiten los cambios remotos a Room para que la UI lea siempre desde una única fuente offline-capable.

---

## 9. Permisos

Declarados en `AndroidManifest.xml`:

- `INTERNET`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`
- `POST_NOTIFICATIONS`

Las solicitudes en tiempo de ejecución se gestionan mediante Accompanist Permissions en las pantallas correspondientes (Mapa, Grabación de paseos, Notificaciones en Android 13+).

---

## 10. Build, Herramientas y Convenciones

- Sistema de build: **Gradle Kotlin DSL**.
- Las versiones están fijadas en `app/build.gradle.kts` y no deben cambiarse sin motivo explícito (ver `CLAUDE.md`).
- Verificaciones antes de cada commit:
  1. `./gradlew :app:compileDebugKotlin` debe completarse sin errores.
  2. Ningún archivo puede importar `com.example.dogmap.R`; solo `com.dogmap.R`.
  3. Las nuevas pantallas deben estar conectadas en `NavGraph`.
  4. Los nuevos campos de BD deben propagarse por DAO → Repository → ViewModel → UI.
- Stack de tests: JUnit, Mockk, Espresso, Room Testing, Coroutines Test.

---

## 11. Estado del Proyecto

La aplicación está completa funcionalmente para su entrega. Todos los flujos principales —autenticación, registro de perros/lugares, visualización en el mapa, grabación de paseos, interacción social y notificaciones— están implementados de extremo a extremo. Los elementos pendientes son tareas menores de pulido documentadas en `plans/`. El código está organizado para facilitar el mantenimiento, con un modelo de skills/agentes documentado (`.claude/skills/`) que describe las responsabilidades especializadas utilizadas durante el desarrollo.

---

## 12. Glosario

- **Perro / Lugar** — Un punto de interés geolocalizado asociado a un perro o a un lugar dog-friendly.
- **Paseo** — Una ruta GPS grabada con metadatos (distancia, duración, autor).
- **TrackPoint** — Una muestra individual `(lat, lon, timestamp)` dentro de un paseo.
- **Público / Privado** — Flag de visibilidad que controla si otros usuarios pueden ver una entrada.
- **Foreground Service** — Servicio Android que se ejecuta con una notificación persistente, necesario para el muestreo continuo de GPS.
- **UDF** — Unidirectional Data Flow (Flujo de Datos Unidireccional): el estado baja, los eventos suben.
