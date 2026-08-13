package com.codetivelab.fieldcalc.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.ui.components.Key
import com.codetivelab.fieldcalc.ui.components.Keypad
import com.codetivelab.fieldcalc.ui.components.LcdPanel
import com.codetivelab.fieldcalc.ui.theme.LcdBackground
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.Olive
import com.codetivelab.fieldcalc.ui.theme.OliveLight
import com.codetivelab.fieldcalc.ui.theme.PanelBlack
import com.codetivelab.fieldcalc.ui.theme.TextMuted
import com.codetivelab.fieldcalc.ui.theme.WarnRed

/** Stable test tags for the operator screen. */
object CalculatorTags {
    const val STATUS = "calc_status"
    const val PROFILE_NAME = "calc_profile_name"
    const val DEMO_BADGE = "calc_demo_badge"
    fun field(field: Field) = "calc_field_${field.name}"
    fun value(field: Field) = "calc_value_${field.name}"
}

@Composable
fun CalculatorScreen(
    onOpenResults: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    vm: CalculatorViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // One-shot: only a completed SOLVE navigates, so coming BACK here never bounces forward again.
    LaunchedEffect(Unit) { vm.solved.collect { onOpenResults() } }

    var menuOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        // ---- Header ----
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.title_main),
                    style = MaterialTheme.typography.titleLarge,
                    color = LcdGreen
                )
                Text(
                    stringResource(R.string.subtitle_profile),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            if (state.isDemo) DemoBadge()
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu), tint = LcdGreen)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_select_profile)) },
                        onClick = { menuOpen = false; onOpenProfiles() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_settings)) },
                        onClick = { menuOpen = false; onOpenSettings() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_admin)) },
                        onClick = { menuOpen = false; onOpenAdmin() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_reset)) },
                        onClick = { menuOpen = false; vm.reset() }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Input panel ----
        LcdPanel(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.section_input_data),
                style = MaterialTheme.typography.labelSmall,
                color = LcdGreenDim
            )
            Spacer(Modifier.height(8.dp))
            FieldRow(stringResource(R.string.label_range), Field.RANGE, state, vm)
            FieldRow(stringResource(R.string.label_wind_speed), Field.WIND, state, vm)
            FieldRow(stringResource(R.string.label_wind_direction), Field.DIRECTION, state, vm)
            FieldRow(stringResource(R.string.label_temperature), Field.TEMPERATURE, state, vm)
            FieldRow(stringResource(R.string.label_altitude), Field.ALTITUDE, state, vm)
            FieldRow(stringResource(R.string.label_humidity), Field.HUMIDITY, state, vm)
            FieldRow(stringResource(R.string.label_inclination), Field.INCLINATION, state, vm)
        }

        // ---- Error / status line ----
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(state.error ?: state.status),
            color = if (state.error != null) WarnRed else OliveLight,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag(CalculatorTags.STATUS)
        )

        Spacer(Modifier.height(12.dp))

        // ---- Keypad ----
        Keypad(onKey = { key -> if (key == Key.Menu) menuOpen = true else vm.onKey(key) })

        Spacer(Modifier.height(14.dp))

        // ---- Footer ----
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PanelBlack)
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_profile), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = state.profile?.name ?: stringResource(R.string.profile_none),
                    color = LcdGreen,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag(CalculatorTags.PROFILE_NAME)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.label_status), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = stringResource(if (state.locked) R.string.status_locked else R.string.status_ready),
                    color = OliveLight,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, field: Field, state: CalcUiState, vm: CalculatorViewModel) {
    val selected = state.selected == field
    val unit = when (field) {
        Field.DIRECTION -> stringResource(R.string.unit_oclock)
        Field.HUMIDITY -> stringResource(R.string.unit_percent)
        Field.INCLINATION -> stringResource(R.string.unit_degree)
        else -> vm.unitLabel(field).orEmpty()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag(CalculatorTags.field(field))
            .clickable { vm.select(field) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = LcdGreenDim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .widthIn(min = 96.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LcdBackground)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) LcdGreen else Olive,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            val value = vm.displayValue(field)
            Text(
                text = if (selected) "${value}_" else value,
                color = LcdGreen,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.testTag(CalculatorTags.value(field))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = unit,
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.widthIn(min = 48.dp)
        )
    }
}

@Composable
private fun DemoBadge() {
    Box(
        Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Olive)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag(CalculatorTags.DEMO_BADGE)
    ) {
        Text(stringResource(R.string.badge_demo), color = LcdGreen, style = MaterialTheme.typography.labelSmall)
    }
}
