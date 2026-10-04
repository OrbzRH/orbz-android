package app.orbz.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The console's palette: ink, marble, brass and one ember. */
object Ink {
    val bg = Color(0xFF141312)
    val bg2 = Color(0xFF1C1A18)
    val night = Color(0xFF0F0E0D)
    val marbleLight = Color(0xFFFBF0E3)
    val marble = Color(0xFFE2D0C2)
    val vein = Color(0xFFCCB098)
    val dust = Color(0xFF8A7B6E)
    val brass = Color(0xFFBD8E6B)
    val ember = Color(0xFFFF7A1A)
    val emberGlow = Color(0xFFF18E3F)
    val line = Color(0x47BD8E6B)
}

val Display = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, color = Ink.marbleLight)
val Label = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp, color = Ink.brass)
val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Ink.marble)

@Composable
fun OrbzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.marbleLight, onPrimary = Ink.bg,
            secondary = Ink.ember, onSecondary = Ink.bg,
            tertiary = Ink.brass,
            background = Ink.bg, onBackground = Ink.marbleLight,
            surface = Ink.bg, onSurface = Ink.marbleLight,
            surfaceVariant = Ink.bg2, onSurfaceVariant = Ink.vein,
            surfaceContainer = Ink.bg2, surfaceContainerHigh = Ink.bg2, surfaceContainerLow = Ink.night,
            outline = Ink.line, outlineVariant = Ink.line,
            error = Ink.emberGlow, onError = Ink.bg,
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = Ink.marble),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = Ink.marble),
            bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp, color = Ink.dust),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Ink.marbleLight),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink.marbleLight),
            labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        ),
        content = content,
    )
}
