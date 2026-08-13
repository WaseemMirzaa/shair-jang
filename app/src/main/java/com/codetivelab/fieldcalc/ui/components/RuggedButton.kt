package com.codetivelab.fieldcalc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codetivelab.fieldcalc.ui.theme.KeyEdge
import com.codetivelab.fieldcalc.ui.theme.KeyFace
import com.codetivelab.fieldcalc.ui.theme.KeyHighlight
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.Olive
import com.codetivelab.fieldcalc.ui.theme.TextPrimary

/**
 * A chunky, tactile-looking key: raised face over a dark edge that collapses when pressed.
 *
 * [accent] gives the big SOLVE / selected keys their olive-and-green treatment.
 * [tag] is a stable test tag — labels are localized, so UI tests address keys by tag instead.
 */
@Composable
fun RuggedButton(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
    tag: String? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val topColor = if (accent) Olive else KeyFace
    val edge = if (pressed) topColor else KeyEdge
    val faceBrush = Brush.verticalGradient(listOf(if (accent) Olive else KeyHighlight, topColor))

    Column(
        modifier = modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(edge)
            .padding(bottom = if (pressed) 0.dp else 3.dp)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(faceBrush)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (accent) LcdGreen else TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
