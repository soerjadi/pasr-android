package id.oemah.nusantara.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1B5E20)
private val GreenContainer = Color(0xFFE3F1E4)
private val Amber = Color(0xFFFFC107)
private val AmberContainer = Color(0xFFFFF3CD)
private val Background = Color(0xFFF9FBF7)

private val NusantaraColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Color(0xFF0B3010),
    secondary = Amber,
    onSecondary = Color(0xFF3A2E00),
    secondaryContainer = AmberContainer,
    onSecondaryContainer = Color(0xFF3A2E00),
    background = Background,
    surface = Color.White,
    onBackground = Color(0xFF1A1C19),
    onSurface = Color(0xFF1A1C19),
)

@Composable
fun NusantaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NusantaraColors, content = content)
}
