package com.codetivelab.fieldcalc.domain.models

/** Unit system selectable in Settings. Calculations run in SI; conversion happens at the edges. */
enum class UnitSystem { METRIC, IMPERIAL }

/** Rugged dark is the default; extra themes can be added later. */
enum class AppTheme { RUGGED_DARK, HIGH_CONTRAST, AMBER }

/** Physical quantities the UnitConverter knows how to display/parse. */
enum class Quantity { DISTANCE, VELOCITY, TEMPERATURE, PRESSURE, ALTITUDE, ENERGY }

/**
 * Wind direction on a 12-hour clock face (shooter-style convention).
 * 12 o'clock = headwind (toward target), 6 = tailwind, 3 = from the right, 9 = from the left.
 */
@JvmInline
value class ClockDirection(val hour: Int) {
    init { require(hour in 1..12) { "Clock direction must be 1..12" } }

    /** Compass-style bearing in degrees, 0 = 12 o'clock, clockwise. */
    val bearingDegrees: Double get() = ((hour % 12) * 30.0)

    companion object { val Default = ClockDirection(3) }
}
