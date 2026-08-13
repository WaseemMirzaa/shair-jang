package com.codetivelab.fieldcalc

import com.codetivelab.fieldcalc.domain.input.InputEditor
import com.codetivelab.fieldcalc.domain.input.InputField
import com.codetivelab.fieldcalc.domain.models.ClockDirection
import com.codetivelab.fieldcalc.domain.models.OperatorInput
import com.codetivelab.fieldcalc.domain.models.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** The keypad's entry rules, exercised without a device. */
class InputEditorTest {

    private val base = OperatorInput(
        rangeM = 650.0, windSpeedMs = 8.0, windDirection = ClockDirection(3),
        temperatureC = 30.0, altitudeM = 1371.6, humidityPct = 50.0, inclinationDeg = 8.0
    )

    // ---- Buffer editing -------------------------------------------------------------------

    @Test fun digits_appendInOrder() {
        var b = ""
        listOf(6, 5, 0).forEach { b = InputEditor.appendDigit(b, it) }
        assertEquals("650", b)
    }

    @Test fun leadingZero_isReplacedNotKept() {
        assertEquals("5", InputEditor.appendDigit("0", 5))
        assertEquals("0", InputEditor.appendDigit("", 0))
        assertEquals("0", InputEditor.appendDigit("0", 0))
    }

    @Test fun entryIsCappedAtTheDigitLimit() {
        var b = ""
        repeat(20) { b = InputEditor.appendDigit(b, 9) }
        assertEquals(InputEditor.MAX_DIGITS, b.length)
    }

    @Test fun decimalPoint_isAddedOnceAndSeedsALeadingZero() {
        assertEquals("0.", InputEditor.appendDecimalPoint("", InputField.WIND))
        assertEquals("8.", InputEditor.appendDecimalPoint("8", InputField.WIND))
        assertEquals("8.5", InputEditor.appendDecimalPoint("8.5", InputField.WIND))
    }

    @Test fun decimalPoint_isRefusedForWholeHourClockDirection() {
        assertEquals("3", InputEditor.appendDecimalPoint("3", InputField.DIRECTION))
    }

    @Test fun signKey_togglesBothWays() {
        assertEquals("-8", InputEditor.toggleSign("8"))
        assertEquals("8", InputEditor.toggleSign("-8"))
        assertEquals("-", InputEditor.toggleSign(""))
    }

    @Test fun backspace_removesOneCharacterAndStopsAtEmpty() {
        assertEquals("12", InputEditor.backspace("123"))
        assertEquals("", InputEditor.backspace("1"))
        assertEquals("", InputEditor.backspace(""))
    }

    // ---- Navigation -----------------------------------------------------------------------

    @Test fun fieldNavigation_wrapsInBothDirections() {
        assertEquals(InputField.WIND, InputEditor.next(InputField.RANGE))
        assertEquals(InputField.RANGE, InputEditor.next(InputField.INCLINATION))
        assertEquals(InputField.INCLINATION, InputEditor.previous(InputField.RANGE))
        assertEquals(InputField.RANGE, InputEditor.previous(InputField.WIND))
    }

    @Test fun everyFieldIsReachableByWalkingForward() {
        var field = InputField.RANGE
        val seen = mutableSetOf(field)
        repeat(InputField.entries.size - 1) {
            field = InputEditor.next(field)
            seen.add(field)
        }
        assertEquals(InputField.entries.toSet(), seen)
    }

    // ---- Commit ---------------------------------------------------------------------------

    @Test fun commit_metricRange_storesTheValueUnchanged() {
        val out = InputEditor.commit("450", InputField.RANGE, UnitSystem.METRIC, base)
        assertEquals(450.0, out.rangeM, 1e-9)
    }

    @Test fun commit_imperialRange_convertsYardsToMetres() {
        val out = InputEditor.commit("711", InputField.RANGE, UnitSystem.IMPERIAL, base)
        assertEquals(650.0, out.rangeM, 0.5)
    }

    @Test fun commit_imperialAltitude_convertsFeetToMetres() {
        val out = InputEditor.commit("4500", InputField.ALTITUDE, UnitSystem.IMPERIAL, base)
        assertEquals(1371.6, out.altitudeM, 1e-6)
    }

    @Test fun commit_imperialTemperature_convertsFahrenheit() {
        val out = InputEditor.commit("86", InputField.TEMPERATURE, UnitSystem.IMPERIAL, base)
        assertEquals(30.0, out.temperatureC, 1e-9)
    }

    @Test fun commit_negativeInclination_isKept() {
        val out = InputEditor.commit("-12", InputField.INCLINATION, UnitSystem.METRIC, base)
        assertEquals(-12.0, out.inclinationDeg, 1e-9)
    }

    @Test fun commit_clockDirection_isPulledIntoOneToTwelve() {
        assertEquals(12, InputEditor.commit("13", InputField.DIRECTION, UnitSystem.METRIC, base).windDirection.hour)
        assertEquals(1, InputEditor.commit("0", InputField.DIRECTION, UnitSystem.METRIC, base).windDirection.hour)
        assertEquals(9, InputEditor.commit("9", InputField.DIRECTION, UnitSystem.METRIC, base).windDirection.hour)
    }

    @Test fun commit_ofAHalfTypedBuffer_leavesTheInputAlone() {
        listOf("", "-", ".", "-.", "   ").forEach { buffer ->
            assertSame("buffer '$buffer' must not change anything", base,
                InputEditor.commit(buffer, InputField.RANGE, UnitSystem.METRIC, base))
        }
    }

    @Test fun parse_rejectsUnusableBuffers() {
        assertNull(InputEditor.parse(""))
        assertNull(InputEditor.parse("-"))
        assertNull(InputEditor.parse("."))
        assertEquals(8.5, InputEditor.parse("8.5")!!, 1e-9)
        assertEquals(-3.0, InputEditor.parse("-3")!!, 1e-9)
    }

    // ---- Display --------------------------------------------------------------------------

    @Test fun display_metric_showsStoredSiValues() {
        assertEquals("650", InputEditor.display(InputField.RANGE, base, UnitSystem.METRIC))
        assertEquals("8.0", InputEditor.display(InputField.WIND, base, UnitSystem.METRIC))
        assertEquals("30", InputEditor.display(InputField.TEMPERATURE, base, UnitSystem.METRIC))
        assertEquals("3", InputEditor.display(InputField.DIRECTION, base, UnitSystem.METRIC))
        assertEquals("50", InputEditor.display(InputField.HUMIDITY, base, UnitSystem.METRIC))
    }

    @Test fun display_imperial_convertsForTheOperator() {
        assertEquals("711", InputEditor.display(InputField.RANGE, base, UnitSystem.IMPERIAL))
        assertEquals("4500", InputEditor.display(InputField.ALTITUDE, base, UnitSystem.IMPERIAL))
        assertEquals("86", InputEditor.display(InputField.TEMPERATURE, base, UnitSystem.IMPERIAL))
        assertEquals("26.2", InputEditor.display(InputField.WIND, base, UnitSystem.IMPERIAL))
    }

    @Test fun display_inclination_carriesAnExplicitPlusSign() {
        assertEquals("+8", InputEditor.display(InputField.INCLINATION, base, UnitSystem.METRIC))
        assertEquals("-8", InputEditor.display(InputField.INCLINATION, base.copy(inclinationDeg = -8.0), UnitSystem.METRIC))
        assertEquals("0", InputEditor.display(InputField.INCLINATION, base.copy(inclinationDeg = 0.0), UnitSystem.METRIC))
    }

    @Test fun typingThenSwitchingUnits_keepsThePhysicalValue() {
        // Enter 600 yd, then read the field back in metric.
        val committed = InputEditor.commit("600", InputField.RANGE, UnitSystem.IMPERIAL, base)
        assertEquals("549", InputEditor.display(InputField.RANGE, committed, UnitSystem.METRIC))
        assertEquals("600", InputEditor.display(InputField.RANGE, committed, UnitSystem.IMPERIAL))
    }

    @Test fun fullEntrySequence_matchesWhatTheOperatorTyped() {
        // Select RANGE, CLEAR, type 4 5 0, ENTER.
        var buffer = ""
        listOf(4, 5, 0).forEach { buffer = InputEditor.appendDigit(buffer, it) }
        val afterEnter = InputEditor.commit(buffer, InputField.RANGE, UnitSystem.METRIC, base)

        assertEquals(450.0, afterEnter.rangeM, 1e-9)
        assertEquals(InputField.WIND, InputEditor.next(InputField.RANGE))
        // Everything else is untouched.
        assertEquals(base.copy(rangeM = 450.0), afterEnter)
    }
}
