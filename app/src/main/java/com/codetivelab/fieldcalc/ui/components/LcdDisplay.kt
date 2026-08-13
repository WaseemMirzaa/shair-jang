package com.codetivelab.fieldcalc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.codetivelab.fieldcalc.ui.theme.LcdBackground
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.OliveDark

/** Monochrome-green LCD panel wrapper. */
@Composable
fun LcdPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LcdBackground)
            .border(BorderStroke(2.dp, OliveDark), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) { content() }
}

/** A labelled value row rendered in LCD style: "LABEL .......... VALUE unit". */
@Composable
fun LcdRow(label: String, value: String, unit: String = "", modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LcdGreenDim, style = MaterialTheme.typography.bodyMedium)
        Text(
            buildString { append(value); if (unit.isNotEmpty()) append(" $unit") },
            color = LcdGreen,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
