package com.fitness.snapapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FitnessColorScheme = darkColorScheme(
    primary         = Color(0xFF00E5FF),   // Cyan accent
    onPrimary       = Color(0xFF001F26),
    primaryContainer= Color(0xFF003640),
    secondary       = Color(0xFF00FF87),   // Green accent
    onSecondary     = Color(0xFF00210F),
    background      = Color(0xFF0A0A0F),   // Near-black
    surface         = Color(0xFF14141E),
    onBackground    = Color(0xFFE0E0E0),
    onSurface       = Color(0xFFE0E0E0),
    error           = Color(0xFFFF4444)
)

@Composable
fun FitnessAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FitnessColorScheme,
        typography  = Typography(),
        content     = content
    )
}
