---
name: ui-specialist
description: >
  Creates and maintains Jetpack Compose UI for the Dogmap Android project using Material 3 design.
  Use this skill whenever you need to: create a new screen or Composable, style existing UI
  components, implement Material 3 theming (colors, typography, shapes), build forms for dog
  data entry, create list/grid views of dogs, design detail screens, add bottom sheets or dialogs,
  handle responsive layouts, or integrate Coil for image loading. Trigger when anyone mentions
  "screen", "UI", "Compose", "Material", "design", "layout", "button", "card", "form",
  "theme", "color", "typography", "image loading", "Coil", "Scaffold", "TopAppBar",
  or any visual component in the context of Dogmap. Always pair with android-resource-manager
  skill when referencing R resources.
---

# UI Specialist — Dogmap

## Tech Stack

- **Compose BOM:** 2024.12.01 (pin all Compose dependencies to this)
- **Material 3:** `androidx.compose.material3`
- **Navigation:** `androidx.navigation:navigation-compose:2.8.4`
- **Image loading:** Coil (`io.coil-kt:coil-compose`)
- **Maps:** `com.google.maps.android:maps-compose:6.2.1`

## File Conventions

- Screens go in `ui/screens/` — one file per screen (e.g., `DogListScreen.kt`, `DogDetailScreen.kt`)
- Reusable components go in `ui/components/` (e.g., `DogCard.kt`, `SearchBar.kt`)
- Theme files in `ui/theme/` (Color.kt, Theme.kt, Type.kt)
- Package: `com.example.dogmap.ui.screens` / `com.example.dogmap.ui.components`
- **R import:** Always `import com.dogmap.R` — see android-resource-manager skill

## Screen Template

Every new screen follows this pattern:

```kotlin
package com.example.dogmap.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import com.dogmap.R  // Always this, never com.example.dogmap.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(
    viewModel: DogViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Title") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Screen content here
        }
    }
}
```

Key points:
- Navigation callbacks as lambdas, not NavController directly — makes previews and testing easier.
- Always apply `paddingValues` from Scaffold before adding custom padding.
- Use `ExperimentalMaterial3Api` opt-in where needed (TopAppBar, etc.).

## Material 3 Component Patterns

### Cards for Dog Items
```kotlin
@Composable
fun DogCard(
    dog: Dog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = dog.imageUri ?: R.drawable.placeholder_dog,
                contentDescription = dog.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = dog.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = dog.breed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### Form Inputs
```kotlin
@Composable
fun DogFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    breed: String,
    onBreedChange: (String) -> Unit,
    // ... other fields
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Nombre del perro") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = breed,
        onValueChange = onBreedChange,
        label = { Text("Raza") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
```

### Dialogs
```kotlin
@Composable
fun DeleteConfirmationDialog(
    dogName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar perro") },
        text = { Text("¿Estás seguro de que quieres eliminar a $dogName?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
```

## Image Loading with Coil

```kotlin
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(dog.imageUri)
        .crossfade(true)
        .placeholder(R.drawable.placeholder_dog)
        .error(R.drawable.placeholder_dog)
        .build(),
    contentDescription = "Foto de ${dog.name}",
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clip(RoundedCornerShape(12.dp)),
    contentScale = ContentScale.Crop
)
```

## Theming Guidelines

Use semantic color roles from MaterialTheme, never hardcode colors:
- `MaterialTheme.colorScheme.primary` — main brand actions
- `MaterialTheme.colorScheme.secondary` — secondary elements
- `MaterialTheme.colorScheme.surface` / `surfaceVariant` — backgrounds
- `MaterialTheme.colorScheme.error` — destructive actions
- `MaterialTheme.typography.headlineMedium` — screen titles
- `MaterialTheme.typography.titleMedium` — card titles
- `MaterialTheme.typography.bodyMedium` — body text

## Loading and Empty States

Every screen that displays data needs both:

```kotlin
when {
    uiState.isLoading -> {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    uiState.dogs.isEmpty() -> {
        EmptyStateMessage(
            icon = Icons.Outlined.Pets,
            message = "No hay perros registrados",
            actionLabel = "Añadir perro",
            onAction = { /* navigate to add screen */ }
        )
    }
    else -> {
        // Normal content
    }
}
```

## Checklist for New Screens

1. [ ] File in `ui/screens/` with correct package declaration
2. [ ] `import com.dogmap.R` (not com.example.dogmap.R)
3. [ ] Receives ViewModel and navigation callbacks as parameters
4. [ ] Uses Scaffold with proper padding
5. [ ] Handles loading, empty, and error states
6. [ ] Uses Material 3 components and semantic colors
7. [ ] Added navigation route (see navigation-manager skill)
