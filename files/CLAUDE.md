---
name: dogmap-project-guide
description: >
  Master guide for the Dogmap Android project. Consult this FIRST for any task related to
  Dogmap development. It provides the project overview, critical rules, and directs you to
  the appropriate specialized skill for each type of task. Use whenever you see "Dogmap",
  "dog app", "android project", or any reference to this codebase.
---

# Dogmap — Project Development Guide

## What Is Dogmap?

An Android native app for registering and geolocating dogs. Users can map dog locations, manage a local dog database, and visualize dogs on a real-time map.

## Critical Rule #1 — The Namespace Split

The project namespace (`com.dogmap`) differs from the source package (`com.example.dogmap`).
**Every Kotlin file that uses `R` must have `import com.dogmap.R`.**
See: `android-resource-manager` skill for full details.

## Architecture

Clean Architecture + MVVM with unidirectional data flow:

```
Room DB  →  Repository  →  ViewModel (StateFlow)  →  Compose UI
```

Changes flow down; events flow up via ViewModel functions.

## Skill Map — Which Skill to Use

| Task | Primary Skill | Also Check |
|------|--------------|------------|
| Adding/editing UI components | `ui-specialist` | `android-resource-manager` |
| New screen + navigation route | `navigation-manager` | `ui-specialist` |
| Database schema changes | `data-architect` | `room-database-manager` |
| ViewModel / state logic | `mvvm-flow-handler` | `data-architect` |
| Map features & markers | `maps-integration` | `android-resource-manager` |
| DB setup & dependency injection | `room-database-manager` | `data-architect` |
| Any file referencing R | `android-resource-manager` | — |

## Dependency Versions (Do Not Change Without Reason)

- Compose BOM: `2024.12.01`
- Navigation Compose: `2.8.4`
- Room: `2.6.1`
- Lifecycle: `2.8.7`
- Play Services Maps: `19.0.0`
- Maps Compose: `6.2.1`

## Build Commands

```bash
./gradlew clean assembleDebug    # Clean build
./gradlew test                    # Run tests
./gradlew :app:compileDebugKotlin # Quick check for R and compilation errors
```

## Before Every Commit

1. Run `./gradlew :app:compileDebugKotlin` — fix any errors
2. `grep -rn "import com.example.dogmap.R" app/src/main/java/` — must return zero results
3. Verify new screens are wired in NavHost
4. Verify new DB fields propagated through DAO → Repository → ViewModel
