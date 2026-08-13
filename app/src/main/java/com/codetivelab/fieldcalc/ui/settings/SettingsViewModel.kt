package com.codetivelab.fieldcalc.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codetivelab.fieldcalc.ServiceLocator
import com.codetivelab.fieldcalc.data.prefs.SettingsStore
import com.codetivelab.fieldcalc.domain.models.AppTheme
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ServiceLocator.provideSettings(app)

    val settings: StateFlow<SettingsStore.Settings> =
        store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.Settings())

    fun setUnit(u: UnitSystem) = viewModelScope.launch { store.setUnitSystem(u) }
    fun setTheme(t: AppTheme) = viewModelScope.launch { store.setTheme(t) }
    fun setBrightness(v: Int) = viewModelScope.launch { store.setBrightness(v) }
    fun setTimeout(v: Int) = viewModelScope.launch { store.setTimeout(v) }
    fun setSound(v: Boolean) = viewModelScope.launch { store.setSound(v) }
    fun setVibration(v: Boolean) = viewModelScope.launch { store.setVibration(v) }
    fun setLanguage(v: String) = viewModelScope.launch { store.setLanguage(v) }
    fun setPin(v: String) = viewModelScope.launch { if (v.length in 4..8) store.setAdminPassword(v) }
}
