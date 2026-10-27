package com.premierpicks.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.premierpicks.engine.CardType

val Pitch = Color(0xFF0B3D1E)
val PitchLight = Color(0xFF14572C)
val CardBack = Color(0xFF1F7A3A)
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

data class CardFace(val emoji: String, val label: String, val background: Color, val text: Color = Color(0xFF1B1B1B))

fun faceOf(type: CardType): CardFace = when (type) {
    CardType.GOAL -> CardFace("⚽", "GOAL", Color(0xFFFFD54F))
    CardType.PENALTY -> CardFace("🎯", "PENALTY", Color(0xFFFFB74D))
    CardType.HINT -> CardFace("💡", "HINT", Color(0xFF4FC3F7))
    CardType.MISS -> CardFace("🥅", "MISS", Color(0xFFB0BEC5))
    CardType.SUBSTITUTE -> CardFace("🔄", "SUB", Color(0xFFA5D6A7))
    CardType.FOUL -> CardFace("🤕", "FOUL", Color(0xFFE57373))
    CardType.YELLOW -> CardFace("🟨", "BOOKED", Color(0xFFFFF176))
    CardType.RED -> CardFace("🟥", "SENT OFF", Color(0xFFC62828), Color.White)
}
