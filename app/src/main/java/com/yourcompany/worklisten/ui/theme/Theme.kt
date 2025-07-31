package com.yourcompany.worklisten.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yourcompany.worklisten.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource

private val AccentDark = Color(0xFF905D5D)
private val AccentLight = Color(0xFFE9B384)
private val BackgroundCream = Color(0xFFF5F5DC)
private val TextDark = Color(0xFF333333)
private val CardBackground = Color(0xFFFFFBF5)
private val White = Color.White
private val Black = Color.Black

private val LightColorScheme = lightColorScheme(
    primary = AccentDark,
    onPrimary = White,
    primaryContainer = AccentLight,
    onPrimaryContainer = Black,
    secondary = AccentLight,
    onSecondary = Black,
    background = BackgroundCream,
    onBackground = TextDark,
    surface = CardBackground,
    onSurface = TextDark,
    onSurfaceVariant = Color(0xFF757575) // 为词性添加一个明确的颜色
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentLight,
    onPrimary = Black,
    primaryContainer = AccentDark,
    onPrimaryContainer = White,
    secondary = AccentLight,
    onSecondary = Black
)

@Composable
fun WorkListenTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
