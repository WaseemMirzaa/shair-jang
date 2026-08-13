package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.models.Quantity
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import com.codetivelab.fieldcalc.domain.units.UnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test fun metersToYards_isCorrect() {
        assertEquals(100.0, UnitConverter.yardsToMeters(UnitConverter.metersToYards(100.0)), 1e-9)
        assertEquals(109.361, UnitConverter.metersToYards(100.0), 0.01)
    }

    @Test fun roundTrip_velocity() {
        val v = 800.0
        val back = UnitConverter.ftsToMs(UnitConverter.msToFts(v))
        assertEquals(v, back, 1e-9)
    }

    @Test fun temperature_c_to_f() {
        assertEquals(32.0, UnitConverter.celsiusToFahrenheit(0.0), 1e-9)
        assertEquals(212.0, UnitConverter.celsiusToFahrenheit(100.0), 1e-9)
        assertEquals(0.0, UnitConverter.fahrenheitToCelsius(32.0), 1e-9)
    }

    @Test fun toDisplay_metric_isIdentity() {
        assertEquals(650.0, UnitConverter.toDisplay(650.0, Quantity.DISTANCE, UnitSystem.METRIC), 1e-9)
    }

    @Test fun toSi_imperial_distance() {
        // 711 yd -> ~650 m
        val si = UnitConverter.toSi(711.0, Quantity.DISTANCE, UnitSystem.IMPERIAL)
        assertEquals(650.0, si, 0.5)
    }

    @Test fun labels_switchWithSystem() {
        assertEquals("m", UnitConverter.label(Quantity.DISTANCE, UnitSystem.METRIC))
        assertEquals("yd", UnitConverter.label(Quantity.DISTANCE, UnitSystem.IMPERIAL))
        assertEquals("ft", UnitConverter.label(Quantity.ALTITUDE, UnitSystem.IMPERIAL))
    }

    @Test fun altitude_metersToFeet() {
        // 4500 ft is the brief's example altitude.
        assertEquals(1371.6, UnitConverter.feetToMeters(4500.0), 1e-6)
        assertEquals(4500.0, UnitConverter.metersToFeet(1371.6), 1e-6)
    }

    @Test fun pressure_hpaToInHg() {
        // Standard sea-level pressure: 1013.25 hPa = 29.9213 inHg
        assertEquals(29.9213, UnitConverter.hpaToInHg(1013.25), 1e-3)
        assertEquals(1013.25, UnitConverter.inHgToHpa(29.9213), 1e-2)
    }

    @Test fun energy_joulesToFootPounds() {
        assertEquals(0.737562, UnitConverter.joulesToFootPounds(1.0), 1e-6)
        assertEquals(100.0, UnitConverter.footPoundsToJoules(UnitConverter.joulesToFootPounds(100.0)), 1e-9)
    }

    @Test fun everyQuantity_roundTripsThroughBothSystems() {
        Quantity.entries.forEach { q ->
            UnitSystem.entries.forEach { system ->
                val si = 42.0
                val back = UnitConverter.toSi(UnitConverter.toDisplay(si, q, system), q, system)
                assertEquals("$q in $system", si, back, 1e-9)
            }
        }
    }

    @Test fun format_roundsToTheRequestedPrecision() {
        assertEquals("650", UnitConverter.format(650.4, 0))
        assertEquals("651", UnitConverter.format(650.6, 0))
        assertEquals("8.0", UnitConverter.format(8.0, 1))
        assertEquals("1.2250", UnitConverter.format(1.225, 4))
        assertEquals("-3", UnitConverter.format(-3.2, 0))
    }
}
