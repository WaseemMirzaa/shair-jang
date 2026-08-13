package com.codetivelab.fieldcalc.domain.validation

/**
 * Range checks for operator inputs. Bounds are expressed in SI (the app converts before validating),
 * so they hold regardless of the selected unit system.
 *
 * Failures are reported as a [ValidationCode], not as a display string: the UI layer maps the code
 * to a localized resource, which keeps this module free of any platform or language dependency.
 * [ValidationCode.defaultMessage] is the English fallback used by tests and non-Android hosts.
 */
object InputValidator {

    // Generous, non-operational sanity bounds for a physics/education tool.
    const val RANGE_MIN_M = 0.0
    const val RANGE_MAX_M = 5000.0
    const val WIND_MIN_MS = 0.0
    const val WIND_MAX_MS = 60.0
    const val TEMP_MIN_C = -60.0
    const val TEMP_MAX_C = 60.0
    const val ALT_MIN_M = -430.0     // ~Dead Sea
    const val ALT_MAX_M = 9000.0
    const val HUMIDITY_MIN = 0.0
    const val HUMIDITY_MAX = 100.0
    const val INCLINATION_MIN_DEG = -60.0
    const val INCLINATION_MAX_DEG = 60.0

    enum class ValidationCode(val defaultMessage: String) {
        INVALID_RANGE("INVALID RANGE"),
        INVALID_WIND_SPEED("INVALID WIND SPEED"),
        INVALID_DIRECTION("INVALID DIRECTION"),
        INVALID_TEMPERATURE("INVALID TEMPERATURE"),
        INVALID_ALTITUDE("INVALID ALTITUDE"),
        INVALID_HUMIDITY("INVALID HUMIDITY"),
        INVALID_INCLINATION("INVALID INCLINATION"),
        INPUT_REQUIRED("INPUT REQUIRED")
    }

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val code: ValidationCode) : Result {
            /** English fallback; the UI prefers the localized string for [code]. */
            val message: String get() = code.defaultMessage
        }
    }

    private fun requireRange(v: Double, min: Double, max: Double, code: ValidationCode): Result =
        if (v.isNaN() || v < min || v > max) Result.Invalid(code) else Result.Valid

    fun validateRange(m: Double) = requireRange(m, RANGE_MIN_M, RANGE_MAX_M, ValidationCode.INVALID_RANGE)
    fun validateWind(ms: Double) = requireRange(ms, WIND_MIN_MS, WIND_MAX_MS, ValidationCode.INVALID_WIND_SPEED)
    fun validateTemperature(c: Double) = requireRange(c, TEMP_MIN_C, TEMP_MAX_C, ValidationCode.INVALID_TEMPERATURE)
    fun validateAltitude(m: Double) = requireRange(m, ALT_MIN_M, ALT_MAX_M, ValidationCode.INVALID_ALTITUDE)
    fun validateHumidity(pct: Double) = requireRange(pct, HUMIDITY_MIN, HUMIDITY_MAX, ValidationCode.INVALID_HUMIDITY)
    fun validateInclination(deg: Double) =
        requireRange(deg, INCLINATION_MIN_DEG, INCLINATION_MAX_DEG, ValidationCode.INVALID_INCLINATION)

    fun validateClock(hour: Int) =
        if (hour in 1..12) Result.Valid else Result.Invalid(ValidationCode.INVALID_DIRECTION)

    /** A range of zero would make the simulation meaningless, so SOLVE demands a real value. */
    fun validateRangeEntered(m: Double) =
        if (m <= 0.0) Result.Invalid(ValidationCode.INPUT_REQUIRED) else validateRange(m)

    /** Validate every operator field; returns the first problem found, or Valid. */
    fun validateAll(
        rangeM: Double, windMs: Double, clockHour: Int, tempC: Double,
        altM: Double, humidityPct: Double, inclinationDeg: Double
    ): Result {
        val checks = listOf(
            validateRangeEntered(rangeM), validateWind(windMs), validateClock(clockHour),
            validateTemperature(tempC), validateAltitude(altM),
            validateHumidity(humidityPct), validateInclination(inclinationDeg)
        )
        return checks.firstOrNull { it is Result.Invalid } ?: Result.Valid
    }
}
