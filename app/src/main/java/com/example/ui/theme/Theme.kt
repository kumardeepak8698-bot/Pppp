package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0E4FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF3E4759),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF8E9199),
    onSecondary = Color(0xFF1E2125),
    tertiary = Color(0xFFB4E39B),
    onTertiary = Color(0xFF153800),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF212327),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF212327),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF43474E)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF005FAF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    tertiary = Color(0xFF3B692A),
    onTertiary = Color.White,
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFECEFF5),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set to false to enforce authentic Slate look
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
