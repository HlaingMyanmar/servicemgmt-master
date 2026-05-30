package com.sspd.servicemgmt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = Color.White,
    primaryContainer   = PrimaryLight,
    onPrimaryContainer = Primary,
    secondary          = Violet,
    onSecondary        = Color.White,
    background         = ScreenBg,
    onBackground       = TextMain,
    surface            = CardBg,
    onSurface          = TextMain,
    surfaceVariant     = ScreenBg,
    outline            = BorderColor,
    error              = Danger,
    onError            = Color.White,
)

private val AppTypography = Typography(
    displaySmall  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.5).sp, color = TextMain),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.3).sp, color = TextMain),
    headlineSmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 18.sp, color = TextMain),
    titleLarge     = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 16.sp, color = TextMain),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, color = TextMain),
    titleSmall     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 13.sp, color = TextMain),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp, color = TextMain),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp, color = TextMain),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp, color = TextMuted),
    labelLarge     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 13.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 10.sp, color = TextMuted),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
