package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PastelDarkColorScheme = darkColorScheme(
    primary = PastelLavender,
    onPrimary = Color(0xFF261833),
    primaryContainer = Color(0xFF382949),
    onPrimaryContainer = PastelLavender,
    
    secondary = PastelRose,
    onSecondary = Color(0xFF381A22),
    secondaryContainer = Color(0xFF4D2B35),
    onSecondaryContainer = PastelRose,
    
    tertiary = PastelMint,
    onTertiary = Color(0xFF0F3830),
    tertiaryContainer = Color(0xFF1E4D43),
    onTertiaryContainer = PastelMint,
    
    background = DarkBackground,
    onBackground = OnDarkText,
    
    surface = DarkSurface,
    onSurface = OnDarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkTextMuted,
    
    outline = DarkOutline
)

@Composable
fun AreYouOkayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PastelDarkColorScheme,
        typography = Typography,
        content = content
    )
}

