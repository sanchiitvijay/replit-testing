package app.lastcalled.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = WarmWhite,
    primaryContainer = Sage,
    onPrimaryContainer = ForestDark,
    secondary = SoftGold,
    background = Mist,
    surface = WarmWhite,
    onSurface = Ink,
    onBackground = Ink,
    outline = Border,
)

private val DarkColors = darkColorScheme(
    primary = ColorTokens.ForestLight,
    onPrimary = ColorTokens.ForestDeep,
    primaryContainer = ColorTokens.ForestDeep,
    onPrimaryContainer = ColorTokens.Sage,
    secondary = ColorTokens.GoldLight,
)

@Composable
fun LastCalledTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

private object ColorTokens {
    val ForestLight = androidx.compose.ui.graphics.Color(0xFF8BC4A5)
    val ForestDeep = androidx.compose.ui.graphics.Color(0xFF163A2E)
    val Sage = androidx.compose.ui.graphics.Color(0xFFDCEADF)
    val GoldLight = androidx.compose.ui.graphics.Color(0xFFE7B66C)
}
