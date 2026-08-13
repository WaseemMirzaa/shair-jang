package com.codetivelab.fieldcalc.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.ProjectileSpec
import com.codetivelab.fieldcalc.ui.components.RuggedButton
import com.codetivelab.fieldcalc.ui.profile.ProfileViewModel
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.Olive
import com.codetivelab.fieldcalc.ui.theme.PanelBlack
import com.codetivelab.fieldcalc.ui.theme.TextMuted
import com.codetivelab.fieldcalc.ui.theme.WarnRed

const val TAG_ADMIN_PIN = "admin_pin_field"
const val TAG_ADMIN_UNLOCK = "admin_unlock"
const val TAG_ADMIN_BACK = "admin_back"

@Composable
fun AdminScreen(
    onBack: () -> Unit,
    vm: AdminViewModel = viewModel(),
    profileVm: ProfileViewModel = viewModel()
) {
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    if (!unlocked) PinGate(vm, onBack) else AdminPanel(vm, profileVm, onBack)
}

@Composable
private fun PinGate(vm: AdminViewModel, onBack: () -> Unit) {
    val error by vm.error.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.admin_title), style = MaterialTheme.typography.titleLarge, color = LcdGreen)
        Text(stringResource(R.string.admin_pin_hint), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
            label = { Text(stringResource(R.string.admin_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().testTag(TAG_ADMIN_PIN)
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.admin_incorrect), color = WarnRed, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.admin_unlock), Modifier.weight(1f), accent = true, tag = TAG_ADMIN_UNLOCK) {
                vm.tryUnlock(pin)
            }
            RuggedButton(stringResource(R.string.btn_back), Modifier.weight(1f), tag = TAG_ADMIN_BACK) { onBack() }
        }
    }
}

@Composable
private fun AdminPanel(vm: AdminViewModel, profileVm: ProfileViewModel, onBack: () -> Unit) {
    val profiles by profileVm.profiles.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Profile?>(null) }

    val current = editing
    if (current != null) {
        ProfileEditor(
            initial = current,
            onCancel = { editing = null },
            onSave = { profileVm.save(it); editing = null },
            onDelete = if (current.id != 0L) { { profileVm.delete(current); editing = null } } else null
        )
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(14.dp)) {
        Text(stringResource(R.string.admin_profiles_title), style = MaterialTheme.typography.titleLarge, color = LcdGreen)
        Text(stringResource(R.string.admin_profiles_subtitle), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(profiles) { p ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PanelBlack)
                        .border(1.dp, Olive, RoundedCornerShape(8.dp)).padding(14.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = LcdGreen, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(if (p.isLocked) R.string.profile_locked else R.string.profile_unlocked),
                            color = LcdGreenDim,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    RuggedButton(stringResource(R.string.admin_edit)) { editing = p }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuggedButton(stringResource(R.string.admin_new_profile), Modifier.weight(1f), accent = true) {
                editing = Profile(name = "NEW PROFILE", isDemo = false, isLocked = true, projectile = ProjectileSpec())
            }
            RuggedButton(stringResource(R.string.admin_lock), Modifier.weight(1f)) { vm.lock() }
            RuggedButton(stringResource(R.string.btn_back), Modifier.weight(1f), tag = TAG_ADMIN_BACK) { onBack() }
        }
    }
}
