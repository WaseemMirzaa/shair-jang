package com.codetivelab.fieldcalc.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codetivelab.fieldcalc.domain.models.AppTheme
import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-wide settings persisted locally (offline). */
class SettingsStore(private val context: Context) {

    private object Keys {
        val UNIT = stringPreferencesKey("unit_system")
        val THEME = stringPreferencesKey("theme")
        val BRIGHTNESS = intPreferencesKey("brightness")
        val TIMEOUT = intPreferencesKey("screen_timeout_s")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATION = booleanPreferencesKey("vibration")
        val LANGUAGE = stringPreferencesKey("language")
        val LAST_PROFILE = intPreferencesKey("last_profile_id")
        val ADMIN_PASSWORD = stringPreferencesKey("admin_password")

        // Last operator inputs, kept in SI so the stored values survive a unit-system change.
        val IN_RANGE = doublePreferencesKey("in_range_m")
        val IN_WIND = doublePreferencesKey("in_wind_ms")
        val IN_CLOCK = intPreferencesKey("in_wind_clock")
        val IN_TEMP = doublePreferencesKey("in_temp_c")
        val IN_ALT = doublePreferencesKey("in_alt_m")
        val IN_HUMIDITY = doublePreferencesKey("in_humidity_pct")
        val IN_INCLINATION = doublePreferencesKey("in_inclination_deg")
    }

    data class Settings(
        val unitSystem: UnitSystem = UnitSystem.METRIC,
        val theme: AppTheme = AppTheme.RUGGED_DARK,
        val brightness: Int = 80,
        val screenTimeoutS: Int = 60,
        val sound: Boolean = true,
        val vibration: Boolean = true,
        val language: String = "en",
        val lastProfileId: Long = -1L
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            unitSystem = p[Keys.UNIT]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.METRIC,
            theme = p[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.RUGGED_DARK,
            brightness = p[Keys.BRIGHTNESS] ?: 80,
            screenTimeoutS = p[Keys.TIMEOUT] ?: 60,
            sound = p[Keys.SOUND] ?: true,
            vibration = p[Keys.VIBRATION] ?: true,
            language = p[Keys.LANGUAGE] ?: "en",
            lastProfileId = (p[Keys.LAST_PROFILE] ?: -1).toLong()
        )
    }

    /**
     * The operator's last entered values, so the calculator comes back up exactly as it was left.
     * Null until the first SOLVE/ENTER, which lets the screen fall back to its worked example.
     */
    val lastInput: Flow<OperatorInput?> = context.dataStore.data.map { p ->
        val range = p[Keys.IN_RANGE] ?: return@map null
        OperatorInput(
            rangeM = range,
            windSpeedMs = p[Keys.IN_WIND] ?: 0.0,
            windDirection = ClockDirection((p[Keys.IN_CLOCK] ?: 3).coerceIn(1, 12)),
            temperatureC = p[Keys.IN_TEMP] ?: 15.0,
            altitudeM = p[Keys.IN_ALT] ?: 0.0,
            humidityPct = p[Keys.IN_HUMIDITY] ?: 50.0,
            inclinationDeg = p[Keys.IN_INCLINATION] ?: 0.0
        )
    }

    suspend fun setLastInput(input: OperatorInput) = context.dataStore.edit { p ->
        p[Keys.IN_RANGE] = input.rangeM
        p[Keys.IN_WIND] = input.windSpeedMs
        p[Keys.IN_CLOCK] = input.windDirection.hour
        p[Keys.IN_TEMP] = input.temperatureC
        p[Keys.IN_ALT] = input.altitudeM
        p[Keys.IN_HUMIDITY] = input.humidityPct
        p[Keys.IN_INCLINATION] = input.inclinationDeg
    }

    /** Admin PIN for the prototype. Defaults to "0000" until changed. Local only. */
    val adminPassword: Flow<String> = context.dataStore.data.map { it[Keys.ADMIN_PASSWORD] ?: "0000" }

    suspend fun setUnitSystem(u: UnitSystem) = context.dataStore.edit { it[Keys.UNIT] = u.name }
    suspend fun setTheme(t: AppTheme) = context.dataStore.edit { it[Keys.THEME] = t.name }
    suspend fun setBrightness(v: Int) = context.dataStore.edit { it[Keys.BRIGHTNESS] = v.coerceIn(10, 100) }
    suspend fun setTimeout(v: Int) = context.dataStore.edit { it[Keys.TIMEOUT] = v.coerceIn(10, 600) }
    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[Keys.SOUND] = v }
    suspend fun setVibration(v: Boolean) = context.dataStore.edit { it[Keys.VIBRATION] = v }
    suspend fun setLanguage(v: String) = context.dataStore.edit { it[Keys.LANGUAGE] = v }
    suspend fun setLastProfile(id: Long) = context.dataStore.edit { it[Keys.LAST_PROFILE] = id.toInt() }
    suspend fun setAdminPassword(pw: String) = context.dataStore.edit { it[Keys.ADMIN_PASSWORD] = pw }
}
