package io.github.watervxv.mtpadphotos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.watervxv.mtpadphotos.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = Gray400,
    tertiary = Gray600,
    background = BlackBackground,
    surface = Gray800,
    surfaceVariant = Color(0xFF333333),
    onBackground = WhiteText,
    onSurface = WhiteText,
    onSurfaceVariant = Gray400
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    secondary = Gray600,
    tertiary = Gray400,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = Color(0xFFEEEEEE),
    onBackground = OnLightText,
    onSurface = OnLightText,
    onSurfaceVariant = Gray600
)

@Composable
fun MTPadPhotosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
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
        content = content
    )
}
