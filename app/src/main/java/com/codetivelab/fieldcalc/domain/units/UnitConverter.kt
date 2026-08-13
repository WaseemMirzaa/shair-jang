package com.codetivelab.fieldcalc.domain.units

import com.codetivelab.fieldcalc.domain.models.Quantity
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Converts between SI (used internally everywhere) and the user-facing unit system.
 *
 * Convention:
 *  - toDisplay(): SI value  -> value in the selected system's display unit
 *  - toSi():      display value in the selected system -> SI value
 */
object UnitConverter {

    // ---- Fundamental factors ----
    private const val M_PER_YARD = 0.9144
    private const val M_PER_FOOT = 0.3048
    private const val HPA_PER_INHG = 33.8638866667
    private const val JOULES_PER_FTLB = 1.3558179483314004

    // Distance (m <-> yd)
    fun metersToYards(m: Double) = m / M_PER_YARD
    fun yardsToMeters(y: Double) = y * M_PER_YARD

    // Altitude (m <-> ft)
    fun metersToFeet(m: Double) = m / M_PER_FOOT
    fun feetToMeters(f: Double) = f * M_PER_FOOT

    // Velocity (m/s <-> ft/s)
    fun msToFts(v: Double) = v / M_PER_FOOT
    fun ftsToMs(v: Double) = v * M_PER_FOOT

    // Temperature (°C <-> °F)
    fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0
    fun fahrenheitToCelsius(f: Double) = (f - 32.0) * 5.0 / 9.0

    // Pressure (hPa <-> inHg)
    fun hpaToInHg(hpa: Double) = hpa / HPA_PER_INHG
    fun inHgToHpa(inHg: Double) = inHg * HPA_PER_INHG

    // Energy (J <-> ft·lb)
    fun joulesToFootPounds(j: Double) = j / JOULES_PER_FTLB
    fun footPoundsToJoules(ftlb: Double) = ftlb * JOULES_PER_FTLB

    /** SI -> display unit for [system]. */
    fun toDisplay(value: Double, quantity: Quantity, system: UnitSystem): Double {
        if (system == UnitSystem.METRIC) return value
        return when (quantity) {
            Quantity.DISTANCE -> metersToYards(value)
            Quantity.ALTITUDE -> metersToFeet(value)
            Quantity.VELOCITY -> msToFts(value)
            Quantity.TEMPERATURE -> celsiusToFahrenheit(value)
            Quantity.PRESSURE -> hpaToInHg(value)
            Quantity.ENERGY -> joulesToFootPounds(value)
        }
    }

    /** Display unit for [system] -> SI. */
    fun toSi(value: Double, quantity: Quantity, system: UnitSystem): Double {
        if (system == UnitSystem.METRIC) return value
        return when (quantity) {
            Quantity.DISTANCE -> yardsToMeters(value)
            Quantity.ALTITUDE -> feetToMeters(value)
            Quantity.VELOCITY -> ftsToMs(value)
            Quantity.TEMPERATURE -> fahrenheitToCelsius(value)
            Quantity.PRESSURE -> inHgToHpa(value)
            Quantity.ENERGY -> footPoundsToJoules(value)
        }
    }

    /** Short label used next to fields and in the LCD readout. */
    fun label(quantity: Quantity, system: UnitSystem): String = when (quantity) {
        Quantity.DISTANCE -> if (system == UnitSystem.METRIC) "m" else "yd"
        Quantity.ALTITUDE -> if (system == UnitSystem.METRIC) "m" else "ft"
        Quantity.VELOCITY -> if (system == UnitSystem.METRIC) "m/s" else "ft/s"
        Quantity.TEMPERATURE -> if (system == UnitSystem.METRIC) "\u00B0C" else "\u00B0F"
        Quantity.PRESSURE -> if (system == UnitSystem.METRIC) "hPa" else "inHg"
        Quantity.ENERGY -> if (system == UnitSystem.METRIC) "J" else "ft·lb"
    }

    /** Format for display with a fixed number of decimals. */
    fun format(value: Double, decimals: Int = 1): String =
        if (decimals <= 0) value.roundToInt().toString()
        else String.format(Locale.US, "%.${decimals}f", value)
}
