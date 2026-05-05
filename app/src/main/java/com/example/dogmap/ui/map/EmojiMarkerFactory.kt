package com.example.dogmap.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.roundToInt

fun buildEmojiMarkerBitmap(
    context: Context,
    emoji: String,
    backgroundColor: Int
): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (44 * density).roundToInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val outerR = sizePx / 2f
    val innerR = outerR * 0.682f  // matches 15/22 ratio from existing drawables

    val r = (backgroundColor shr 16) and 0xFF
    val g = (backgroundColor shr 8) and 0xFF
    val b = backgroundColor and 0xFF
    fun withAlpha(a: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    // Radial halo
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, cy, outerR,
            intArrayOf(withAlpha(0x55), withAlpha(0x10), withAlpha(0x00)),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
    }.also { canvas.drawCircle(cx, cy, outerR, it) }

    // Solid colored disc
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = withAlpha(0xDD)
    }.also { canvas.drawCircle(cx, cy, innerR, it) }

    // Frosted glass overlay
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx - innerR, cy - innerR, cx + innerR, cy + innerR,
            intArrayOf(0x2EFFFFFF, 0x0AFFFFFF, 0x40000000),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }.also { canvas.drawCircle(cx, cy, innerR, it) }

    // Stroke
    val strokeW = sizePx * 0.023f
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeW
        color = withAlpha(0x88)
    }.also { canvas.drawCircle(cx, cy, innerR - strokeW / 2f, it) }

    // Emoji centered
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = innerR * 0.88f
        textAlign = Paint.Align.CENTER
    }.also { paint ->
        val metrics = paint.fontMetrics
        val textY = cy - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(emoji, cx, textY, paint)
    }

    return bitmap
}
