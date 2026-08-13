package com.codetivelab.fieldcalc.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.ui.components.RuggedButton
import com.codetivelab.fieldcalc.ui.theme.LcdGreen
import com.codetivelab.fieldcalc.ui.theme.LcdGreenDim
import com.codetivelab.fieldcalc.ui.theme.Olive
import com.codetivelab.fieldcalc.ui.theme.PanelBlack
import com.codetivelab.fieldcalc.ui.theme.TextMuted

const val TAG_PROFILE_LIST = "profile_list"
const val TAG_PROFILE_BACK = "profile_back"

@Composable
fun ProfileScreen(onBack: () -> Unit, vm: ProfileViewModel = viewModel()) {
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val selected by vm.selectedId.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(14.dp)
    ) {
        Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.titleLarge, color = LcdGreen)
        Text(stringResource(R.string.profiles_subtitle), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            Modifier.weight(1f).testTag(TAG_PROFILE_LIST),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles) { p -> ProfileCard(p, p.id == selected) { vm.select(p); onBack() } }
        }

        Spacer(Modifier.height(12.dp))
        RuggedButton(stringResource(R.string.btn_back), Modifier.fillMaxWidth(), tag = TAG_PROFILE_BACK) { onBack() }
    }
}

@Composable
private fun ProfileCard(profile: Profile, selected: Boolean, onClick: () -> Unit) {
    val lockLabel = stringResource(if (profile.isLocked) R.string.profile_locked else R.string.profile_unlocked)
    val demoLabel = stringResource(R.string.badge_demo)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBlack)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) LcdGreen else Olive,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(profile.name, color = LcdGreen, style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (profile.isDemo) "$lockLabel  •  $demoLabel" else lockLabel,
                color = LcdGreenDim,
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (selected) {
            Text(stringResource(R.string.profile_active), color = LcdGreen, style = MaterialTheme.typography.labelLarge)
        }
    }
}
