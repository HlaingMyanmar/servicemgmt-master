package com.sspd.servicemgmt.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sspd.servicemgmt.ui.theme.Primary
import kotlin.math.PI
import kotlin.math.sin

/**
 * Drop-in replacement for full-screen CircularProgressIndicator.
 * Usage:  AppLoading(Modifier.fillMaxSize().padding(padding))
 */
@Composable
fun AppLoading(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TypingLoader(Modifier.size(190.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                "ခေတ္တစောင့်ပါ...",
                color      = Primary,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Internal animation composable ─────────────────────────────────────────────
@Composable
private fun TypingLoader(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "typing")

    // Key-press phase: 0→1 every 900 ms  (controls which keys are pressed)
    val phase by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    // Cursor blink: visible / invisible
    val cursor by inf.animateFloat(
        1f, 0f,
        infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursor"
    )

    // Loading-dot wave: 0→1 every 1 200 ms (staggered via sin offset)
    val dot by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "dot"
    )

    // Which keys are pressed each quarter-phase
    val pressedLeft  = phase < 0.50f
    val pressedRight = phase >= 0.25f && phase < 0.75f

    val pressedKeys: Set<Pair<Int, Int>> = when {
        phase < 0.25f -> setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        phase < 0.50f -> setOf(0 to 7, 0 to 8, 1 to 7, 1 to 8)
        phase < 0.75f -> setOf(1 to 2, 1 to 3, 2 to 1, 2 to 2)
        else          -> setOf(1 to 5, 1 to 6, 2 to 5, 2 to 6)
    }

    Canvas(modifier = modifier) {
        val W   = size.width
        val H   = size.height
        val cx  = W / 2f

        // ── Palette ────────────────────────────────────────────────────
        val cBezel   = Color(0xFF334155)
        val cScreen  = Color(0xFF0F172A)
        val cBody    = Color(0xFF475569)
        val cKey     = Color(0xFF475569)
        val cKeyPrs  = Color(0xFF2563EB)
        val cHand    = Color(0xFFFFD0A0)
        val cBlue    = Color(0xFF60A5FA)
        val cGreen   = Color(0xFF34D399)
        val cYellow  = Color(0xFFFBBF24)
        val cDot     = Primary

        // ── Monitor bezel ──────────────────────────────────────────────
        val monW  = W * 0.66f
        val monH  = H * 0.36f
        val monL  = cx - monW / 2f
        val monT  = H * 0.03f

        drawRoundRect(cBezel, Offset(monL - 7f, monT - 7f),
            Size(monW + 14f, monH + 14f), CornerRadius(12f))
        // Webcam dot
        drawCircle(cBody, 3f, Offset(cx, monT - 3.5f))

        // ── Screen display ─────────────────────────────────────────────
        drawRoundRect(cScreen, Offset(monL, monT), Size(monW, monH), CornerRadius(7f))

        // Code lines (colored bars representing syntax)
        val lineData = listOf(
            Triple(0.06f, 0.44f, cBlue),   // keyword line
            Triple(0.06f, 0.32f, cGreen),  // string line
            Triple(0.06f, 0.50f, cYellow), // variable line  — cursor here
        )
        lineData.forEachIndexed { i, (xRatio, lenRatio, color) ->
            val ly = monT + monH * (0.26f + i * 0.27f)
            drawRoundRect(color.copy(0.65f),
                Offset(monL + monW * xRatio, ly),
                Size(monW * lenRatio, 6f), CornerRadius(3f))
        }

        // Blinking cursor after the third line
        if (cursor > 0.5f) {
            val cursorX = monL + monW * (0.06f + 0.50f) + 4f
            val cursorY = monT + monH * (0.26f + 2 * 0.27f) - 2f
            drawRect(cBlue, Offset(cursorX, cursorY), Size(2.5f, 10f))
        }

        // ── Laptop body / hinge ────────────────────────────────────────
        val bodyT = monT + monH + 6f
        val bodyH = H * 0.045f
        drawRoundRect(cBody, Offset(monL - 10f, bodyT),
            Size(monW + 20f, bodyH), CornerRadius(5f))
        // Hinge groove
        drawLine(cScreen.copy(0.5f),
            Offset(monL - 10f, bodyT + 2f),
            Offset(monL + monW + 10f, bodyT + 2f), 1.5f)

        // ── Keyboard body ──────────────────────────────────────────────
        val kbT  = bodyT + bodyH + 3f
        val kbW  = monW + 44f
        val kbH  = H * 0.22f
        val kbL  = cx - kbW / 2f

        drawRoundRect(cScreen, Offset(kbL, kbT), Size(kbW, kbH), CornerRadius(10f))
        drawRoundRect(cBezel, Offset(kbL + 4f, kbT + 4f),
            Size(kbW - 8f, kbH - 8f), CornerRadius(8f),
            style = Stroke(1.5f))

        // ── Keys grid (3 rows × 10 cols) ───────────────────────────────
        val rows = 3; val cols = 10
        val kPad = 9f; val kGap = 3.5f
        val kw   = (kbW - 8f - kPad * 2 - kGap * (cols - 1)) / cols
        val kh   = (kbH - 8f - kPad * 2 - kGap * (rows - 1)) / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val kx  = kbL + 4f + kPad + c * (kw + kGap)
                val ky  = kbT + 4f + kPad + r * (kh + kGap)
                val pressed = (r to c) in pressedKeys
                val pOff = if (pressed) 2f else 0f
                drawRoundRect(
                    if (pressed) cKeyPrs else cKey,
                    Offset(kx, ky + pOff),
                    Size(kw, kh - pOff),
                    CornerRadius(3.5f)
                )
                // Subtle key shadow on bottom
                if (!pressed) drawRoundRect(
                    cScreen.copy(0.5f),
                    Offset(kx, ky + kh - 2f),
                    Size(kw, 2f), CornerRadius(1f)
                )
            }
        }

        // ── Hands ──────────────────────────────────────────────────────
        val handY = kbT + kbH * 0.50f

        // Left hand
        val lhX  = kbL + kbW * 0.19f
        val lhOff = if (pressedLeft) 4f else 0f
        // Palm
        drawOval(cHand, Offset(lhX - 24f, handY + lhOff), Size(48f, 20f))
        // Fingers (4)
        val lFingerX = listOf(-20f, -8f, 4f, 16f)
        val lFingerH = listOf(14f, 18f, 18f, 13f)
        lFingerX.zip(lFingerH).forEach { (fx, fh) ->
            drawRoundRect(cHand,
                Offset(lhX + fx, handY - fh + lhOff),
                Size(10f, fh), CornerRadius(5f))
        }

        // Right hand
        val rhX  = kbL + kbW * 0.81f
        val rhOff = if (pressedRight) 4f else 0f
        drawOval(cHand, Offset(rhX - 24f, handY + rhOff), Size(48f, 20f))
        val rFingerX = listOf(-20f, -8f, 4f, 16f)
        val rFingerH = listOf(13f, 18f, 18f, 14f)
        rFingerX.zip(rFingerH).forEach { (fx, fh) ->
            drawRoundRect(cHand,
                Offset(rhX + fx, handY - fh + rhOff),
                Size(10f, fh), CornerRadius(5f))
        }

        // ── Loading dots (wave bounce) ─────────────────────────────────
        val dotY   = H * 0.94f
        val dotR   = 5f
        val dotGap = 18f

        fun bounce(offset: Float): Float {
            val p = ((dot + offset) % 1f).toDouble() * PI
            return -(dotR * 1.8f) * sin(p).toFloat().coerceAtLeast(0f)
        }

        drawCircle(cDot, dotR, Offset(cx - dotGap, dotY + bounce(0.00f)))
        drawCircle(cDot, dotR, Offset(cx,           dotY + bounce(0.33f)))
        drawCircle(cDot, dotR, Offset(cx + dotGap,  dotY + bounce(0.66f)))
    }
}
