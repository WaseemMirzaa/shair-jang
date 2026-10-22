package com.codetivelab.fieldcalc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codetivelab.fieldcalc.ui.theme.BezelOlive
import com.codetivelab.fieldcalc.ui.theme.BezelOliveDark
import com.codetivelab.fieldcalc.ui.theme.BodyBlack
import com.codetivelab.fieldcalc.ui.theme.LcdHeaderBar
import com.codetivelab.fieldcalc.ui.theme.LcdInk
import com.codetivelab.fieldcalc.ui.theme.LcdInkDim
import com.codetivelab.fieldcalc.ui.theme.LcdPositive
import com.codetivelab.fieldcalc.ui.theme.LcdPositiveEdge
import com.codetivelab.fieldcalc.ui.theme.ScreenWell
import com.codetivelab.fieldcalc.ui.theme.TextMuted
import com.codetivelab.fieldcalc.ui.theme.TextPrimary

/**
 * The instrument chrome: olive outer shell, black body, and the pale-glass LCD inside it.
 *
 * Everything here is presentation only — the pieces are deliberately small so the main screen
 * reads as a layout description rather than a pile of modifiers.
 */

/** Olive-edged black body that everything else sits inside. */
@Composable
fun DeviceBody(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(BezelOliveDark)
            .padding(5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BodyBlack)
            .border(BorderStroke(1.dp, BezelOlive.copy(alpha = 0.55f)), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        content = content
    )
}

/** Engraved title block above the glass. */
@Composable
fun DeviceTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

/** The LCD itself: dark well, pale glass, thin inner edge. */
@Composable
fun LcdScreen(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ScreenWell)
            .padding(6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(LcdPositive)
            .border(BorderStroke(1.dp, LcdPositiveEdge), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) { content() }
}

/** Inverted header strip that titles a column of the display ("INPUT DATA"). */
@Composable
fun LcdHeader(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(LcdHeaderBar)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = LcdPositive,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp
        )
    }
}

/** One "icon · LABEL : value unit" line of the input column. */
@Composable
fun LcdDataRow(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    valueTag: String? = null
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) LcdInk else LcdInkDim,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = LcdInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ":",
            color = LcdInkDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            color = LcdInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(54.dp)
                .then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = unit,
            color = LcdInkDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier.width(42.dp)
        )
    }
}

/** A large segment-style readout, the way the solution column shows its headline numbers. */
@Composable
fun LcdBigReadout(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueTag: String? = null
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = LcdInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = LcdInk,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 34.sp,
                letterSpacing = 1.sp,
                modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = unit,
                color = LcdInkDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

/** "LABEL : value" line used under the headline numbers and in the profile strip. */
@Composable
fun LcdStatRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LcdInkDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Text(
            text = value,
            color = LcdInk,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp
        )
    }
}

/** Hairline used between the two halves of the display and under the headline numbers. */
@Composable
fun LcdDivider(vertical: Boolean = false, modifier: Modifier = Modifier) {
    val line = LcdInkDim.copy(alpha = 0.45f)
    if (vertical) {
        Box(modifier.width(1.dp).fillMaxHeight().background(line))
    } else {
        Box(modifier.fillMaxWidth().height(1.dp).background(line))
    }
}

/** Inverted strip at the foot of a column — the display's own status line. */
@Composable
fun LcdStatusBar(text: String, tint: Color = LcdPositive, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(LcdHeaderBar)
            .padding(vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = tint,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

/** Small label/value pair for the engraved profile strip under the screen. */
@Composable
fun BodyStat(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            text = label,
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.width(74.dp)
        )
        Text(":", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
        Spacer(Modifier.width(5.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            maxLines = 1
        )
    }
}
