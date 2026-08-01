package com.example.healthbridgeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4F8EF7),
    secondary = Color(0xFF00E5A0),
    background = Color(0xFF0A0E1A),
    surface = Color(0xFF141829),
    onPrimary = Color.White,
    onBackground = Color(0xFFEEF0F8),
    onSurface = Color(0xFFEEF0F8)
)

@Composable
fun HealthBridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
