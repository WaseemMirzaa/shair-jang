package com.codetivelab.fieldcalc.ui.calculator

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.ServiceLocator
import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.Profile
import com.codetivelab.fieldcalc.domain.models.Quantity
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

enum class Field { RANGE, WIND, DIRECTION, TEMPERATURE, ALTITUDE, HUMIDITY, INCLINATION }

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

    // ---- Field <-> Quantity mapping ----

    private fun quantityOf(f: Field): Quantity? = when (f) {
        Field.RANGE -> Quantity.DISTANCE
        Field.WIND -> Quantity.VELOCITY
        Field.TEMPERATURE -> Quantity.TEMPERATURE
        Field.ALTITUDE -> Quantity.ALTITUDE
        Field.HUMIDITY, Field.INCLINATION, Field.DIRECTION -> null // unitless / special
    }

    /** Current value of a field in the active display unit; the live edit buffer wins if present. */
    fun displayValue(f: Field): String {
        val st = _state.value
        if (f == st.selected && st.buffer.isNotEmpty()) return st.buffer
        return rawDisplay(f)
    }

    private fun rawDisplay(f: Field): String {
        val st = _state.value
        return when (f) {
            Field.DIRECTION -> st.si.windDirection.hour.toString()
            Field.HUMIDITY -> UnitConverter.format(st.si.humidityPct, 0)
            Field.INCLINATION -> {
                val v = st.si.inclinationDeg
                (if (v > 0) "+" else "") + UnitConverter.format(v, 0)
            }
            else -> {
                val si = when (f) {
                    Field.RANGE -> st.si.rangeM
                    Field.WIND -> st.si.windSpeedMs
                    Field.TEMPERATURE -> st.si.temperatureC
                    Field.ALTITUDE -> st.si.altitudeM
                    else -> 0.0
                }
                val q = quantityOf(f)!!
                val decimals = if (f == Field.WIND) 1 else 0
                UnitConverter.format(UnitConverter.toDisplay(si, q, st.unit), decimals)
            }
        }
    }

    /** Unit suffix shown next to a field; DIRECTION/HUMIDITY/INCLINATION are handled by the screen. */
    fun unitQuantity(f: Field): Quantity? = quantityOf(f)

    fun unitLabel(f: Field): String? = quantityOf(f)?.let { UnitConverter.label(it, _state.value.unit) }

    // ---- Keypad handling ----

    fun onKey(key: Key) {
        val st = _state.value
        when (key) {
            is Key.Digit -> setBuffer(appendDigit(currentBuffer(), key.value.toString()))
            Key.Dot -> if (!currentBuffer().contains(".") && st.selected != Field.DIRECTION)
                setBuffer(currentBuffer().ifEmpty { "0" } + ".")
            Key.Sign -> setBuffer(toggleSign(currentBuffer()))
            Key.Back, Key.Left -> setBuffer(currentBuffer().dropLast(1))
            Key.Clear -> setBuffer("")
            Key.Up -> select(prevField(st.selected))
            Key.Down, Key.Enter, Key.Right -> select(nextField(st.selected))
            Key.Solve -> solve()
            Key.Menu -> Unit // handled by the screen (opens the menu)
        }
    }

    fun select(field: Field) {
        commit()
        _state.value = _state.value.copy(selected = field, error = null, buffer = "")
    }

    private fun currentBuffer(): String = _state.value.buffer
    private fun setBuffer(v: String) { _state.value = _state.value.copy(buffer = v, error = null) }

    private fun appendDigit(buffer: String, digit: String): String {
        val next = (if (buffer == "0") "" else buffer) + digit
        return next.take(8)
    }

    private fun toggleSign(buffer: String): String =
        if (buffer.startsWith("-")) buffer.drop(1) else "-$buffer"

    /** Parse the edit buffer for the selected field and fold it back into the SI state. */
    private fun commit() {
        val st = _state.value
        val raw = st.buffer
        if (raw.isBlank() || raw == "-" || raw == ".") return
        val value = raw.toDoubleOrNull() ?: return
        val newSi = when (st.selected) {
            Field.DIRECTION -> st.si.copy(windDirection = ClockDirection(value.toInt().coerceIn(1, 12)))
            Field.HUMIDITY -> st.si.copy(humidityPct = value)
            Field.INCLINATION -> st.si.copy(inclinationDeg = value)
            Field.RANGE -> st.si.copy(rangeM = UnitConverter.toSi(value, Quantity.DISTANCE, st.unit))
            Field.WIND -> st.si.copy(windSpeedMs = UnitConverter.toSi(value, Quantity.VELOCITY, st.unit))
            Field.TEMPERATURE -> st.si.copy(temperatureC = UnitConverter.toSi(value, Quantity.TEMPERATURE, st.unit))
            Field.ALTITUDE -> st.si.copy(altitudeM = UnitConverter.toSi(value, Quantity.ALTITUDE, st.unit))
        }
        _state.value = st.copy(si = newSi, buffer = "")
        persist(newSi)
    }

    private fun persist(input: OperatorInput) {
        viewModelScope.launch { runCatching { settings.setLastInput(input) } }
    }

    private fun nextField(f: Field): Field {
        val v = Field.entries
        return v[(v.indexOf(f) + 1) % v.size]
    }

    private fun prevField(f: Field): Field {
        val v = Field.entries
        return v[(v.indexOf(f) - 1 + v.size) % v.size]
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
