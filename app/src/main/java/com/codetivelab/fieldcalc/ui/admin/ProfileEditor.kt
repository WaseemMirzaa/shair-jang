package com.codetivelab.fieldcalc.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.engine.DragModels
import com.codetivelab.fieldcalc.domain.models.EnvironmentDefaults
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.ProjectileSpec
import com.codetivelab.fieldcalc.domain.models.ReferenceGeometry
import com.codetivelab.fieldcalc.ui.components.RuggedButton
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.TextMuted

@Composable
fun ProfileEditor(
    initial: Profile,
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial.name) }
    var locked by remember { mutableStateOf(initial.isLocked) }
    var demo by remember { mutableStateOf(initial.isDemo) }

    var calibre by remember { mutableStateOf(initial.projectile.calibreMm.str()) }
    var mass by remember { mutableStateOf(initial.projectile.massKg.str()) }
    var length by remember { mutableStateOf(initial.projectile.lengthM.str()) }
    var launchVelocity by remember { mutableStateOf(initial.projectile.nominalLaunchVelocityMs.str()) }
    var dragModel by remember { mutableStateOf(initial.projectile.dragModelId) }

    var refHeight by remember { mutableStateOf(initial.geometry.referenceHeightM.str()) }
    var refDist by remember { mutableStateOf(initial.geometry.referenceDistanceM.str()) }

    var temp by remember { mutableStateOf(initial.environmentDefaults.temperatureC.str()) }
    var humidity by remember { mutableStateOf(initial.environmentDefaults.humidityPct.str()) }
    var altitude by remember { mutableStateOf(initial.environmentDefaults.altitudeM.str()) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(
            stringResource(if (initial.id == 0L) R.string.editor_new else R.string.editor_edit),
            style = MaterialTheme.typography.titleLarge,
            color = LcdGreen
        )
        Spacer(Modifier.height(12.dp))

        Field(stringResource(R.string.editor_name), name) { name = it }

        Section(stringResource(R.string.editor_section_projectile))
        Field(stringResource(R.string.editor_calibre), calibre, number = true) { calibre = it }
        Field(stringResource(R.string.editor_mass), mass, number = true) { mass = it }
        Field(stringResource(R.string.editor_length), length, number = true) { length = it }
        Field(stringResource(R.string.editor_velocity), launchVelocity, number = true) { launchVelocity = it }

        Text(stringResource(R.string.editor_drag_model), color = LcdGreenDim, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DragModels.available.forEach { id ->
                RuggedButton(id, Modifier.weight(1f), accent = dragModel.equals(id, ignoreCase = true)) {
                    dragModel = id
                }
            }
        }

        Section(stringResource(R.string.editor_section_geometry))
        Field(stringResource(R.string.editor_ref_height), refHeight, number = true) { refHeight = it }
        Field(stringResource(R.string.editor_ref_distance), refDist, number = true) { refDist = it }

        Section(stringResource(R.string.editor_section_env))
        Field(stringResource(R.string.editor_temp), temp, number = true) { temp = it }
        Field(stringResource(R.string.editor_humidity), humidity, number = true) { humidity = it }
        Field(stringResource(R.string.editor_altitude), altitude, number = true) { altitude = it }

        Spacer(Modifier.height(8.dp))
        ToggleRow(stringResource(R.string.editor_locked), locked) { locked = it }
        ToggleRow(stringResource(R.string.editor_demo), demo) { demo = it }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.editor_save), Modifier.weight(1f), accent = true) {
                onSave(
                    initial.copy(
                        name = name.ifBlank { "PROFILE" },
                        isLocked = locked,
                        isDemo = demo,
                        projectile = ProjectileSpec(
                            calibreMm = calibre.d(), massKg = mass.d(), lengthM = length.d(),
                            nominalLaunchVelocityMs = launchVelocity.d(),
                            dragModelId = dragModel.ifBlank { DragModels.GENERIC },
                            aeroParams = initial.projectile.aeroParams
                        ),
                        geometry = ReferenceGeometry(
                            referenceHeightM = refHeight.d(),
                            referenceDistanceM = refDist.d(),
                            simulatorConfig = initial.geometry.simulatorConfig
                        ),
                        environmentDefaults = EnvironmentDefaults(
                            temperatureC = temp.d(), humidityPct = humidity.d(), altitudeM = altitude.d()
                        )
                    )
                )
            }
            RuggedButton(stringResource(R.string.editor_cancel), Modifier.weight(1f)) { onCancel() }
        }
        if (onDelete != null) {
            Spacer(Modifier.height(8.dp))
            RuggedButton(stringResource(R.string.editor_delete), Modifier.fillMaxWidth()) { onDelete() }
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = LcdGreenDim,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun Field(label: String, value: String, number: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

private fun Double.str(): String = if (this == 0.0) "" else this.toString()
private fun String.d(): Double = this.toDoubleOrNull() ?: 0.0
