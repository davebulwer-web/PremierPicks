package com.premierpicks.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Pitch = Color(0xFF0B3D1E)
val PitchLight = Color(0xFF14572C)
val Gold = Color(0xFFFFC107)

private val colors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1B1B1B),
    secondary = Color(0xFF81C784),
    background = Pitch,
    surface = PitchLight,
    onSurface = Color.White,
)

@Composable
fun PremierPicksTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
