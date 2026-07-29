package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = PastelDarkColorScheme,
        typography = Typography,
        content = content
    )
}

