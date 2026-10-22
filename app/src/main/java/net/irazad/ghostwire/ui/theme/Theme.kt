package net.irazad.ghostwire.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import net.irazad.ghostwire.data.models.AppTheme

private val DarkShieldColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    secondary = DeepViolet,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkCardBorder
)

private val LightCyberColorScheme = lightColorScheme(
    primary = SharpCobalt,
    onPrimary = Color.White,
    secondary = DeepViolet,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder
)

@Composable
fun GhostWireTheme(
    selectedTheme: AppTheme = AppTheme.DARK_SHIELD,
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        AppTheme.DARK_SHIELD -> DarkShieldColorScheme
        AppTheme.LIGHT_CYBER -> LightCyberColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GhostWireTypography,
        content = content
    )
}
