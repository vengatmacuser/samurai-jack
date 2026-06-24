package com.thigazhini_labs.samuraijack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Primary Colors - Samurai Jack Theme
private val PrimaryDark = Color(0xFF1A1A1A)
private val PrimaryLight = Color(0xFFEDEFF4)
private val SecondaryDark = Color(0xFFD4AF37) // Gold accent
private val SecondaryLight = Color(0xFF1A1A1A)
private val TertiaryDark = Color(0xFF00CED1) // Cyan accent
private val TertiaryLight = Color(0xFF1A1A1A)

// Surface Colors
private val SurfaceDark = Color(0xFF121212)
private val SurfaceVariantDark = Color(0xFF2C2C2C)

// Error Colors
private val ErrorDark = Color(0xFFCF6679)
private val ErrorLight = Color(0xFFB3261E)

private val DarkColorScheme = darkColorScheme(
    primary = SecondaryDark,
    onPrimary = PrimaryDark,
    primaryContainer = Color(0xFF332200),
    onPrimaryContainer = Color(0xFFFFDDB2),
    secondary = SecondaryLight,
    onSecondary = PrimaryDark,
    secondaryContainer = Color(0xFF332200),
    onSecondaryContainer = Color(0xFFFFDDB2),
    tertiary = TertiaryDark,
    onTertiary = PrimaryDark,
    tertiaryContainer = Color(0xFF004E4F),
    onTertiaryContainer = Color(0xFF4FFFFF),
    error = ErrorDark,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = PrimaryDark,
    onBackground = Color(0xFFEDEFF4),
    surface = SurfaceDark,
    onSurface = Color(0xFFEDEFF4),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFC6C7CC),
    outline = Color(0xFF8F9097),
    outlineVariant = Color(0xFF49454E),
    scrim = Color(0xFF000000)
)

@Composable
fun SamuraiJackTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SamuraiJackTypography,
        content = content
    )
}
