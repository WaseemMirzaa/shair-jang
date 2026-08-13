package com.codetivelab.fieldcalc.domain.input

import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.Quantity
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.domain.units.UnitConverter

/** The seven operator-editable fields, in the order the keypad walks them. */
enum class InputField { RANGE, WIND, DIRECTION, TEMPERATURE, ALTITUDE, HUMIDITY, INCLINATION }

/**
 * All the calculator's text-entry behaviour: what a digit/dot/sign/backspace does to the edit
 * buffer, how a buffer is folded back into the SI [OperatorInput], and how a stored value is
 * rendered in the operator's unit system.
 *
 * Kept here — platform independent, alongside the rest of the domain — rather than in the
 * ViewModel, so the keypad rules can be executed and unit-tested without a device, and so a future
 * ESP32 / Raspberry Pi front end inherits exactly the same behaviour.
 */
object InputEditor {

    /** An entry longer than this is a fat-finger, not a value. */
    const val MAX_DIGITS = 8

    // ---- Buffer editing -------------------------------------------------------------------

    fun appendDigit(buffer: String, digit: Int): String {
        // A leading zero is replaced rather than kept ("0" then "5" reads 5, not 05).
        val next = (if (buffer == "0") "" else buffer) + digit.toString()
        return next.take(MAX_DIGITS)
    }

    /** The clock-direction field is whole hours only, so it refuses a decimal point. */
    fun appendDecimalPoint(buffer: String, field: InputField): String {
        if (field == InputField.DIRECTION || buffer.contains(".")) return buffer
        return (buffer.ifEmpty { "0" } + ".").take(MAX_DIGITS)
    }

    fun toggleSign(buffer: String): String =
        if (buffer.startsWith("-")) buffer.drop(1) else "-$buffer"

    fun backspace(buffer: String): String = buffer.dropLast(1)

    // ---- Field navigation -----------------------------------------------------------------

    fun next(field: InputField): InputField {
        val all = InputField.entries
        return all[(all.indexOf(field) + 1) % all.size]
    }

    fun previous(field: InputField): InputField {
        val all = InputField.entries
        return all[(all.indexOf(field) - 1 + all.size) % all.size]
    }

    // ---- Units ----------------------------------------------------------------------------

    /** The convertible quantity behind a field, or null for the unitless/special ones. */
    fun quantityOf(field: InputField): Quantity? = when (field) {
        InputField.RANGE -> Quantity.DISTANCE
        InputField.WIND -> Quantity.VELOCITY
        InputField.TEMPERATURE -> Quantity.TEMPERATURE
        InputField.ALTITUDE -> Quantity.ALTITUDE
        InputField.DIRECTION, InputField.HUMIDITY, InputField.INCLINATION -> null
    }

    /** Decimal places shown for a field. */
    fun decimalsFor(field: InputField): Int = if (field == InputField.WIND) 1 else 0

    // ---- Commit / display -----------------------------------------------------------------

    /**
     * Fold [buffer] into [input]. A blank or half-typed buffer ("", "-", ".") leaves the input
     * untouched, so an operator can tap through fields without destroying values.
     */
    fun commit(
        buffer: String,
        field: InputField,
        unit: UnitSystem,
        input: OperatorInput
    ): OperatorInput {
        val value = parse(buffer) ?: return input
        return when (field) {
            InputField.DIRECTION -> input.copy(windDirection = ClockDirection(clampHour(value)))
            InputField.HUMIDITY -> input.copy(humidityPct = value)
            InputField.INCLINATION -> input.copy(inclinationDeg = value)
            InputField.RANGE -> input.copy(rangeM = UnitConverter.toSi(value, Quantity.DISTANCE, unit))
            InputField.WIND -> input.copy(windSpeedMs = UnitConverter.toSi(value, Quantity.VELOCITY, unit))
            InputField.TEMPERATURE -> input.copy(temperatureC = UnitConverter.toSi(value, Quantity.TEMPERATURE, unit))
            InputField.ALTITUDE -> input.copy(altitudeM = UnitConverter.toSi(value, Quantity.ALTITUDE, unit))
        }
    }

    /** Null when the buffer holds nothing usable yet. */
    fun parse(buffer: String): Double? {
        val trimmed = buffer.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "." || trimmed == "-.") return null
        return trimmed.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    /** ClockDirection only accepts 1..12; a typed 0 or 13 is pulled to the nearest legal hour. */
    private fun clampHour(value: Double): Int = value.toInt().coerceIn(1, 12)

    /** The stored value of [field], rendered in [unit] the way the LCD shows it. */
    fun display(field: InputField, input: OperatorInput, unit: UnitSystem): String = when (field) {
        InputField.DIRECTION -> input.windDirection.hour.toString()
        InputField.HUMIDITY -> UnitConverter.format(input.humidityPct, 0)
        InputField.INCLINATION -> {
            val v = input.inclinationDeg
            (if (v > 0) "+" else "") + UnitConverter.format(v, 0)
        }
        else -> {
            val si = when (field) {
                InputField.RANGE -> input.rangeM
                InputField.WIND -> input.windSpeedMs
                InputField.TEMPERATURE -> input.temperatureC
                InputField.ALTITUDE -> input.altitudeM
                else -> 0.0
            }
            val quantity = quantityOf(field)!!
            UnitConverter.format(UnitConverter.toDisplay(si, quantity, unit), decimalsFor(field))
        }
    }
}
