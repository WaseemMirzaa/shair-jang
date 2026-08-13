package com.codetivelab.fieldcalc.ui.settings

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
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.models.AppTheme
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.ui.components.RuggedButton
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.TextMuted

const val TAG_SETTINGS_METRIC = "settings_metric"
const val TAG_SETTINGS_IMPERIAL = "settings_imperial"
const val TAG_SETTINGS_BACK = "settings_back"

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, color = LcdGreen)
        Spacer(Modifier.height(12.dp))

        Label(stringResource(R.string.settings_unit_system))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(
                stringResource(R.string.settings_metric), Modifier.weight(1f),
                accent = s.unitSystem == UnitSystem.METRIC, tag = TAG_SETTINGS_METRIC
            ) { vm.setUnit(UnitSystem.METRIC) }
            RuggedButton(
                stringResource(R.string.settings_imperial), Modifier.weight(1f),
                accent = s.unitSystem == UnitSystem.IMPERIAL, tag = TAG_SETTINGS_IMPERIAL
            ) { vm.setUnit(UnitSystem.IMPERIAL) }
        }

        Label(stringResource(R.string.settings_theme))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.theme_olive), Modifier.weight(1f), accent = s.theme == AppTheme.RUGGED_DARK) {
                vm.setTheme(AppTheme.RUGGED_DARK)
            }
            RuggedButton(stringResource(R.string.theme_amber), Modifier.weight(1f), accent = s.theme == AppTheme.AMBER) {
                vm.setTheme(AppTheme.AMBER)
            }
            RuggedButton(stringResource(R.string.theme_hicon), Modifier.weight(1f), accent = s.theme == AppTheme.HIGH_CONTRAST) {
                vm.setTheme(AppTheme.HIGH_CONTRAST)
            }
        }

        Label(stringResource(R.string.settings_brightness, s.brightness))
        Slider(value = s.brightness.toFloat(), onValueChange = { vm.setBrightness(it.toInt()) }, valueRange = 10f..100f)

        Label(stringResource(R.string.settings_timeout, s.screenTimeoutS))
        Slider(value = s.screenTimeoutS.toFloat(), onValueChange = { vm.setTimeout(it.toInt()) }, valueRange = 10f..600f)

        ToggleRow(stringResource(R.string.settings_sound), s.sound) { vm.setSound(it) }
        ToggleRow(stringResource(R.string.settings_vibration), s.vibration) { vm.setVibration(it) }

        Label(stringResource(R.string.settings_language))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton("EN", Modifier.weight(1f), accent = s.language == "en") { vm.setLanguage("en") }
            RuggedButton("ES", Modifier.weight(1f), accent = s.language == "es") { vm.setLanguage("es") }
            RuggedButton("FR", Modifier.weight(1f), accent = s.language == "fr") { vm.setLanguage("fr") }
        }

        Label(stringResource(R.string.settings_admin_pin))
        var pin by remember { mutableStateOf("") }
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
            label = { Text(stringResource(R.string.settings_new_pin)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        RuggedButton(stringResource(R.string.settings_update_pin), Modifier.fillMaxWidth()) {
            if (pin.length in 4..8) { vm.setPin(pin); pin = "" }
        }

        Label(stringResource(R.string.settings_about))
        Text(
            stringResource(R.string.about_body, BuildConfigVersion),
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(Modifier.height(20.dp))
        RuggedButton(stringResource(R.string.btn_back), Modifier.fillMaxWidth(), accent = true, tag = TAG_SETTINGS_BACK) {
            onBack()
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_offline), color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Kept as a plain constant so the About line needs no BuildConfig generation. */
private const val BuildConfigVersion = "1.0.0"

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, color = LcdGreenDim, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = LcdGreenDim, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
