package com.codetivelab.fieldcalc.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codetivelab.fieldcalc.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Handles the admin PIN gate. Profile CRUD is delegated to ProfileViewModel. */
class AdminViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = ServiceLocator.provideSettings(app)

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun tryUnlock(pin: String) = viewModelScope.launch {
        val actual = settings.adminPassword.first()
        if (pin == actual) { _unlocked.value = true; _error.value = null }
        else _error.value = "INCORRECT PIN"
    }

    fun changePin(newPin: String) = viewModelScope.launch {
        if (newPin.length in 4..8) settings.setAdminPassword(newPin)
    }

    fun lock() { _unlocked.value = false }
}
