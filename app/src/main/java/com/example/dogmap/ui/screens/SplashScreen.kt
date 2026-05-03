// File: app/src/main/java/com/example/dogmap/ui/screens/SplashScreen.kt
// PawPlace splash — calm dark gradient, soft cyan pulse, fine progress bar.
package com.example.dogmap.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dogmap.ui.components.PawPlaceLogo
import com.example.dogmap.ui.theme.BrandPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onStartClick: () -> Unit) {
    // Auto-advance after 2.6s — feels like the app is sourcing locations.
    LaunchedEffect(Unit) {
        delay(2600)
        onStartClick()
    }

    val transition = rememberInfiniteTransition(label = "pp_splash")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

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
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Halo + logo
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f * pulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BrandPrimary.copy(alpha = 0.20f * haloAlpha),
                                BrandPrimary.copy(alpha = 0.06f * haloAlpha),
                                Color.Transparent,
                            ),
                            radius = r,
                        ),
                        radius = r,
                    )
                }
                Box(Modifier.scale(pulse)) {
                    PawPlaceLogo(size = 108.dp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "PawPlace",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
                letterSpacing = (-0.6).sp,
            )

            Spacer(Modifier.height(28.dp))

            // Hairline progress sweep
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(1.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0x14FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    BrandPrimary,
                                    Color.Transparent,
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "BUSCANDO UBICACIONES PET-FRIENDLY…",
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                color = Color(0x73F2F6F8),
                textAlign = TextAlign.Center,
            )
        }

        // Footer
        Text(
            text = "v 1.0  ·  2026",
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = Color(0x40F2F6F8),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}
