---
name: mvvm-flow-handler
description: >
  Handles MVVM state management patterns for the Dogmap Android project using StateFlow, UiState
  data classes, and Room Flow integration. Use this skill whenever you need to: add or modify
  ViewModel logic, create new state properties in DogUiState, expose Room database queries as
  observable state, handle user events or actions in the ViewModel, connect UI state to Compose
  screens, debug state not updating in the UI, or any time someone mentions "ViewModel", "state",
  "StateFlow", "UiState", "MutableStateFlow", or "event handling" in the context of Dogmap.
  Also trigger when creating new features that need reactive data flow from Room → ViewModel → UI.
---

# MVVM Flow Handler — Dogmap

## Architecture Overview

Dogmap follows a unidirectional data flow:

```
Room DB → Repository (Flow) → ViewModel (StateFlow) → Compose UI (collectAsState)
```

State is centralized in a single `DogUiState` data class. The ViewModel is the only class that mutates state. UI reads state and sends events back to the ViewModel.

## The UiState Pattern

All screen state lives inside one data class:

```kotlin
data class DogUiState(
    val dogs: List<Dog> = emptyList(),
    val selectedDog: Dog? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Add new properties here with defaults
)
```

**Rules for extending UiState:**
- Every new property MUST have a default value so existing code keeps compiling.
- Use immutable types (List, not MutableList).
- Keep it flat — avoid deeply nested data classes unless there's a clear domain reason.

## ViewModel Patterns

### Exposing State

```kotlin
class DogViewModel(private val repository: DogRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(DogUiState())
    val uiState: StateFlow<DogUiState> = _uiState.asStateFlow()
```

### Updating State — Always Use `.copy()`

```kotlin
fun selectDog(dog: Dog) {
    _uiState.value = _uiState.value.copy(selectedDog = dog)
}
```

Never assign a brand-new `DogUiState(...)` — you'll lose every other field. Always `.copy()` the current value.

### Converting Room Flows to StateFlow

When the ViewModel needs to observe a Room query:

```kotlin
init {
    viewModelScope.launch {
        repository.getAllDogs().collect { dogList ->
            _uiState.value = _uiState.value.copy(
                dogs = dogList,
                isLoading = false
            )
        }
    }
}
```

Alternatively, for a single derived flow use `stateIn`:

```kotlin
val dogCount: StateFlow<Int> = repository.getDogCount()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0
    )
```

Use `SharingStarted.Eagerly` as the project convention — it keeps the flow active for the lifetime of the ViewModel, avoiding resubscription delays on configuration changes.

### Handling Async Operations (CRUD)

```kotlin
fun addDog(dog: Dog) {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            repository.insertDog(dog)
            // Room Flow will automatically update the dogs list
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = e.message,
                isLoading = false
            )
        }
    }
}
```

The pattern is: set loading → call repository → let the Room Flow update the list, or catch and set error.

## Connecting to Compose UI

```kotlin
@Composable
fun DogListScreen(viewModel: DogViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        CircularProgressIndicator()
    }

    LazyColumn {
        items(uiState.dogs) { dog ->
            DogCard(dog = dog, onClick = { viewModel.selectDog(dog) })
        }
    }

    uiState.errorMessage?.let { error ->
        Snackbar { Text(error) }
    }
}
```

## Checklist for Adding a New Feature

1. Add any new state properties to `DogUiState` with defaults.
2. Add the corresponding mutation functions to `DogViewModel` using `.copy()`.
3. If the feature reads from Room, wire the Flow in the `init` block or via `stateIn`.
4. In the Composable, `collectAsState()` and derive UI from the state — never hold local mutable state that duplicates ViewModel state.
5. Handle loading and error states visibly in the UI.

## Anti-Patterns to Avoid

- **Mutating `_uiState.value` fields directly** — `_uiState.value.dogs = newList` won't trigger recomposition. Always `.copy()`.
- **Using `LiveData`** — this project uses `StateFlow` exclusively.
- **Launching coroutines from Compose** — use `LaunchedEffect` only for side-effects tied to composition, not for data loading (that belongs in the ViewModel).
- **Creating multiple MutableStateFlows** for unrelated state — keep it unified in `DogUiState` unless there's a performance reason to split.
