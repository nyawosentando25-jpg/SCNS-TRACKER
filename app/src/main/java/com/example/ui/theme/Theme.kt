package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ScnsNavyLight,
    onPrimary = ScnsNavyDark,
    primaryContainer = ScnsNavyDarkContainer,
    onPrimaryContainer = Color(0xFFE5ECFF),
    secondary = ScnsGold,
    onSecondary = ScnsNavyDark,
    secondaryContainer = Color(0xFF4D3B09),
    onSecondaryContainer = ScnsGoldContainer,
    tertiary = ScnsEmerald,
    onTertiary = Color(0xFF00211A),
    tertiaryContainer = Color(0xFF0D4F41),
    onTertiaryContainer = ScnsEmeraldContainer,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF6B1810),
    onErrorContainer = Color(0xFFFFDAD6),
    background = ScnsCanvasDark,
    onBackground = ScnsOnSurfaceDark,
    surface = ScnsSurfaceDark,
    onSurface = ScnsOnSurfaceDark,
    surfaceVariant = ScnsSurfaceVariantDark,
    onSurfaceVariant = ScnsOnSurfaceMutedDark,
    outline = ScnsOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ScnsNavy,
    onPrimary = Color.White,
    primaryContainer = ScnsNavyContainer,
    onPrimaryContainer = ScnsNavyDark,
    secondary = ScnsGoldDeep,
    onSecondary = Color.White,
    secondaryContainer = ScnsGoldContainer,
    onSecondaryContainer = ScnsNavyDark,
    tertiary = ScnsEmeraldDeep,
    onTertiary = Color.White,
    tertiaryContainer = ScnsEmeraldContainer,
    onTertiaryContainer = Color(0xFF063A30),
    error = ScnsCrimson,
    onError = Color.White,
    errorContainer = ScnsCrimsonContainer,
    onErrorContainer = Color(0xFF5D130C),
    background = ScnsCanvasLight,
    onBackground = ScnsOnSurfaceLight,
    surface = ScnsSurfaceLight,
    onSurface = ScnsOnSurfaceLight,
    surfaceVariant = ScnsSurfaceVariantLight,
    onSurfaceVariant = ScnsOnSurfaceMutedLight,
    outline = ScnsOutlineLight
)

val ScnsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun SCNSRecordsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ScnsShapes,
        content = content
    )
}
