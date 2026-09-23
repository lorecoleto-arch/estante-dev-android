package com.estante.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta "papel e couro". Os pares cor/"on-cor" seguem os tons do Material 3,
// que garantem contraste mínimo de 4.5:1 para texto (WCAG AA).
private val LightColors = lightColorScheme(
    primary = Color(0xFF8B4F24),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF755848),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF2B160A),
    tertiary = Color(0xFF5F6135),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE5E6AF),
    onTertiaryContainer = Color(0xFF1C1D00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceDim = Color(0xFFE7D7CF),
    surfaceBright = Color(0xFFFFF8F5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFCEAE3),
    surfaceContainerHigh = Color(0xFFF6E5DD),
    surfaceContainerHighest = Color(0xFFF0DFD7),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD7C2B8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF6F3810),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE5BFA9),
    onSecondary = Color(0xFF432B1D),
    secondaryContainer = Color(0xFF5C4132),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFFC9CA95),
    onTertiary = Color(0xFF31320B),
    tertiaryContainer = Color(0xFF47491F),
    onTertiaryContainer = Color(0xFFE5E6AF),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFF0DFD7),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFF0DFD7),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
    surfaceDim = Color(0xFF1A120D),
    surfaceBright = Color(0xFF423731),
    surfaceContainerLowest = Color(0xFF140C08),
    surfaceContainerLow = Color(0xFF221A15),
    surfaceContainer = Color(0xFF271E19),
    surfaceContainerHigh = Color(0xFF322823),
    surfaceContainerHighest = Color(0xFF3D332D),
    outline = Color(0xFF9F8D84),
    outlineVariant = Color(0xFF52443C),
)

@Composable
fun EstanteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
