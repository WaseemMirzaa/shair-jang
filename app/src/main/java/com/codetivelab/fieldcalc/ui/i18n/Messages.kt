package com.codetivelab.fieldcalc.ui.i18n

import androidx.annotation.StringRes
import com.codetivelab.fieldcalc.R
import com.codetivelab.fieldcalc.domain.validation.InputValidator.ValidationCode

/**
 * The single bridge between the platform-independent domain layer and Android string resources.
 *
 * The domain returns codes, never sentences, so it stays portable (ESP32 / Raspberry Pi builds have
 * no resource system) and so a new language is purely a `res/values-<code>/strings.xml` drop-in.
 */
@StringRes
fun messageResFor(code: ValidationCode): Int = when (code) {
    ValidationCode.INVALID_RANGE -> R.string.error_invalid_range
    ValidationCode.INVALID_WIND_SPEED -> R.string.error_invalid_wind
    ValidationCode.INVALID_DIRECTION -> R.string.error_invalid_direction
    ValidationCode.INVALID_TEMPERATURE -> R.string.error_invalid_temperature
    ValidationCode.INVALID_ALTITUDE -> R.string.error_invalid_altitude
    ValidationCode.INVALID_HUMIDITY -> R.string.error_invalid_humidity
    ValidationCode.INVALID_INCLINATION -> R.string.error_invalid_inclination
    ValidationCode.INPUT_REQUIRED -> R.string.error_input_required
}
