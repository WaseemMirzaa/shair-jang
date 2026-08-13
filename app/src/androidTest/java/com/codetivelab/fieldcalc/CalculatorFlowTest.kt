package com.codetivelab.fieldcalc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codetivelab.fieldcalc.ui.calculator.CalculatorTags
import com.codetivelab.fieldcalc.ui.calculator.Field
import com.codetivelab.fieldcalc.ui.components.KeypadTags
import com.codetivelab.fieldcalc.ui.results.ResultsTags
import com.codetivelab.fieldcalc.ui.results.TAG_TRAJECTORY_GRAPH
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end operator flow on a device/emulator:
 * keypad entry → CLEAR → BACK → ENTER → SOLVE → results → table → graph → back.
 */
@RunWith(AndroidJUnit4::class)
class CalculatorFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** The built-in profiles are seeded on a background coroutine at startup. */
    private fun awaitReady() = rule.waitUntil(timeoutMillis = 10_000) {
        rule.hasTag(CalculatorTags.PROFILE_NAME)
    }

    private fun enter(field: Field, vararg digits: Int) {
        rule.onNodeWithTag(CalculatorTags.field(field)).performClick()
        rule.onNodeWithTag(KeypadTags.CLEAR).performClick()
        digits.forEach { rule.onNodeWithTag(KeypadTags.digit(it)).performClick() }
    }

    @Test fun digitKeys_enterAValueIntoTheSelectedField() {
        awaitReady()
        enter(Field.RANGE, 4, 5, 0)
        // The selected field shows a trailing caret.
        rule.onNodeWithTag(CalculatorTags.value(Field.RANGE)).assertTextEquals("450_")
    }

    @Test fun clearKey_emptiesTheEditBuffer() {
        awaitReady()
        rule.onNodeWithTag(CalculatorTags.field(Field.WIND)).performClick()
        rule.onNodeWithTag(KeypadTags.digit(9)).performClick()
        rule.onNodeWithTag(KeypadTags.digit(9)).performClick()
        rule.onNodeWithTag(KeypadTags.CLEAR).performClick()

        // With an empty buffer the field falls back to its committed value, not "99".
        val shown = rule.textOf(CalculatorTags.value(Field.WIND))
        assertTrue("CLEAR left the buffer as $shown", !shown.startsWith("99"))
    }

    @Test fun backKey_deletesTheLastDigit() {
        awaitReady()
        enter(Field.RANGE, 1, 2, 3)
        rule.onNodeWithTag(KeypadTags.BACK).performClick()
        rule.onNodeWithTag(CalculatorTags.value(Field.RANGE)).assertTextEquals("12_")
    }

    @Test fun enterKey_commitsAndAdvancesToTheNextField() {
        awaitReady()
        enter(Field.RANGE, 6, 0, 0)
        rule.onNodeWithTag(KeypadTags.ENTER).performClick()

        // RANGE keeps the committed value (caret gone) and WIND now carries it.
        rule.onNodeWithTag(CalculatorTags.value(Field.RANGE)).assertTextEquals("600")
        val wind = rule.textOf(CalculatorTags.value(Field.WIND))
        assertTrue("ENTER did not move the selection; WIND showed $wind", wind.endsWith("_"))
    }

    @Test fun downAndUpKeys_walkTheFieldList() {
        awaitReady()
        rule.onNodeWithTag(CalculatorTags.field(Field.RANGE)).performClick()
        rule.onNodeWithTag(KeypadTags.DOWN).performClick()
        assertTrue(rule.textOf(CalculatorTags.value(Field.WIND)).endsWith("_"))

        rule.onNodeWithTag(KeypadTags.UP).performClick()
        assertTrue(rule.textOf(CalculatorTags.value(Field.RANGE)).endsWith("_"))
    }

    @Test fun solve_showsTheResultThenTableThenGraphThenReturns() {
        awaitReady()
        enter(Field.RANGE, 6, 5, 0)

        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()
        rule.waitUntil(timeoutMillis = 10_000) { rule.hasTag(ResultsTags.TITLE) }
        rule.onNodeWithTag(ResultsTags.SUMMARY).assertIsDisplayed()

        rule.onNodeWithTag(ResultsTags.BTN_TABLE).performClick()
        rule.onNodeWithTag(ResultsTags.TABLE).assertIsDisplayed()

        rule.onNodeWithTag(ResultsTags.BTN_GRAPH).performClick()
        rule.onNodeWithTag(TAG_TRAJECTORY_GRAPH).assertIsDisplayed()

        rule.onNodeWithTag(ResultsTags.BTN_BACK).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { rule.hasTag(CalculatorTags.STATUS) }
    }

    @Test fun back_fromResults_staysOnTheCalculator() {
        awaitReady()
        enter(Field.RANGE, 5, 0, 0)
        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()
        rule.waitUntil(timeoutMillis = 10_000) { rule.hasTag(ResultsTags.TITLE) }

        rule.onNodeWithTag(ResultsTags.BTN_BACK).performClick()
        rule.waitForIdle()
        // A stale result must not bounce the operator forward again.
        assertTrue(rule.hasTag(CalculatorTags.STATUS))
        assertTrue(!rule.hasTag(ResultsTags.TITLE))
    }

    @Test fun invalidInput_showsAnOperatorMessageAndDoesNotNavigate() {
        awaitReady()
        enter(Field.HUMIDITY, 9, 9, 9)          // 999 % is out of range
        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()

        rule.waitForIdle()
        rule.onNodeWithTag(CalculatorTags.STATUS).assertTextEquals("INVALID HUMIDITY")
        assertTrue(!rule.hasTag(ResultsTags.TITLE))
    }
}
