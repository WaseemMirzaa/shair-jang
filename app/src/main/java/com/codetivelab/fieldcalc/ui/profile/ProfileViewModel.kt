package com.codetivelab.fieldcalc.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codetivelab.fieldcalc.ServiceLocator
import com.codetivelab.fieldcalc.domain.models.Profile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs both the operator profile-picker and the admin editor. */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ServiceLocator.provideRepository(app)
    private val settings = ServiceLocator.provideSettings(app)

    val profiles: StateFlow<List<Profile>> =
        repo.profiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedId: StateFlow<Long> =
        settings.settings.map { it.lastProfileId }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    fun select(profile: Profile) = viewModelScope.launch { settings.setLastProfile(profile.id) }

    fun save(profile: Profile) = viewModelScope.launch { repo.save(profile) }

    fun delete(profile: Profile) = viewModelScope.launch { repo.delete(profile) }
}
