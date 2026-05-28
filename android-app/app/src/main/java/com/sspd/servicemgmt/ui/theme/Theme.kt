package com.sspd.servicemgmt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary      = Primary,
    background   = ScreenBg,
    surface      = CardBg,
    onPrimary    = Color.White,
    onBackground = TextMain,
    onSurface    = TextMain,
    outline      = BorderColor,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColorScheme, content = content)
}
