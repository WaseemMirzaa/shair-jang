package com.codetivelab.fieldcalc.ui.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.models.Quantity
import com.codetivelab.fieldcalc.domain.models.TrajectoryPoint
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.domain.units.UnitConverter
import com.codetivelab.fieldcalc.ui.components.RuggedButton
import com.codetivelab.fieldcalc.ui.theme.LcdBackground
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.TextMuted

const val TAG_TRAJECTORY_GRAPH = "trajectory_graph"

/**
 * Distance-vs-height plot. Pinch to zoom, drag to pan, RESET VIEW to go back to the full curve.
 *
 * Axes are drawn in screen space and the curve inside a clipped, transformed layer, so zooming
 * magnifies the trajectory without thickening the gridlines.
 */
@Composable
fun TrajectoryGraph(
    points: List<TrajectoryPoint>,
    unit: UnitSystem,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.graph_title),
            color = LcdGreenDim,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.graph_axes), color = TextMuted, style = MaterialTheme.typography.labelSmall)
            Text(stringResource(R.string.graph_hint), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .padding(top = 6.dp)
                .background(LcdBackground)
                .testTag(TAG_TRAJECTORY_GRAPH),
            contentAlignment = Alignment.Center
        ) {
            if (points.isEmpty()) {
                Text(
                    stringResource(R.string.graph_empty),
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            } else {
                GraphCanvas(points, scale, offset) { zoom, pan ->
                    scale = (scale * zoom).coerceIn(1f, 25f)
                    offset += pan
                }
                AxisLabels(points, unit, Modifier.fillMaxSize().padding(6.dp))
            }
        }

        if (points.isNotEmpty()) {
            RuggedButton(
                label = stringResource(R.string.btn_reset_view),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                tag = "graph_reset"
            ) { scale = 1f; offset = Offset.Zero }
        }
    }
}

@Composable
private fun GraphCanvas(
    points: List<TrajectoryPoint>,
    scale: Float,
    offset: Offset,
    onTransform: (zoom: Float, pan: Offset) -> Unit
) {
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ -> onTransform(zoom, pan) }
            }
    ) {
        val pad = 28f
        val maxX = points.maxOf { it.distanceM }.coerceAtLeast(1.0)
        val maxY = points.maxOf { it.heightM }
        val minY = points.minOf { it.heightM }
        val spanY = (maxY - minY).coerceAtLeast(1.0)

        fun px(d: Double) = pad + (d / maxX * (size.width - 2 * pad)).toFloat()
        fun py(h: Double) = size.height - pad - ((h - minY) / spanY * (size.height - 2 * pad)).toFloat()

        // Axes (screen space, never scaled).
        drawLine(LcdGreenDim, Offset(pad, size.height - pad), Offset(size.width - pad, size.height - pad), 2f)
        drawLine(LcdGreenDim, Offset(pad, pad), Offset(pad, size.height - pad), 2f)

        // Gridlines every 25% of the plot area.
        for (i in 1..3) {
            val y = pad + (size.height - 2 * pad) * i / 4f
            drawLine(LcdGreenDim.copy(alpha = 0.25f), Offset(pad, y), Offset(size.width - pad, y), 1f)
            val x = pad + (size.width - 2 * pad) * i / 4f
            drawLine(LcdGreenDim.copy(alpha = 0.25f), Offset(x, pad), Offset(x, size.height - pad), 1f)
        }

        // Zero-height reference, when the curve crosses it.
        if (minY < 0.0 && maxY > 0.0) {
            val zero = py(0.0)
            drawLine(LcdGreenDim.copy(alpha = 0.5f), Offset(pad, zero), Offset(size.width - pad, zero), 1f)
        }

        clipRect(pad, pad, size.width - pad, size.height - pad) {
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, scale, pivot = Offset(pad, size.height - pad))
            }) {
                val path = Path().apply {
                    moveTo(px(points.first().distanceM), py(points.first().heightM))
                    points.drop(1).forEach { lineTo(px(it.distanceM), py(it.heightM)) }
                }
                drawPath(path, LcdGreen, style = Stroke(width = 3f / scale))
            }
        }
    }
}

/** Corner readouts for the plotted extents, in the operator's unit system. */
@Composable
private fun AxisLabels(points: List<TrajectoryPoint>, unit: UnitSystem, modifier: Modifier) {
    val distanceLabel = UnitConverter.label(Quantity.DISTANCE, unit)
    val heightLabel = UnitConverter.label(Quantity.ALTITUDE, unit)
    val maxDistance = UnitConverter.format(
        UnitConverter.toDisplay(points.maxOf { it.distanceM }, Quantity.DISTANCE, unit), 0
    )
    val maxHeight = UnitConverter.format(
        UnitConverter.toDisplay(points.maxOf { it.heightM }, Quantity.ALTITUDE, unit), 1
    )
    val minHeight = UnitConverter.format(
        UnitConverter.toDisplay(points.minOf { it.heightM }, Quantity.ALTITUDE, unit), 1
    )

    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Text("$maxHeight $heightLabel", color = LcdGreenDim, style = MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$minHeight $heightLabel", color = LcdGreenDim, style = MaterialTheme.typography.labelSmall)
            Text("$maxDistance $distanceLabel", color = LcdGreenDim, style = MaterialTheme.typography.labelSmall)
        }
    }
}
