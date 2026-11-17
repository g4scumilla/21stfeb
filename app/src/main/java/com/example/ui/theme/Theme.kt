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
import com.example.data.model.ThemeMode

private val MyStoreDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CyanSecondary,
    onSecondary = CyanOnSecondary,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = CyanOnContainer,
    tertiary = AmberTertiary,
    onTertiary = AmberOnTertiary,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    error = CoralError,
    onError = CoralOnError,
    errorContainer = CoralContainer,
    onErrorContainer = CoralOnContainer,
    background = ObsidianBg,
    onBackground = DarkOnBg,
    surface = ObsidianSurface,
    onSurface = DarkOnBg,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceElevated,
    outline = ObsidianOutline
)

private val MyStoreLightColorScheme = lightColorScheme(
    primary = LightEmeraldPrimary,
    onPrimary = LightEmeraldOnPrimary,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = LightEmeraldOnContainer,
    secondary = LightCyanSecondary,
    onSecondary = LightCyanOnSecondary,
    secondaryContainer = LightCyanContainer,
    onSecondaryContainer = LightCyanOnContainer,
    tertiary = LightAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LightAmberContainer,
    onTertiaryContainer = LightAmberOnContainer,
    error = LightError,
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = PorcelainBg,
    onBackground = LightOnBg,
    surface = PorcelainSurface,
    onSurface = LightOnBg,
    surfaceVariant = PorcelainSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = PorcelainSurface,
    surfaceContainer = PorcelainSurfaceVariant,
    outline = PorcelainOutline
)

val MyStoreShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyStoreTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> MyStoreDarkColorScheme
        else -> MyStoreLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MyStoreShapes,
        content = content
    )
}
