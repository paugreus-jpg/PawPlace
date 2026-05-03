// File: app/src/main/java/com/example/dogmap/ui/theme/Color.kt
// PawPlace palette — deep space dark + electric cyan accent
package com.example.dogmap.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand (cyan accent) ───────────────────────────────────────────
val BrandPrimary = Color(0xFF7DD8FF)          // electric cyan
val BrandPrimaryDark = Color(0xFF7DD8FF)      // same in dark — accent stays
val BrandPrimaryDeep = Color(0xFF5BC2EE)      // pressed / gradient end
val BrandOnPrimary = Color(0xFF03161F)        // ink on cyan

val BrandSecondary = Color(0xFF9FF0C8)        // mint (open / available)
val BrandSecondaryDark = Color(0xFF9FF0C8)
val BrandOnSecondary = Color(0xFF03161F)

val BrandTertiary = Color(0xFFF5C77E)         // warm amber (alerts / new pin)
val BrandTertiaryDark = Color(0xFFF5C77E)

// ─── Light ─────────────────────────────────────────────────────────
val LightBackground = Color(0xFFF7F9FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFEEF2F5)
val LightSurfaceContainerHigh = Color(0xFFE3E9ED)
val LightOnBackground = Color(0xFF0A1218)
val LightOnSurfaceVariant = Color(0xFF4A5560)

// ─── Dark — premium "deep space" base ──────────────────────────────
val DarkBackground = Color(0xFF070D11)        // app background
val DarkSurface = Color(0xFF0B141A)            // map base, sheets behind glass
val DarkSurfaceContainer = Color(0xFF101A22)   // chips, low-elev
val DarkSurfaceContainerHigh = Color(0xFF142028) // glass tint base
val DarkOnBackground = Color(0xFFF2F6F8)       // 100% white-ish
val DarkOnSurfaceVariant = Color(0xFFB0BCC5)   // ~70% — secondary text

// ─── Glass tokens (for Modifier.background with alpha) ─────────────
val GlassFill = Color(0x14FFFFFF)              // 8% white
val GlassFillStrong = Color(0x1FFFFFFF)        // 12% white
val GlassStroke = Color(0x14FFFFFF)            // 8% white border
val GlassStrokeBright = Color(0x33FFFFFF)      // 20% — top highlight

// ─── Marker accents ────────────────────────────────────────────────
val MarkerCafe = Color(0xFF7DD8FF)             // cyan
val MarkerPark = Color(0xFF9FF0C8)             // mint
val MarkerStay = Color(0xFFF5C77E)             // amber
val MarkerVet  = Color(0xFFFF9DB1)             // soft coral
val NewPinAmber = Color(0xFFF5C77E)
