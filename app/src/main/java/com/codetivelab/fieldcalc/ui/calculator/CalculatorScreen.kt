package com.codetivelab.fieldcalc.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.models.Quantity
import com.codetivelab.fieldcalc.domain.models.SolveResult
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.domain.units.UnitConverter
import com.codetivelab.fieldcalc.ui.components.BodyStat
import com.codetivelab.fieldcalc.ui.components.DeviceBody
import com.codetivelab.fieldcalc.ui.components.DeviceTitle
import com.codetivelab.fieldcalc.ui.components.Key
import com.codetivelab.fieldcalc.ui.components.Keypad
import com.codetivelab.fieldcalc.ui.components.LcdBigReadout
import com.codetivelab.fieldcalc.ui.components.LcdDataRow
import com.codetivelab.fieldcalc.ui.components.LcdDivider
import com.codetivelab.fieldcalc.ui.components.LcdHeader
import com.codetivelab.fieldcalc.ui.components.LcdScreen
import com.codetivelab.fieldcalc.ui.components.LcdStatRow
import com.codetivelab.fieldcalc.ui.components.LcdStatusBar
import com.codetivelab.fieldcalc.ui.theme.LcdInk
import com.codetivelab.fieldcalc.ui.theme.LcdPositive
import com.codetivelab.fieldcalc.ui.theme.Olive
import com.codetivelab.fieldcalc.ui.theme.PanelBlack
import com.codetivelab.fieldcalc.ui.theme.TextMuted
import com.codetivelab.fieldcalc.ui.theme.WarnRed

/** Stable test tags for the operator screen. */
object CalculatorTags {
    const val STATUS = "calc_status"
    const val PROFILE_NAME = "calc_profile_name"
    const val DEMO_BADGE = "calc_demo_badge"
    const val OUTPUT = "calc_output_panel"
    const val OUT_HEIGHT = "calc_out_height"
    const val OUT_DRIFT = "calc_out_drift"
    fun field(field: Field) = "calc_field_${field.name}"
    fun value(field: Field) = "calc_value_${field.name}"
}

private fun iconFor(field: Field): ImageVector = when (field) {
    Field.RANGE -> Icons.Filled.GpsFixed
    Field.WIND -> Icons.Filled.Air
    Field.DIRECTION -> Icons.Filled.Explore
    Field.TEMPERATURE -> Icons.Filled.Thermostat
    Field.ALTITUDE -> Icons.Filled.Terrain
    Field.HUMIDITY -> Icons.Filled.WaterDrop
    Field.INCLINATION -> Icons.Filled.Architecture
}

private fun labelFor(field: Field): Int = when (field) {
    Field.RANGE -> R.string.label_range
    Field.WIND -> R.string.label_wind_speed
    Field.DIRECTION -> R.string.label_wind_direction
    Field.TEMPERATURE -> R.string.label_temperature
    Field.ALTITUDE -> R.string.label_altitude
    Field.HUMIDITY -> R.string.label_humidity
    Field.INCLINATION -> R.string.label_inclination
}

/**
 * The whole instrument on one screen: input column on the left of the glass, simulation output on
 * the right, profile strip engraved underneath, keypad below. Nothing here scrolls on a normal
 * phone — the display takes the space the keypad does not.
 */
@Composable
fun CalculatorScreen(
    onOpenResults: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    vm: CalculatorViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(10.dp)
    ) {
        DeviceBody(Modifier.fillMaxSize()) {

            DeviceTitle(
                title = stringResource(R.string.title_main),
                subtitle = stringResource(R.string.subtitle_device)
            )
            Spacer(Modifier.height(8.dp))

            // ---- The glass -------------------------------------------------------------
            LcdScreen(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 250.dp)
            ) {
                Row(Modifier.fillMaxSize()) {
                    InputColumn(state, vm, Modifier.weight(1.15f))
                    LcdDivider(vertical = true, modifier = Modifier.padding(horizontal = 7.dp))
                    OutputColumn(
                        result = result,
                        unit = state.unit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(CalculatorTags.OUTPUT)
                            .clickable(enabled = result != null) { onOpenResults() }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            ProfileStrip(state)
            Spacer(Modifier.height(8.dp))

            // ---- Keypad ----------------------------------------------------------------
            Box {
                Keypad(onKey = { key -> if (key == Key.Menu) menuOpen = true else vm.onKey(key) })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_results)) },
                        onClick = { menuOpen = false; onOpenResults() }
                    )
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

            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.tagline),
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Left half of the glass
// ---------------------------------------------------------------------------------------------

@Composable
private fun InputColumn(state: CalcUiState, vm: CalculatorViewModel, modifier: Modifier) {
    Column(modifier.fillMaxHeight()) {
        LcdHeader(stringResource(R.string.section_input_data))
        Spacer(Modifier.height(4.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Field.entries.forEach { field ->
                val selected = state.selected == field
                val shown = vm.displayValue(field)
                LcdDataRow(
                    icon = iconFor(field),
                    label = stringResource(labelFor(field)),
                    value = if (selected) "${shown}_" else shown,
                    unit = unitLabel(field, vm),
                    selected = selected,
                    valueTag = CalculatorTags.value(field),
                    modifier = Modifier
                        .testTag(CalculatorTags.field(field))
                        .clickable { vm.select(field) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        LcdStatusBar(
            text = stringResource(state.error ?: state.status),
            tint = if (state.error != null) WarnRed else LcdPositive,
            modifier = Modifier.testTag(CalculatorTags.STATUS)
        )
    }
}

@Composable
private fun unitLabel(field: Field, vm: CalculatorViewModel): String = when (field) {
    Field.DIRECTION -> stringResource(R.string.unit_oclock)
    Field.HUMIDITY -> stringResource(R.string.unit_percent)
    Field.INCLINATION -> stringResource(R.string.unit_degree)
    else -> vm.unitLabel(field).orEmpty()
}

// ---------------------------------------------------------------------------------------------
// Right half of the glass
// ---------------------------------------------------------------------------------------------

@Composable
private fun OutputColumn(result: SolveResult?, unit: UnitSystem, modifier: Modifier) {
    val blank = stringResource(R.string.placeholder_value)
    val point = result?.terminal

    fun show(value: Double?, quantity: Quantity?, decimals: Int): String {
        if (value == null) return blank
        val display = if (quantity == null) value else UnitConverter.toDisplay(value, quantity, unit)
        return UnitConverter.format(display, decimals)
    }

    Column(modifier.fillMaxHeight()) {
        LcdHeader(stringResource(R.string.section_output))
        Spacer(Modifier.height(6.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            LcdBigReadout(
                label = stringResource(R.string.results_height),
                value = show(point?.heightM, Quantity.ALTITUDE, 2),
                unit = UnitConverter.label(Quantity.ALTITUDE, unit),
                valueTag = CalculatorTags.OUT_HEIGHT
            )
            Spacer(Modifier.height(6.dp))
            LcdBigReadout(
                label = stringResource(R.string.table_col_drift),
                value = show(point?.driftM, Quantity.ALTITUDE, 2),
                unit = UnitConverter.label(Quantity.ALTITUDE, unit),
                valueTag = CalculatorTags.OUT_DRIFT
            )

            Spacer(Modifier.height(8.dp))
            LcdDivider()
            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.section_flight_data),
                color = LcdInk,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(2.dp))
            LcdStatRow(
                stringResource(R.string.results_velocity),
                show(point?.velocityMs, Quantity.VELOCITY, 1)
            )
            LcdStatRow(stringResource(R.string.results_mach), show(point?.mach, null, 2))
            LcdStatRow(
                stringResource(R.string.results_energy),
                show(point?.energyJ, Quantity.ENERGY, 0)
            )
            LcdStatRow(stringResource(R.string.results_time), show(point?.timeS, null, 3))
            LcdStatRow(
                stringResource(R.string.results_density),
                show(result?.atmosphere?.densityKgM3, null, 3)
            )
        }

        Spacer(Modifier.height(4.dp))
        LcdStatusBar(
            text = stringResource(
                if (result == null) R.string.hint_press_solve else R.string.hint_tap_detail
            )
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Engraved profile strip
// ---------------------------------------------------------------------------------------------

@Composable
private fun ProfileStrip(state: CalcUiState) {
    val profile = state.profile
    val blank = stringResource(R.string.placeholder_value)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PanelBlack)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.subtitle_profile),
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            if (state.isDemo) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Olive)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag(CalculatorTags.DEMO_BADGE)
                ) {
                    Text(
                        stringResource(R.string.badge_demo),
                        color = LcdPositive,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_profile),
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        modifier = Modifier.width(74.dp)
                    )
                    Text(
                        text = profile?.name ?: stringResource(R.string.profile_none),
                        color = LcdPositive,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        modifier = Modifier.testTag(CalculatorTags.PROFILE_NAME)
                    )
                }
                BodyStat(
                    stringResource(R.string.profile_calibre),
                    profile?.let { "${UnitConverter.format(it.projectile.calibreMm, 2)} mm" } ?: blank
                )
                BodyStat(
                    stringResource(R.string.profile_mass),
                    profile?.let { "${UnitConverter.format(it.projectile.massKg, 3)} kg" } ?: blank
                )
            }
            Column(Modifier.weight(1f)) {
                BodyStat(
                    stringResource(R.string.profile_drag),
                    profile?.projectile?.dragModelId ?: blank
                )
                BodyStat(
                    stringResource(R.string.profile_launch),
                    profile?.let {
                        val v = UnitConverter.toDisplay(
                            it.projectile.nominalLaunchVelocityMs, Quantity.VELOCITY, state.unit
                        )
                        "${UnitConverter.format(v, 0)} ${UnitConverter.label(Quantity.VELOCITY, state.unit)}"
                    } ?: blank
                )
                BodyStat(
                    stringResource(R.string.profile_units),
                    stringResource(
                        if (state.unit == UnitSystem.METRIC) R.string.settings_metric
                        else R.string.settings_imperial
                    )
                )
            }
        }
    }
}
