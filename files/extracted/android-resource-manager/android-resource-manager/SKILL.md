---
name: android-resource-manager
description: >
  Manages the critical namespace discrepancy in the Dogmap Android project where the application
  namespace is `com.dogmap` but source classes live under `com.example.dogmap`. Use this skill
  ANY time you add, modify, or review UI components (Image, Text, Icon, drawable references,
  string resources), Compose files, XML layouts, or anything that references `R` in the Dogmap
  project. Also trigger when creating new Kotlin/Compose files, moving files between packages,
  debugging "Unresolved reference: R" errors, or when the build fails with resource-related issues.
  Even if the user just says "add a button" or "create a new screen", this skill applies.
---

# Android Resource Manager — Dogmap

## The Core Problem

Dogmap has a **split identity** between its build namespace and its source package:

| Concept | Value |
|---------|-------|
| **Application namespace** (build.gradle.kts) | `com.dogmap` |
| **Source package** (Kotlin files) | `com.example.dogmap` |
| **R class lives at** | `com.dogmap.R` |

This means every file that touches resources — drawables, strings, colors, dimensions, themes — needs an explicit import for `com.dogmap.R`. The IDE's auto-import will suggest `com.example.dogmap.R`, which does **not** exist and will fail the build.

## Rules

1. **Every Composable or Activity file that uses `R`** must contain:
   ```kotlin
   import com.dogmap.R
   ```
   Place it with the other imports, never use a wildcard that could shadow it.

2. **Never auto-accept IDE suggestions for R imports.** If you see `com.example.dogmap.R` anywhere, replace it immediately with `com.dogmap.R`.

3. **When creating a new Kotlin file** under `com.example.dogmap.*`:
   - The `package` declaration stays `com.example.dogmap.<subpackage>`
   - But any R reference must import `com.dogmap.R`

4. **Resource XML files** (in `res/`) are unaffected — they don't use package imports.

5. **Quick diagnostic command** if the build breaks on R references:
   ```bash
   grep -rn "import com.example.dogmap.R" app/src/main/java/
   ```
   Every hit is a bug. Replace with `com.dogmap.R`.

## Checklist Before Committing Any UI Change

- [ ] Searched the changed files for `com.example.dogmap.R` — zero matches
- [ ] Every file using `R.drawable`, `R.string`, `R.color`, etc. has `import com.dogmap.R`
- [ ] Ran `./gradlew :app:compileDebugKotlin` — no unresolved R errors

## Common Patterns

**Loading an image with Coil in Compose:**
```kotlin
import com.dogmap.R  // ← critical
import coil.compose.AsyncImage

AsyncImage(
    model = R.drawable.placeholder_dog,
    contentDescription = stringResource(R.string.dog_image_desc),
    // ...
)
```

**Referencing a string resource:**
```kotlin
import com.dogmap.R
import androidx.compose.ui.res.stringResource

Text(text = stringResource(R.string.app_name))
```
