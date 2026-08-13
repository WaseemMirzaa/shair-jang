package com.codetivelab.fieldcalc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.codetivelab.fieldcalc.domain.models.AppTheme

private fun schemeFor(theme: AppTheme) = when (theme) {
    AppTheme.AMBER -> darkColorScheme(
        primary = AmberLcd, onPrimary = RuggedBlack, secondary = OliveLight,
        background = RuggedBlack, onBackground = AmberLcd,
        surface = PanelBlack, onSurface = TextPrimary, error = WarnRed
    )
    AppTheme.HIGH_CONTRAST -> darkColorScheme(
        primary = LcdGreen, onPrimary = RuggedBlack, secondary = OliveLight,
        background = androidx.compose.ui.graphics.Color.Black, onBackground = LcdGreen,
        surface = androidx.compose.ui.graphics.Color(0xFF0A0A0A), onSurface = androidx.compose.ui.graphics.Color.White,
        error = WarnRed
    )
    else -> darkColorScheme(
        primary = LcdGreen, onPrimary = RuggedBlack, secondary = OliveLight,
        background = RuggedBlack, onBackground = TextPrimary,
        surface = PanelBlack, onSurface = TextPrimary, error = WarnRed
    )
}

@Composable
fun FieldCalcTheme(theme: AppTheme = AppTheme.RUGGED_DARK, content: @Composable () -> Unit) {
    // Intentionally ignores dynamic color: the instrument look must stay consistent across devices.
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = schemeFor(theme), typography = Typography, content = content)
}
