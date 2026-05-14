package com.example.smartkrishi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenPrimary   = Color(0xFF1A5C2E)
val GreenLight     = Color(0xFF2D7A47)
val GreenSurface   = Color(0xFFE8F5E9)
val GreenChip      = Color(0xFFEAF3DE)
val PageBg         = Color(0xFFF4F6F0)
val CardWhite      = Color(0xFFFFFFFF)
val InputBg        = Color(0xFFF8FAF6)
val InputBorder    = Color(0xFFDCEDC8)
val TextPrimary    = Color(0xFF1A1A1A)
val TextSecondary  = Color(0xFF888888)
val TextHint       = Color(0xFFBBBBBB)
val AmberBg        = Color(0xFFFFF8E1)
val AmberBorder    = Color(0xFFFFE082)
val AmberText      = Color(0xFFBF6F00)
val BlueBg         = Color(0xFFE3F2FD)
val OrangeBg       = Color(0xFFFFF3E0)
val PurpleBg       = Color(0xFFF3E5F5)
val RedBtn         = Color(0xFFC62828)
val RedBg          = Color(0xFFFFEBEE)
val TrendUp        = Color(0xFF2E7D32)
val TrendDown      = Color(0xFFC62828)
val TrendFlat      = Color(0xFF888888)
val DividerColor   = Color(0xFFF0F0F0)
val BadgeBorder    = Color(0xFFFFCDD2)

private val SmartKrishiColors = lightColorScheme(
    primary   = GreenPrimary,
    secondary = GreenLight,
    background = PageBg,
    surface   = CardWhite,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SmartKrishiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SmartKrishiColors, content = content)
}
