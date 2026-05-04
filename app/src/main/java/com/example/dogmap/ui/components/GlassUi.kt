// File: app/src/main/java/com/example/dogmap/ui/components/GlassUi.kt
// PawPlace shared glass primitives — used by Login, Register, List, Add, Profile.
// All screens borrow the language established by MapScreen (deep-space gradient,
// 8% white fill, 8% white hairline border, BrandPrimary cyan accents).
package com.example.dogmap.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.dogmap.R
import com.example.dogmap.ui.theme.BrandPrimary

/** Normaliza cualquier string de tipo (en cualquier idioma o clave) a una clave invariante. */
fun typeKey(type: String): String = when (type.trim().lowercase()) {
    "café", "cafe", "cafetería", "cafeteria" -> "cafe"
    "parque", "park" -> "park"
    "playa", "beach" -> "beach"
    "restaurante", "restaurant" -> "restaurant"
    "veterinario", "vet" -> "vet"
    else -> type.lowercase()
}

/* ─── Background ─── */
/** The deep-space radial gradient used behind every PawPlace surface. */
@Composable
fun PawPlaceBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF142028),
                        Color(0xFF0A1218),
                        Color(0xFF04080B),
                    ),
                    center = Offset(0.5f, 0.25f) * 1000f,
                    radius = 1400f,
                )
            ),
        content = content
    )
}

/* ─── Glass card ─── */
/** A single sheet of glass — 8% white over the gradient, hairline border, soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 24,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x1FFFFFFF),
                            Color(0x0AFFFFFF),
                        )
                    )
                )
                .padding(20.dp),
            content = content
        )
    }
}

/* ─── Glass pill — used by chips and the search bar ─── */
@Composable
fun GlassPill(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    accent: Color = BrandPrimary,
    label: String,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else Color(0x14FFFFFF),
        label = "pill_bg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) Color(0x66FFFFFF) else Color(0x14FFFFFF),
        label = "pill_border"
    )
    val fg = if (selected) Color(0xFF03161F) else Color(0xCCF2F6F8)

    Surface(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(width = 1.dp, color = border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leading != null) leading()
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = fg,
            )
        }
    }
}

/* ─── Marker glyph disc — the same vocabulary as map markers ─── */
@Composable
fun MarkerGlyph(
    type: String,
    size: Int = 36,
) {
    val (accent, glyph) = when (typeKey(type)) {
        "cafe" -> Color(0xFF7DD8FF) to "☕"
        "park" -> Color(0xFF9FF0C8) to "🌳"
        "beach" -> Color(0xFFC9B8FF) to "🌊"
        "restaurant" -> Color(0xFFF5C77E) to "🍽"
        "vet" -> Color(0xFFFF9DB1) to "✚"
        else -> Color(0xFFF5C77E) to "🏠"
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.45f),
                        accent.copy(alpha = 0.10f),
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            fontSize = (size * 0.45f).sp,
            color = Color(0xFFF2F6F8),
        )
    }
}

/** Returns the brand accent for a place type — keeps glyph and chip colours in sync. */
fun accentForType(type: String): Color = when (typeKey(type)) {
    "cafe" -> Color(0xFF7DD8FF)
    "park" -> Color(0xFF9FF0C8)
    "beach" -> Color(0xFFC9B8FF)
    "restaurant" -> Color(0xFFF5C77E)
    "vet" -> Color(0xFFFF9DB1)
    else -> Color(0xFFF5C77E)
}

/** Traduce una clave de tipo (o string legacy) al nombre localizado según el idioma activo. */
@Composable
fun localizeType(type: String): String = when (typeKey(type)) {
    "park" -> stringResource(R.string.type_park)
    "beach" -> stringResource(R.string.type_beach)
    "restaurant" -> stringResource(R.string.type_restaurant)
    "vet" -> stringResource(R.string.type_vet)
    "cafe" -> stringResource(R.string.type_cafe)
    else -> type
}
