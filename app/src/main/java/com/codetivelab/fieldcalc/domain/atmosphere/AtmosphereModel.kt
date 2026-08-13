package com.codetivelab.fieldcalc.domain.atmosphere

import com.codetivelab.fieldcalc.domain.models.AtmosphereState
import kotlin.math.exp

/**
 * Generic atmospheric physics: derives an illustrative atmospheric state from altitude,
 * temperature and humidity. This is standard, textbook meteorology (also used in aviation,
 * acoustics and HVAC) and is NOT a firing solution — it just describes the air.
 *
 * References: International Standard Atmosphere (ISA) barometric formula; ideal-gas density of
 * moist air; Newton–Laplace speed of sound.
 */
object AtmosphereModel {

    private const val SEA_LEVEL_PRESSURE_PA = 101325.0
    private const val SEA_LEVEL_TEMP_K = 288.15
    private const val LAPSE_RATE_K_PER_M = 0.0065      // ISA troposphere
    private const val GRAVITY = 9.80665
    private const val MOLAR_MASS_DRY_AIR = 0.0289644   // kg/mol
    private const val UNIVERSAL_GAS_CONST = 8.31446    // J/(mol·K)
    private const val R_DRY = 287.058                  // J/(kg·K)
    private const val R_VAPOUR = 461.495               // J/(kg·K)
    private const val GAMMA = 1.4                       // ratio of specific heats for air
    private const val KELVIN = 273.15

    /**
     * @param altitudeM   geometric altitude in metres
     * @param temperatureC measured/ambient temperature in °C (overrides ISA temperature at altitude)
     * @param humidityPct  relative humidity 0..100
     */
    fun calculateAtmosphere(
        altitudeM: Double,
        temperatureC: Double,
        humidityPct: Double
    ): AtmosphereState {
        val tempK = temperatureC + KELVIN

        // Pressure from ISA barometric formula (uses ISA temperature profile for the column).
        val isaTempAtAlt = SEA_LEVEL_TEMP_K - LAPSE_RATE_K_PER_M * altitudeM
        val exponent = (GRAVITY * MOLAR_MASS_DRY_AIR) / (UNIVERSAL_GAS_CONST * LAPSE_RATE_K_PER_M)
        val pressurePa = if (isaTempAtAlt > 0.0) {
            SEA_LEVEL_PRESSURE_PA * Math.pow(isaTempAtAlt / SEA_LEVEL_TEMP_K, exponent)
        } else {
            // Above the troposphere fall back to an isothermal approximation.
            SEA_LEVEL_PRESSURE_PA * exp(-GRAVITY * MOLAR_MASS_DRY_AIR * altitudeM /
                    (UNIVERSAL_GAS_CONST * SEA_LEVEL_TEMP_K))
        }

        // Water-vapour partial pressure from RH using the Tetens saturation formula.
        val satVapourPa = 610.78 * exp((17.27 * temperatureC) / (temperatureC + 237.3))
        val vapourPa = (humidityPct.coerceIn(0.0, 100.0) / 100.0) * satVapourPa
        val dryPa = (pressurePa - vapourPa).coerceAtLeast(0.0)

        // Moist-air density = dry partial / (Rd·T) + vapour partial / (Rv·T).
        val density = dryPa / (R_DRY * tempK) + vapourPa / (R_VAPOUR * tempK)

        // Speed of sound (Newton–Laplace) using the effective gas constant of the moist mixture.
        val rEffective = if (density > 0.0) pressurePa / (density * tempK) else R_DRY
        val speedOfSound = Math.sqrt(GAMMA * rEffective * tempK)

        return AtmosphereState(
            densityKgM3 = density,
            speedOfSoundMs = speedOfSound,
            pressureHpa = pressurePa / 100.0,
            temperatureC = temperatureC
        )
    }
}
