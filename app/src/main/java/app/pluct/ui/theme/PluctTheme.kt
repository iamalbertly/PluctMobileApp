package app.pluct.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PluctBrandPurple,
    onPrimary = Color.White,
    primaryContainer = PluctBrandPurple.copy(alpha = 0.35f),
    onPrimaryContainer = Color.White,
    secondary = PluctBrandCyan,
    onSecondary = Color(0xFF0A0A0F),
    secondaryContainer = PluctMockSurfaceElevated,
    onSecondaryContainer = PluctOnDarkMuted,
    tertiary = Pink80,
    onTertiary = Color.Black,
    background = PluctMockSurface,
    onBackground = Color.White,
    surface = PluctMockSurfaceElevated,
    onSurface = Color.White,
    surfaceVariant = PluctMockSurfaceElevated,
    onSurfaceVariant = PluctOnDarkMuted,
    outline = PluctBrandPurple.copy(alpha = 0.35f),
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
    errorContainer = Color(0xFF3D1515),
    onErrorContainer = Color(0xFFFFD6D6)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun PluctTheme(
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
