package com.yourname.iprotation2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue10,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color.White,
    secondary = Cyan80,
    background = Surface10,
    onBackground = Color.White,
    surface = Surface10,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB8C4E0),
    surfaceContainer = Color(0xFF111B36),
    surfaceContainerHigh = Color(0xFF16223F),
    surfaceContainerHighest = Color(0xFF1B2948),
    outline = Color(0xFF4A5A85)
)

private val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E3FF),
    onPrimaryContainer = Blue10,
    secondary = Blue10,
    background = Surface90,
    surface = Surface90,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF1FA),
    surfaceContainerHighest = Color(0xFFE6EAF6)
)

@Composable
fun IPRotationTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
