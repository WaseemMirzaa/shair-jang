package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.atmosphere.AtmosphereModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtmosphereModelTest {

    @Test fun seaLevel_standard_density_isReasonable() {
        val a = AtmosphereModel.calculateAtmosphere(altitudeM = 0.0, temperatureC = 15.0, humidityPct = 0.0)
        // ISA sea-level dry-air density ~1.225 kg/m^3
        assertEquals(1.225, a.densityKgM3, 0.03)
    }

    @Test fun seaLevel_speedOfSound_isReasonable() {
        val a = AtmosphereModel.calculateAtmosphere(0.0, 15.0, 0.0)
        // ~340 m/s at 15 C
        assertEquals(340.0, a.speedOfSoundMs, 3.0)
    }

    @Test fun pressure_dropsWithAltitude() {
        val low = AtmosphereModel.calculateAtmosphere(0.0, 15.0, 50.0)
        val high = AtmosphereModel.calculateAtmosphere(3000.0, 5.0, 50.0)
        assertTrue(high.pressureHpa < low.pressureHpa)
    }

    @Test fun humidAir_isLessDense_thanDry_atSameTP() {
        val dry = AtmosphereModel.calculateAtmosphere(0.0, 30.0, 0.0)
        val humid = AtmosphereModel.calculateAtmosphere(0.0, 30.0, 100.0)
        assertTrue(humid.densityKgM3 < dry.densityKgM3)
    }
}
