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
    primary = BentoPrimary,
    onPrimary = BentoBackground,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    secondary = BentoSecondaryContainer,
    onSecondary = BentoSecondary,
    secondaryContainer = BentoSecondaryContainer,
    onSecondaryContainer = BentoOnSecondaryContainer,
    tertiary = BentoTertiaryContainer,
    onTertiary = BentoTertiary,
    tertiaryContainer = BentoTertiary,
    onTertiaryContainer = BentoTertiaryContainer,
    background = BentoTertiary, // dark background
    onBackground = BentoTertiaryContainer,
    surface = BentoTertiary,
    onSurface = BentoTertiaryContainer,
    surfaceVariant = Color(0xFF493E50),
    onSurfaceVariant = BentoOutline,
    outline = BentoOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimary,
    onPrimary = Color.White,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    secondary = BentoSecondary,
    onSecondary = Color.White,
    secondaryContainer = BentoSecondaryContainer,
    onSecondaryContainer = BentoOnSecondaryContainer,
    tertiary = BentoTertiary,
    onTertiary = BentoTertiaryContainer,
    tertiaryContainer = BentoTertiaryContainer,
    onTertiaryContainer = BentoOnTertiaryContainer,
    background = BentoBackground,
    onBackground = BentoOnBackground,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    outline = BentoOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep the Bento custom branding by defaulting dynamicColor to false
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
