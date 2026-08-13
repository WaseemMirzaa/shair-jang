package com.codetivelab.fieldcalc

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.codetivelab.fieldcalc.ui.admin.TAG_ADMIN_BACK
import com.codetivelab.fieldcalc.ui.calculator.CalculatorTags
import com.codetivelab.fieldcalc.ui.calculator.Field
import com.codetivelab.fieldcalc.ui.components.KeypadTags
import com.codetivelab.fieldcalc.ui.profile.TAG_PROFILE_LIST
import com.codetivelab.fieldcalc.ui.settings.TAG_SETTINGS_BACK
import com.codetivelab.fieldcalc.ui.settings.TAG_SETTINGS_IMPERIAL
import com.codetivelab.fieldcalc.ui.settings.TAG_SETTINGS_METRIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Profile selection, unit switching and the admin gate. */
@RunWith(AndroidJUnit4::class)
class ProfileAndSettingsTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun awaitProfile() = rule.waitUntil(timeoutMillis = 20_000) {
        rule.hasTag(CalculatorTags.PROFILE_NAME) &&
            rule.textOf(CalculatorTags.PROFILE_NAME).let { it.isNotBlank() && it != "NO PROFILE" }
    }

    private fun openMenuItem(label: String) {
        rule.onNodeWithTag(KeypadTags.MENU).performClick()
        rule.onNodeWithText(label).performClick()
        rule.waitForIdle()
    }

    private fun backToCalculator() =
        rule.waitUntil(timeoutMillis = 10_000) { rule.hasTag(CalculatorTags.STATUS) }

    @Test fun operator_canSelectAnotherProfile() {
        // Pick whichever seeded profile is not currently active, so the test never trivially passes.
        val current = rule.textOf(CalculatorTags.PROFILE_NAME)
        val target = if (current == "DEMO PROFILE") "TRAINING PROFILE" else "DEMO PROFILE"

        openMenuItem("SELECT PROFILE")
        rule.onNodeWithTag(TAG_PROFILE_LIST).assertIsDisplayed()
        rule.onNodeWithText(target).performClick()

        backToCalculator()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.textOf(CalculatorTags.PROFILE_NAME) == target
        }
        assertEquals(target, rule.textOf(CalculatorTags.PROFILE_NAME))
    }

    @Test fun switchingUnits_convertsTheDisplayedValues() {
        // Start from metric and put a known 600 m into RANGE.
        openMenuItem("SETTINGS")
        rule.onNodeWithTag(TAG_SETTINGS_METRIC).performClick()
        rule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
        backToCalculator()

        rule.onNodeWithTag(CalculatorTags.field(Field.RANGE)).performClick()
        rule.onNodeWithTag(KeypadTags.CLEAR).performClick()
        listOf(6, 0, 0).forEach { rule.onNodeWithTag(KeypadTags.digit(it)).performClick() }
        rule.onNodeWithTag(KeypadTags.ENTER).performClick()

        // Switch to imperial: 600 m ≈ 656 yd.
        openMenuItem("SETTINGS")
        rule.onNodeWithTag(TAG_SETTINGS_IMPERIAL).performClick()
        rule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
        backToCalculator()

        rule.waitUntil(timeoutMillis = 10_000) {
            rule.textOf(CalculatorTags.value(Field.RANGE)).removeSuffix("_") == "656"
        }

        // Leave the app in metric for whatever runs next.
        openMenuItem("SETTINGS")
        rule.onNodeWithTag(TAG_SETTINGS_METRIC).performClick()
        rule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
        backToCalculator()
    }

    @Test fun adminArea_isPinGated() {
        openMenuItem("ADMIN")

        // Without the PIN the profile-management panel must not be reachable.
        assertTrue(rule.hasTag(TAG_ADMIN_BACK))
        rule.onNodeWithText("CREATE / EDIT / DELETE PROTECTED DATA").assertDoesNotExist()

        rule.onNodeWithTag(TAG_ADMIN_BACK).performClick()
        backToCalculator()
    }
}
