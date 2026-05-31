package com.al3rted.app.ui

import androidx.compose.animation.core.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

enum class AppTheme(val label: String, val isDark: Boolean = false) {
    VIOLET("Violet"),
    MIDNIGHT("Midnight", isDark = true),
    DARK("Dark", isDark = true),
    SUNSET("Sunset"),
    FOREST("Forest"),
    NEON("Neon", isDark = true),
    ROSE_GOLD("Rose Gold"),
    RAINBOW("Rainbow");

    companion object {
        fun fromOrdinal(o: Int) = entries.getOrElse(o) { VIOLET }
    }
}

fun themeColors(theme: AppTheme, rainbowHue: Float = 0f): ColorScheme = when (theme) {
    AppTheme.VIOLET -> lightColorScheme(
        primary = Color(0xFF7B2FBE),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEAD5FF),
        secondary = Color(0xFF9C4FFF),
        background = Color(0xFFF7F0FF),
        surface = Color(0xFFF7F0FF),
        onBackground = Color(0xFF1A0A2E),
        onSurface = Color(0xFF1A0A2E),
        error = Color(0xFFCC0000),
        onError = Color.White,
    )
    AppTheme.MIDNIGHT -> darkColorScheme(
        primary = Color(0xFFBB86FC),
        onPrimary = Color(0xFF1A0A2E),
        primaryContainer = Color(0xFF3700B3),
        secondary = Color(0xFF03DAC5),
        background = Color(0xFF0D0D1A),
        surface = Color(0xFF1A1A2E),
        onBackground = Color(0xFFE8E0FF),
        onSurface = Color(0xFFE8E0FF),
        error = Color(0xFFCF6679),
        onError = Color.Black,
    )
    AppTheme.DARK -> darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF0D1B2A),
        primaryContainer = Color(0xFF1A2A3A),
        secondary = Color(0xFF80DEEA),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0),
        error = Color(0xFFCF6679),
        onError = Color.Black,
    )
    AppTheme.SUNSET -> lightColorScheme(
        primary = Color(0xFFCC4400),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9CC),
        secondary = Color(0xFFFF6B35),
        background = Color(0xFFFFF5EE),
        surface = Color(0xFFFFF5EE),
        onBackground = Color(0xFF3A0A00),
        onSurface = Color(0xFF3A0A00),
        error = Color(0xFFCC0000),
        onError = Color.White,
    )
    AppTheme.FOREST -> lightColorScheme(
        primary = Color(0xFF1B5E20),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB8F5B1),
        secondary = Color(0xFF388E3C),
        background = Color(0xFFEFF7EF),
        surface = Color(0xFFEFF7EF),
        onBackground = Color(0xFF071A07),
        onSurface = Color(0xFF071A07),
        error = Color(0xFFCC0000),
        onError = Color.White,
    )
    AppTheme.NEON -> darkColorScheme(
        primary = Color(0xFF00FFCC),
        onPrimary = Color(0xFF001A14),
        primaryContainer = Color(0xFF004D3A),
        secondary = Color(0xFFFF00FF),
        background = Color(0xFF060612),
        surface = Color(0xFF0D0D1F),
        onBackground = Color(0xFFE0FFFA),
        onSurface = Color(0xFFE0FFFA),
        error = Color(0xFFFF4444),
        onError = Color.Black,
    )
    AppTheme.ROSE_GOLD -> lightColorScheme(
        primary = Color(0xFFB76E79),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9DD),
        secondary = Color(0xFFCF9B7C),
        background = Color(0xFFFFF5F6),
        surface = Color(0xFFFFF5F6),
        onBackground = Color(0xFF3A0C14),
        onSurface = Color(0xFF3A0C14),
        error = Color(0xFFCC0000),
        onError = Color.White,
    )
    AppTheme.RAINBOW -> {
        val hsl = floatArrayOf(rainbowHue, 0.80f, 0.45f)
        val primary = Color(ColorUtils.HSLToColor(hsl))
        val hsl2 = floatArrayOf((rainbowHue + 40f) % 360f, 0.75f, 0.55f)
        val secondary = Color(ColorUtils.HSLToColor(hsl2))
        val hslBg = floatArrayOf((rainbowHue + 200f) % 360f, 0.30f, 0.96f)
        val bg = Color(ColorUtils.HSLToColor(hslBg))
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = bg,
            secondary = secondary,
            background = bg,
            surface = bg,
            onBackground = Color(0xFF1A1A1A),
            onSurface = Color(0xFF1A1A1A),
            error = Color(0xFFCC0000),
            onError = Color.White,
        )
    }
}

/** Returns animated hue for rainbow theme (0-360 cycling over 8 seconds). */
@Composable
fun rememberRainbowHue(): Float {
    val inf = rememberInfiniteTransition(label = "rainbow")
    val hue by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "hue"
    )
    return hue
}
