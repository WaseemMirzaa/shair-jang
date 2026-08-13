package com.codetivelab.fieldcalc.ui.calculator

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.ServiceLocator
import com.codetivelab.fieldcalc.domain.input.InputEditor
import com.codetivelab.fieldcalc.domain.input.InputField
import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.SolveResult
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.domain.units.UnitConverter
import com.codetivelab.fieldcalc.domain.validation.InputValidator
import com.codetivelab.fieldcalc.ui.components.Key
import com.codetivelab.fieldcalc.ui.i18n.messageResFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The operator fields, defined in the portable domain layer so the entry rules can be unit-tested
 * off-device. Aliased here because the screens and UI tests address them as `Field`.
 */
typealias Field = InputField

data class CalcUiState(
    val profile: Profile? = null,
    val unit: UnitSystem = UnitSystem.METRIC,
    val selected: Field = Field.RANGE,
    /** All operator values, always held in SI. Conversion happens only at the display edge. */
    val si: OperatorInput = WorkedExample,
    val buffer: String = "",
    @StringRes val status: Int = R.string.status_ready,
    @StringRes val error: Int? = null,
    val calculating: Boolean = false
) {
    val isDemo: Boolean get() = profile?.isDemo == true
    val locked: Boolean get() = profile?.isLocked ?: true

    companion object {
        /** The brief's worked example, so a fresh install has something to SOLVE immediately. */
        val WorkedExample = OperatorInput(
            rangeM = 650.0, windSpeedMs = 8.0, windDirection = ClockDirection(3),
            temperatureC = 30.0, altitudeM = 1371.6 /* 4500 ft */, humidityPct = 50.0,
            inclinationDeg = 8.0
        )
    }
}

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ServiceLocator.provideRepository(app)
    private val settings = ServiceLocator.provideSettings(app)
    private val engine = ServiceLocator.provideEngine()

    private val _state = MutableStateFlow(CalcUiState())
    val state: StateFlow<CalcUiState> = _state.asStateFlow()

    private val _result = MutableStateFlow<SolveResult?>(null)
    val result: StateFlow<SolveResult?> = _result.asStateFlow()

    /** One-shot "a solve finished, show the results screen" signal (never replayed on back). */
    private val _solved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val solved: SharedFlow<Unit> = _solved.asSharedFlow()

    /** Profile whose environment defaults have already been applied. */
    private var seededFromProfile: Long? = null

    init {
        viewModelScope.launch {
            // Restore the operator's last entries before anything else touches the state.
            settings.lastInput.first()?.let { saved -> _state.value = _state.value.copy(si = saved) }

            combine(repo.profiles, settings.settings) { profiles, s ->
                val active = profiles.firstOrNull { it.id == s.lastProfileId } ?: profiles.firstOrNull()
                active to s.unitSystem
            }.collect { (profile, unit) ->
                _state.value = _state.value.copy(profile = profile, unit = unit)
                applyProfileDefaults(profile)
            }
        }
    }

    /**
     * A newly selected profile brings its own environment defaults. They only seed the fields the
     * profile actually owns (temperature / humidity / altitude); the operator's range, wind and
     * inclination are left alone.
     */
    private fun applyProfileDefaults(profile: Profile?) {
        if (profile == null || profile.id == seededFromProfile) return
        val firstEver = seededFromProfile == null
        seededFromProfile = profile.id
        if (firstEver) return  // don't stomp on restored/worked-example values at startup

        val d = profile.environmentDefaults
        _state.value = _state.value.copy(
            si = _state.value.si.copy(
                temperatureC = d.temperatureC,
                humidityPct = d.humidityPct,
                altitudeM = d.altitudeM
            ),
            buffer = ""
        )
    }

    // ---- Field <-> display mapping (rules live in InputEditor) ----

    /** Current value of a field in the active display unit; the live edit buffer wins if present. */
    fun displayValue(f: Field): String {
        val st = _state.value
        if (f == st.selected && st.buffer.isNotEmpty()) return st.buffer
        return InputEditor.display(f, st.si, st.unit)
    }

    /** Unit suffix shown next to a field; DIRECTION/HUMIDITY/INCLINATION are labelled by the screen. */
    fun unitLabel(f: Field): String? =
        InputEditor.quantityOf(f)?.let { UnitConverter.label(it, _state.value.unit) }

    // ---- Keypad handling ----

    fun onKey(key: Key) {
        val st = _state.value
        when (key) {
            is Key.Digit -> setBuffer(InputEditor.appendDigit(st.buffer, key.value))
            Key.Dot -> setBuffer(InputEditor.appendDecimalPoint(st.buffer, st.selected))
            Key.Sign -> setBuffer(InputEditor.toggleSign(st.buffer))
            Key.Back, Key.Left -> setBuffer(InputEditor.backspace(st.buffer))
            Key.Clear -> setBuffer("")
            Key.Up -> select(InputEditor.previous(st.selected))
            Key.Down, Key.Enter, Key.Right -> select(InputEditor.next(st.selected))
            Key.Solve -> solve()
            Key.Menu -> Unit // handled by the screen (opens the menu)
        }
    }

    fun select(field: Field) {
        commit()
        _state.value = _state.value.copy(selected = field, error = null, buffer = "")
    }

    private fun setBuffer(v: String) { _state.value = _state.value.copy(buffer = v, error = null) }

    /** Parse the edit buffer for the selected field and fold it back into the SI state. */
    private fun commit() {
        val st = _state.value
        val committed = InputEditor.commit(st.buffer, st.selected, st.unit, st.si)
        if (committed == st.si) return
        _state.value = st.copy(si = committed, buffer = "")
        persist(committed)
    }

    private fun persist(input: OperatorInput) {
        viewModelScope.launch { runCatching { settings.setLastInput(input) } }
    }

    /** RESET restores the default operator inputs. Protected profile data is never touched. */
    fun reset() {
        val defaults = OperatorInput.Defaults
        _state.value = _state.value.copy(
            si = defaults, buffer = "", error = null, status = R.string.status_ready
        )
        persist(defaults)
    }

    private fun solve() {
        commit()
        val st = _state.value
        val profile = st.profile
        if (profile == null) {
            _state.value = st.copy(error = R.string.error_profile_not_found)
            return
        }

        when (val v = InputValidator.validateAll(
            rangeM = st.si.rangeM, windMs = st.si.windSpeedMs, clockHour = st.si.windDirection.hour,
            tempC = st.si.temperatureC, altM = st.si.altitudeM,
            humidityPct = st.si.humidityPct, inclinationDeg = st.si.inclinationDeg
        )) {
            is InputValidator.Result.Invalid -> {
                _state.value = st.copy(error = messageResFor(v.code))
                return
            }
            InputValidator.Result.Valid -> Unit
        }

        _state.value = st.copy(calculating = true, error = null, status = R.string.status_calculating)
        viewModelScope.launch {
            val outcome = runCatching {
                withContext(Dispatchers.Default) { engine.solve(profile, st.si) }
            }
            outcome
                .onSuccess { res ->
                    _result.value = res
                    _state.value = _state.value.copy(calculating = false, status = R.string.status_complete)
                    _solved.tryEmit(Unit)
                }
                .onFailure { t ->
                    // Operators see a plain message; the detail goes to logcat for developers.
                    Log.e(TAG, "Simulation failed", t)
                    _state.value = _state.value.copy(
                        calculating = false,
                        status = R.string.status_ready,
                        error = R.string.error_calculation
                    )
                }
        }
    }

    /** Exposed so the SOLVE key and UI tests share one entry point. */
    fun solveNow() = solve()

    fun clearResult() {
        _result.value = null
        _state.value = _state.value.copy(status = R.string.status_ready)
    }

    private companion object { const val TAG = "CalculatorViewModel" }
}
