package sk.ntnlabs.kyberia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF39FF88),
    onPrimary = Color(0xFF00210E),
    secondary = Color(0xFFFF2E9A),
    background = Color(0xFF000000),
    surface = Color(0xFF050806),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B7F4A),
    secondary = Color(0xFFC2185B),
)

@Composable
fun MokapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
