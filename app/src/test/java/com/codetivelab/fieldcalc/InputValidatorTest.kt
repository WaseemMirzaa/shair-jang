package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.validation.InputValidator
import com.codetivelab.fieldcalc.domain.validation.InputValidator.ValidationCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test fun validRange_passes() {
        assertTrue(InputValidator.validateRange(650.0) is InputValidator.Result.Valid)
    }

    @Test fun negativeRange_fails() {
        assertEquals(
            InputValidator.Result.Invalid(ValidationCode.INVALID_RANGE),
            InputValidator.validateRange(-5.0)
        )
    }

    @Test fun overMaxRange_fails() {
        assertTrue(InputValidator.validateRange(99999.0) is InputValidator.Result.Invalid)
    }

    @Test fun missingRange_reportsInputRequired() {
        assertEquals(
            InputValidator.Result.Invalid(ValidationCode.INPUT_REQUIRED),
            InputValidator.validateRangeEntered(0.0)
        )
    }

    @Test fun notANumber_fails() {
        assertTrue(InputValidator.validateTemperature(Double.NaN) is InputValidator.Result.Invalid)
    }

    @Test fun clock_bounds() {
        assertTrue(InputValidator.validateClock(0) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateClock(13) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateClock(6) is InputValidator.Result.Valid)
    }

    @Test fun humidity_isBoundedToZeroHundred() {
        assertTrue(InputValidator.validateHumidity(-1.0) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateHumidity(101.0) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateHumidity(0.0) is InputValidator.Result.Valid)
        assertTrue(InputValidator.validateHumidity(100.0) is InputValidator.Result.Valid)
    }

    @Test fun temperatureAndAltitudeAndInclination_bounds() {
        assertTrue(InputValidator.validateTemperature(-100.0) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateTemperature(30.0) is InputValidator.Result.Valid)
        assertTrue(InputValidator.validateAltitude(50000.0) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateAltitude(1371.6) is InputValidator.Result.Valid)
        assertTrue(InputValidator.validateInclination(-90.0) is InputValidator.Result.Invalid)
        assertTrue(InputValidator.validateInclination(8.0) is InputValidator.Result.Valid)
    }

    @Test fun validateAll_reportsFirstProblem() {
        val r = InputValidator.validateAll(
            rangeM = 100.0, windMs = 999.0, clockHour = 3, tempC = 20.0,
            altM = 0.0, humidityPct = 50.0, inclinationDeg = 0.0
        )
        assertEquals(InputValidator.Result.Invalid(ValidationCode.INVALID_WIND_SPEED), r)
    }

    @Test fun validateAll_allGood() {
        val r = InputValidator.validateAll(650.0, 8.0, 3, 30.0, 1371.6, 50.0, 8.0)
        assertTrue(r is InputValidator.Result.Valid)
    }

    @Test fun everyCodeCarriesAnEnglishFallbackMessage() {
        ValidationCode.entries.forEach { code ->
            assertTrue("${code.name} needs a message", code.defaultMessage.isNotBlank())
        }
    }
}
