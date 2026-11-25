package com.yourco.grainshot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import com.yourco.grainshot.R

// ── Tokens — Direction B Lo-Fi Warm ──────────────────────────────────────
val ColorBackground   = Color(0xFF120E0A)
val ColorSurface      = Color(0xFF1E1812)
val ColorSurfaceHigh  = Color(0xFF2E261C)
val ColorOnBackground = Color(0xFFEDE0C8)
val ColorOnSurfaceDim = Color(0xFF8A7A60)
val ColorAccent       = Color(0xFFE05030)
val ColorAccentDim    = Color(0xFF601A0A)
val ColorError        = Color(0xFFCC4444)
val ColorScrim        = Color(0xCC000000)

val ShareTechMono = FontFamily(
    Font(R.font.share_tech_mono, FontWeight.Normal)
)

private val GrainshotColorScheme = darkColorScheme(
    background       = ColorBackground,
    surface          = ColorSurface,
    surfaceVariant   = ColorSurfaceHigh,
    onBackground     = ColorOnBackground,
    onSurface        = ColorOnBackground,
    onSurfaceVariant = ColorOnSurfaceDim,
    primary          = ColorAccent,
    onPrimary        = Color.White,
    error            = ColorError,
)

val GrainshotTypography = Typography(
    displayLarge  = TextStyle(fontFamily = ShareTechMono, fontSize = 20.sp, letterSpacing = 0.05.em),
    bodyLarge     = TextStyle(fontFamily = ShareTechMono, fontSize = 14.sp, letterSpacing = 0.05.em),
    labelSmall    = TextStyle(fontFamily = ShareTechMono, fontSize = 11.sp, letterSpacing = 0.08.em),
)

@Composable
fun GrainshotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrainshotColorScheme,
        typography  = GrainshotTypography,
        content     = content,
    )
}
