package com.camerasuite.quest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.camerasuite.quest.settings.AppAccent

val Bg = Color(0xFF05070D)
val Panel = Color(0xFF0B0F1A)
val PanelAlt = Color(0xFF121829)
val Stroke = Color(0xFF22304E)
val TextHi = Color(0xFFE8F6FF)
val TextMid = Color(0xFF8EA3C8)
val Danger = Color(0xFFFF5470)
val Ok = Color(0xFF3CE8A5)
val WarnAmber = Color(0xFFFFB02E)

@Composable
fun QuestCameraTheme(accent: AppAccent, uiScale: Float, content: @Composable () -> Unit) {
    val accentColor = when (accent) {
        AppAccent.CYAN -> Color(0xFF45E8FF)
        AppAccent.VIOLET -> Color(0xFF8F7BFF)
        AppAccent.AMBER -> Color(0xFFFFB02E)
    }

    val scheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color(0xFF04101A),
        secondary = Color(0xFF6E7FFF),
        tertiary = Color(0xFF3CE8A5),
        background = Bg,
        onBackground = TextHi,
        surface = Panel,
        onSurface = TextHi,
        surfaceVariant = PanelAlt,
        onSurfaceVariant = TextMid,
        outline = Stroke,
        error = Danger,
        onError = Color(0xFF1A0408)
    )

    // Interface scaling (settings) scales type without breaking layout density.
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density, base.fontScale * uiScale.coerceIn(0.85f, 1.3f))
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
