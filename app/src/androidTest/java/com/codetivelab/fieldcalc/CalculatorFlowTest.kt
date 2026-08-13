package com.codetivelab.fieldcalc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codetivelab.fieldcalc.ui.calculator.CalculatorTags
import com.codetivelab.fieldcalc.ui.calculator.Field
import com.codetivelab.fieldcalc.ui.components.KeypadTags
import com.codetivelab.fieldcalc.ui.results.ResultsTags
import com.codetivelab.fieldcalc.ui.results.TAG_TRAJECTORY_GRAPH
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end operator flow on a device/emulator:
 * keypad entry → CLEAR → BACK → ENTER → SOLVE → results → table → graph → back.
 *
 * Note on lookups: the input rows are `Modifier.clickable`, so Compose merges their descendants.
 * Rows are therefore clicked by their own tag (merged tree) while the value inside a row is read
 * through [textOf] / `useUnmergedTree`.
 */
@RunWith(AndroidJUnit4::class)
class CalculatorFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun startFromAKnownState() {
        awaitProfile()
        resetInputs()
    }

    /** The built-in profiles are seeded on a background coroutine, so wait for a real one. */
    private fun awaitProfile() = rule.waitUntil(timeoutMillis = 20_000) {
        rule.hasTag(CalculatorTags.PROFILE_NAME) &&
            rule.textOf(CalculatorTags.PROFILE_NAME).let { it.isNotBlank() && it != "NO PROFILE" }
    }

    /** Operator inputs persist across launches, so each test resets them first. */
    private fun resetInputs() {
        rule.onNodeWithTag(KeypadTags.MENU).performClick()
        rule.onNodeWithText("RESET INPUTS").performClick()
        rule.waitForIdle()
    }

    private fun enter(field: Field, vararg digits: Int) {
        rule.onNodeWithTag(CalculatorTags.field(field)).performClick()
        rule.onNodeWithTag(KeypadTags.CLEAR).performClick()
        digits.forEach { rule.onNodeWithTag(KeypadTags.digit(it)).performClick() }
    }

    private fun valueOf(field: Field) =
        rule.onNodeWithTag(CalculatorTags.value(field), useUnmergedTree = true)

    @Test fun digitKeys_enterAValueIntoTheSelectedField() {
        enter(Field.RANGE, 4, 5, 0)
        // The selected field shows a trailing caret.
        valueOf(Field.RANGE).assertTextEquals("450_")
    }

    @Test fun clearKey_emptiesTheEditBuffer() {
        rule.onNodeWithTag(CalculatorTags.field(Field.WIND)).performClick()
        rule.onNodeWithTag(KeypadTags.digit(9)).performClick()
        rule.onNodeWithTag(KeypadTags.digit(9)).performClick()
        rule.onNodeWithTag(KeypadTags.CLEAR).performClick()

        // With an empty buffer the field falls back to its committed value, not "99".
        val shown = rule.textOf(CalculatorTags.value(Field.WIND))
        assertTrue("CLEAR left the buffer as $shown", !shown.startsWith("99"))
    }

    @Test fun backKey_deletesTheLastDigit() {
        enter(Field.RANGE, 1, 2, 3)
        rule.onNodeWithTag(KeypadTags.BACK).performClick()
        valueOf(Field.RANGE).assertTextEquals("12_")
    }

    @Test fun enterKey_commitsAndAdvancesToTheNextField() {
        enter(Field.RANGE, 6, 0, 0)
        rule.onNodeWithTag(KeypadTags.ENTER).performClick()

        // RANGE keeps the committed value (caret gone) and WIND now carries it.
        valueOf(Field.RANGE).assertTextEquals("600")
        val wind = rule.textOf(CalculatorTags.value(Field.WIND))
        assertTrue("ENTER did not move the selection; WIND showed $wind", wind.endsWith("_"))
    }

    @Test fun downAndUpKeys_walkTheFieldList() {
        rule.onNodeWithTag(CalculatorTags.field(Field.RANGE)).performClick()
        rule.onNodeWithTag(KeypadTags.DOWN).performClick()
        assertTrue(rule.textOf(CalculatorTags.value(Field.WIND)).endsWith("_"))

        rule.onNodeWithTag(KeypadTags.UP).performClick()
        assertTrue(rule.textOf(CalculatorTags.value(Field.RANGE)).endsWith("_"))
    }

    @Test fun solve_showsTheResultThenTableThenGraphThenReturns() {
        enter(Field.RANGE, 6, 5, 0)

        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()
        rule.waitUntil(timeoutMillis = 20_000) { rule.hasTag(ResultsTags.TITLE) }
        rule.onNodeWithTag(ResultsTags.SUMMARY).assertIsDisplayed()

        rule.onNodeWithTag(ResultsTags.BTN_TABLE).performClick()
        rule.onNodeWithTag(ResultsTags.TABLE).assertIsDisplayed()

        rule.onNodeWithTag(ResultsTags.BTN_GRAPH).performClick()
        rule.onNodeWithTag(TAG_TRAJECTORY_GRAPH).assertIsDisplayed()

        rule.onNodeWithTag(ResultsTags.BTN_BACK).performClick()
        rule.waitUntil(timeoutMillis = 10_000) { rule.hasTag(CalculatorTags.STATUS) }
    }

    @Test fun back_fromResults_staysOnTheCalculator() {
        enter(Field.RANGE, 5, 0, 0)
        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()
        rule.waitUntil(timeoutMillis = 20_000) { rule.hasTag(ResultsTags.TITLE) }

        rule.onNodeWithTag(ResultsTags.BTN_BACK).performClick()
        rule.waitUntil(timeoutMillis = 10_000) { rule.hasTag(CalculatorTags.STATUS) }
        rule.waitForIdle()
        // A stale result must not bounce the operator forward again.
        assertTrue("the results screen came back on its own", !rule.hasTag(ResultsTags.TITLE))
    }

    @Test fun invalidInput_showsAnOperatorMessageAndDoesNotNavigate() {
        enter(Field.RANGE, 5, 0, 0)             // a valid range, so humidity is what fails
        enter(Field.HUMIDITY, 9, 9, 9)          // 999 % is out of range
        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()

        rule.waitUntil(timeoutMillis = 10_000) {
            rule.textOf(CalculatorTags.STATUS) == "INVALID HUMIDITY"
        }
        assertTrue("an invalid solve must not navigate", !rule.hasTag(ResultsTags.TITLE))
    }

    @Test fun missingRange_isReportedAsInputRequired() {
        // RESET leaves the range at zero, so SOLVE must refuse it.
        rule.onNodeWithTag(KeypadTags.SOLVE).performClick()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.textOf(CalculatorTags.STATUS) == "INPUT REQUIRED"
        }
    }
}
