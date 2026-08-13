package com.codetivelab.fieldcalc.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.models.EngineStatus
import com.codetivelab.fieldcalc.domain.models.Quantity
import com.codetivelab.fieldcalc.domain.models.SolveResult
import com.codetivelab.fieldcalc.domain.models.TrajectoryPoint
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.domain.units.UnitConverter
import com.codetivelab.fieldcalc.ui.calculator.CalculatorViewModel
import com.codetivelab.fieldcalc.ui.components.LcdPanel
import com.codetivelab.fieldcalc.ui.components.LcdRow
import com.codetivelab.fieldcalc.ui.components.RuggedButton
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.OliveLight
import com.codetivelab.fieldcalc.ui.theme.TextMuted
import com.codetivelab.fieldcalc.ui.theme.WarnRed
import kotlin.math.ceil

/** Stable test tags for the results screen. */
object ResultsTags {
    const val TITLE = "results_title"
    const val SUMMARY = "results_summary"
    const val TABLE = "results_table"
    const val BTN_TABLE = "results_btn_table"
    const val BTN_GRAPH = "results_btn_graph"
    const val BTN_SUMMARY = "results_btn_summary"
    const val BTN_NEW_INPUT = "results_btn_new_input"
    const val BTN_BACK = "results_btn_back"
}

private enum class ResultMode { SUMMARY, TABLE, GRAPH }

@Composable
fun ResultsScreen(vm: CalculatorViewModel, onNewInput: () -> Unit, onBack: () -> Unit) {
    val result by vm.result.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(ResultMode.SUMMARY) }

    val res = result
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp)
    ) {
        Text(
            stringResource(R.string.results_title),
            style = MaterialTheme.typography.titleLarge,
            color = LcdGreen,
            modifier = Modifier.testTag(ResultsTags.TITLE)
        )
        Spacer(Modifier.height(12.dp))

        if (res == null) {
            Text(stringResource(R.string.results_none), color = TextMuted)
            Spacer(Modifier.height(16.dp))
            RuggedButton(stringResource(R.string.btn_back), Modifier.fillMaxWidth(), tag = ResultsTags.BTN_BACK) { onBack() }
            return@Column
        }

        // Each pane owns its own scrolling, so the table can be a LazyColumn without nesting
        // two vertical scroll containers.
        Box(Modifier.weight(1f)) {
            when (mode) {
                ResultMode.SUMMARY -> SummaryPane(res, state.unit)
                ResultMode.TABLE -> TablePane(res, state.unit)
                ResultMode.GRAPH -> TrajectoryGraph(res.trajectory.points, state.unit, Modifier.fillMaxSize())
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.btn_table), Modifier.weight(1f), accent = mode == ResultMode.TABLE, tag = ResultsTags.BTN_TABLE) {
                mode = ResultMode.TABLE
            }
            RuggedButton(stringResource(R.string.btn_graph), Modifier.weight(1f), accent = mode == ResultMode.GRAPH, tag = ResultsTags.BTN_GRAPH) {
                mode = ResultMode.GRAPH
            }
            RuggedButton(stringResource(R.string.btn_summary), Modifier.weight(1f), accent = mode == ResultMode.SUMMARY, tag = ResultsTags.BTN_SUMMARY) {
                mode = ResultMode.SUMMARY
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.btn_new_input), Modifier.weight(1f), accent = true, tag = ResultsTags.BTN_NEW_INPUT) {
                vm.clearResult(); onNewInput()
            }
            RuggedButton(stringResource(R.string.btn_back), Modifier.weight(1f), tag = ResultsTags.BTN_BACK) { onBack() }
        }
    }
}

// -------------------------------------------------------------------------------------------
// Summary
// -------------------------------------------------------------------------------------------

@Composable
private fun SummaryPane(res: SolveResult, unit: UnitSystem) {
    val distanceUnit = UnitConverter.label(Quantity.DISTANCE, unit)
    val heightUnit = UnitConverter.label(Quantity.ALTITUDE, unit)
    val velocityUnit = UnitConverter.label(Quantity.VELOCITY, unit)
    val energyUnit = UnitConverter.label(Quantity.ENERGY, unit)
    val pressureUnit = UnitConverter.label(Quantity.PRESSURE, unit)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(ResultsTags.SUMMARY)
    ) {
        LcdPanel(Modifier.fillMaxWidth()) {
            LcdRow(
                stringResource(R.string.label_range),
                UnitConverter.format(UnitConverter.toDisplay(res.requestedRangeM, Quantity.DISTANCE, unit), 0),
                distanceUnit
            )
        }

        if (res.engineStatus == EngineStatus.INCOMPLETE && !res.trajectory.isEmpty) {
            Spacer(Modifier.height(8.dp))
            val reached = UnitConverter.format(
                UnitConverter.toDisplay(res.trajectory.maxDistanceM, Quantity.DISTANCE, unit), 0
            )
            Text(
                stringResource(R.string.engine_incomplete, "$reached $distanceUnit"),
                color = WarnRed,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.results_trajectory_data), style = MaterialTheme.typography.labelSmall, color = LcdGreenDim)
        Spacer(Modifier.height(6.dp))

        LcdPanel(Modifier.fillMaxWidth()) {
            val point = res.terminal
            if (point == null) {
                Text(stringResource(R.string.engine_not_loaded), color = WarnRed, style = MaterialTheme.typography.labelLarge)
            } else {
                LcdRow(
                    stringResource(R.string.results_height),
                    UnitConverter.format(UnitConverter.toDisplay(point.heightM, Quantity.ALTITUDE, unit), 2),
                    heightUnit
                )
                LcdRow(
                    stringResource(R.string.results_drift),
                    UnitConverter.format(UnitConverter.toDisplay(point.driftM, Quantity.ALTITUDE, unit), 2),
                    heightUnit
                )
                LcdRow(
                    stringResource(R.string.results_velocity),
                    UnitConverter.format(UnitConverter.toDisplay(point.velocityMs, Quantity.VELOCITY, unit), 1),
                    velocityUnit
                )
                LcdRow(stringResource(R.string.results_mach), UnitConverter.format(point.mach, 2))
                LcdRow(
                    stringResource(R.string.results_energy),
                    UnitConverter.format(UnitConverter.toDisplay(point.energyJ, Quantity.ENERGY, unit), 0),
                    energyUnit
                )
                LcdRow(stringResource(R.string.results_time), UnitConverter.format(point.timeS, 3), "s")
                LcdRow(
                    stringResource(R.string.results_apex),
                    UnitConverter.format(UnitConverter.toDisplay(res.trajectory.maxHeightM, Quantity.ALTITUDE, unit), 2),
                    heightUnit
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.results_atmosphere), style = MaterialTheme.typography.labelSmall, color = LcdGreenDim)
        Spacer(Modifier.height(6.dp))
        LcdPanel(Modifier.fillMaxWidth()) {
            LcdRow(
                stringResource(R.string.results_density),
                UnitConverter.format(res.atmosphere.densityKgM3, 4),
                stringResource(R.string.unit_density)
            )
            LcdRow(
                stringResource(R.string.results_sound_speed),
                UnitConverter.format(UnitConverter.toDisplay(res.atmosphere.speedOfSoundMs, Quantity.VELOCITY, unit), 1),
                velocityUnit
            )
            LcdRow(
                stringResource(R.string.results_pressure),
                UnitConverter.format(UnitConverter.toDisplay(res.atmosphere.pressureHpa, Quantity.PRESSURE, unit), 2),
                pressureUnit
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// -------------------------------------------------------------------------------------------
// Table
// -------------------------------------------------------------------------------------------

/** Round step (25/50/100/200 …) giving roughly ten rows for the requested range. */
internal fun tableStepM(rangeM: Double): Double {
    if (rangeM <= 0.0) return 100.0
    val candidates = listOf(10.0, 25.0, 50.0, 100.0, 200.0, 250.0, 500.0, 1000.0)
    val target = rangeM / 10.0
    return candidates.firstOrNull { it >= target } ?: ceil(target / 1000.0) * 1000.0
}

@Composable
private fun TablePane(res: SolveResult, unit: UnitSystem) {
    val rows = res.trajectory.sampleEvery(tableStepM(res.trajectory.maxDistanceM))

    Column(Modifier.fillMaxSize()) {
        Text(stringResource(R.string.table_title), style = MaterialTheme.typography.labelSmall, color = LcdGreenDim)
        Spacer(Modifier.height(6.dp))

        if (rows.isEmpty()) {
            LcdPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.table_empty), color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
            return@Column
        }

        // Horizontal scroll for the columns, vertical (lazy) scroll for the rows.
        Column(
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .testTag(ResultsTags.TABLE)
        ) {
            HeaderRow()
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows) { p -> TableRow(p, unit) }
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Row(Modifier.padding(vertical = 4.dp)) {
        listOf(
            R.string.table_col_range, R.string.table_col_height, R.string.table_col_drift,
            R.string.table_col_velocity, R.string.table_col_mach, R.string.table_col_energy,
            R.string.table_col_time
        ).forEach { res ->
            Box(Modifier.width(84.dp)) {
                Text(
                    stringResource(res),
                    color = OliveLight,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TableRow(p: TrajectoryPoint, unit: UnitSystem) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Cell(UnitConverter.format(UnitConverter.toDisplay(p.distanceM, Quantity.DISTANCE, unit), 0))
        Cell(UnitConverter.format(UnitConverter.toDisplay(p.heightM, Quantity.ALTITUDE, unit), 2))
        Cell(UnitConverter.format(UnitConverter.toDisplay(p.driftM, Quantity.ALTITUDE, unit), 2))
        Cell(UnitConverter.format(UnitConverter.toDisplay(p.velocityMs, Quantity.VELOCITY, unit), 1))
        Cell(UnitConverter.format(p.mach, 2))
        Cell(UnitConverter.format(UnitConverter.toDisplay(p.energyJ, Quantity.ENERGY, unit), 0))
        Cell(UnitConverter.format(p.timeS, 3))
    }
}

@Composable
private fun Cell(text: String) {
    Box(Modifier.width(84.dp)) {
        Text(text, color = LcdGreen, style = MaterialTheme.typography.bodyMedium)
    }
}
