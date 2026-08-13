package com.codetivelab.fieldcalc.domain.models

/**
 * The changing environmental / simulation variables an operator may enter on the main screen.
 * All values are stored in SI regardless of the display unit system:
 *   rangeM (m), windSpeedMs (m/s), temperatureC (°C), altitudeM (m), humidityPct (0..100), inclinationDeg (°)
 */
data class OperatorInput(
    val rangeM: Double = 0.0,
    val windSpeedMs: Double = 0.0,
    val windDirection: ClockDirection = ClockDirection.Default,
    val temperatureC: Double = 15.0,
    val altitudeM: Double = 0.0,
    val humidityPct: Double = 50.0,
    val inclinationDeg: Double = 0.0
) {
    companion object {
        /** Sensible defaults used by RESET on the operator screen. */
        val Defaults = OperatorInput()
    }
}
