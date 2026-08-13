package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.data.database.Converters
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Room type converters — the aero-parameter map is stored as text, so it must survive a round trip. */
class ConvertersTest {

    private val converters = Converters()

    @Test fun unitSystem_roundTrips() {
        UnitSystem.entries.forEach { system ->
            assertEquals(system, converters.stringToUnit(converters.unitToString(system)))
        }
    }

    @Test fun aeroParams_roundTrip() {
        val params = mapOf("formFactor" to 1.05, "cd" to 0.3)
        val restored = converters.stringToMap(converters.mapToString(params))
        assertEquals(params, restored)
    }

    @Test fun emptyMap_roundTrips() {
        assertTrue(converters.stringToMap(converters.mapToString(emptyMap())).isEmpty())
        assertTrue(converters.stringToMap("").isEmpty())
    }

    @Test fun malformedEntries_areSkippedRatherThanCrashing() {
        assertEquals(mapOf("cd" to 0.3), converters.stringToMap("cd=0.3;broken;alsoBroken=notANumber"))
    }

    @Test fun negativeAndExponentValues_survive() {
        val params = mapOf("a" to -1.5, "b" to 1.0E-4)
        assertEquals(params, converters.stringToMap(converters.mapToString(params)))
    }
}
