package sk.ntnlabs.kyberia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import sk.ntnlabs.kyberia.data.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFF39FF88),
    onPrimary = Color(0xFF00210E),
    secondary = Color(0xFFFF2E9A),
    background = Color(0xFF000000),
    surface = Color(0xFF050806),
)

/** „Jas na 50 %“: čierne pozadie, sivý text, stlmená zelená a ružová. */
private val DimColors = darkColorScheme(
    primary = Color(0xFF1F8A4C),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF0E3320),
    onPrimaryContainer = Color(0xFF8FB89F),
    secondary = Color(0xFF8A2457),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2E0F1F),
    onSecondaryContainer = Color(0xFFB08A9C),
    background = Color(0xFF000000),
    onBackground = Color(0xFF8C8C8C),
    surface = Color(0xFF000000),
    onSurface = Color(0xFF8C8C8C),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFF6E6E6E),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF101010),
    surfaceContainerHighest = Color(0xFF161616),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF242424),
    error = Color(0xFFA0524F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B7F4A),
    secondary = Color(0xFFC2185B),
)

/** Či majú obrázky (avatary) byť stlmené – zapína tlmená téma. */
val LocalDimImages = staticCompositionLocalOf { false }

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.DIM -> true
}

@Composable
fun MokapTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val colors = when {
        mode == ThemeMode.DIM -> DimColors
        isDarkTheme(mode) -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalDimImages provides (mode == ThemeMode.DIM)) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
