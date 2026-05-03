// File: app/src/main/java/com/example/dogmap/ui/components/PawPlaceLogo.kt
package com.example.dogmap.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dogmap.ui.theme.BrandPrimary

/**
 * PawPlace mark — a teardrop pin with a paw (4 toes + 1 pad) cut as
 * negative space. Glass body + cyan rim + specular arc.
 *
 * Uses Canvas instead of a vector drawable so it scales perfectly and
 * can recolor via the [accent] parameter (handy for light/dark themes).
 */
@Composable
fun PawPlaceLogo(
    size: Dp = 96.dp,
    accent: Color = BrandPrimary,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val w = this.size.width
            val s = w / 120f  // viewBox is 120

            // Teardrop path
            val body = Path().apply {
                moveTo(60f * s, 8f * s)
                cubicTo(84.3f * s, 8f * s, 104f * s, 27.7f * s, 104f * s, 52f * s)
                cubicTo(104f * s, 79f * s, 76f * s, 102f * s, 60f * s, 112f * s)
                cubicTo(44f * s, 102f * s, 16f * s, 79f * s, 16f * s, 52f * s)
                cubicTo(16f * s, 27.7f * s, 35.7f * s, 8f * s, 60f * s, 8f * s)
                close()
            }

            // Paw cutout — 4 toes + 1 pad
            val paw = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(
                    Offset((60 - 13) * s, (58 - 10.5f) * s),
                    Size(26f * s, 21f * s)
                ))
                addOval(androidx.compose.ui.geometry.Rect(
                    Offset((44.5f - 5.2f) * s, (40 - 6.4f) * s),
                    Size(10.4f * s, 12.8f * s)
                ))
                addOval(androidx.compose.ui.geometry.Rect(
                    Offset((55 - 4.6f) * s, (33.5f - 6) * s),
                    Size(9.2f * s, 12f * s)
                ))
                addOval(androidx.compose.ui.geometry.Rect(
                    Offset((65.5f - 4.6f) * s, (33.5f - 6) * s),
                    Size(9.2f * s, 12f * s)
                ))
                addOval(androidx.compose.ui.geometry.Rect(
                    Offset((76 - 5.2f) * s, (40 - 6.4f) * s),
                    Size(10.4f * s, 12.8f * s)
                ))
            }

            val masked = Path.combine(PathOperation.Difference, body, paw)

            // Glass body fill
            drawPath(
                masked,
                Brush.linearGradient(
                    listOf(
                        Color(0xF2EAF6FF),
                        accent.copy(alpha = 0.55f),
                        Color(0xE60A2A3A),
                    ),
                    start = Offset(20f * s, 8f * s),
                    end = Offset(100f * s, 112f * s),
                ),
            )

            // Cyan rim
            drawPath(
                body,
                color = accent.copy(alpha = 0.55f),
                style = Stroke(width = 0.8f * s),
            )

            // Specular arc (top-left)
            val sheen = Path().apply {
                moveTo(34f * s, 28f * s)
                quadraticBezierTo(48f * s, 16f * s, 70f * s, 16f * s)
            }
            drawPath(
                sheen,
                color = Color.White.copy(alpha = 0.55f),
                style = Stroke(width = 1.4f * s),
            )
        }
    }
}
