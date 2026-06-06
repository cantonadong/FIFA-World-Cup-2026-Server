package com.carldong.fifa.worldcup2026.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary          = Blue,
    onPrimary        = WCSurface,
    secondary        = Green,
    onSecondary      = WCSurface,
    error            = Red,
    background       = Bg,
    onBackground     = Label1,
    surface          = WCSurface,
    onSurface        = Label1,
    surfaceVariant   = SurfaceElevated,
    onSurfaceVariant = Label2,
    outline          = Separator,
)

@Composable
fun WC2026Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}

