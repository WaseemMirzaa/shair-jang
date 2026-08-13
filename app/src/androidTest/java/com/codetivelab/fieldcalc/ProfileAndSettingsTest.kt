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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Profile selection, unit switching and the admin gate. */
@RunWith(AndroidJUnit4::class)
class ProfileAndSettingsTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun awaitReady() = rule.waitUntil(timeoutMillis = 10_000) {
        rule.hasTag(CalculatorTags.PROFILE_NAME)
    }

    private fun openMenuItem(label: String) {
        rule.onNodeWithTag(KeypadTags.MENU).performClick()
        rule.onNodeWithText(label).performClick()
    }

    @Test fun operator_canSelectAnotherProfile() {
        awaitReady()
        openMenuItem("SELECT PROFILE")
        rule.onNodeWithTag(TAG_PROFILE_LIST).assertIsDisplayed()

        rule.onNodeWithText("TRAINING PROFILE").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.textOf(CalculatorTags.PROFILE_NAME) == "TRAINING PROFILE"
        }
        assertEquals("TRAINING PROFILE", rule.textOf(CalculatorTags.PROFILE_NAME))
    }

    @Test fun switchingUnits_convertsTheDisplayedValues() {
        awaitReady()

        // Put a known 600 m into RANGE while metric.
        openMenuItem("SETTINGS")
        rule.onNodeWithTag(TAG_SETTINGS_METRIC).performClick()
        rule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { rule.hasTag(CalculatorTags.STATUS) }

        rule.onNodeWithTag(CalculatorTags.field(Field.RANGE)).performClick()
        rule.onNodeWithTag(KeypadTags.CLEAR).performClick()
        listOf(6, 0, 0).forEach { rule.onNodeWithTag(KeypadTags.digit(it)).performClick() }
        rule.onNodeWithTag(KeypadTags.ENTER).performClick()

        // Switch to imperial: 600 m ≈ 656 yd.
        openMenuItem("SETTINGS")
        rule.onNodeWithTag(TAG_SETTINGS_IMPERIAL).performClick()
        rule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { rule.hasTag(CalculatorTags.STATUS) }

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.textOf(CalculatorTags.value(Field.RANGE)).removeSuffix("_") == "656"
        }

        // Put it back so the next test starts from metric.
        openMenuItem("SETTINGS")
        rule.onNodeWithTag(TAG_SETTINGS_METRIC).performClick()
        rule.onNodeWithTag(TAG_SETTINGS_BACK).performClick()
    }

    @Test fun adminArea_isPinGated() {
        awaitReady()
        openMenuItem("ADMIN")

        // Without the PIN the profile-management panel must not be reachable.
        rule.waitForIdle()
        assertTrue(rule.hasTag(TAG_ADMIN_BACK))
        rule.onNodeWithText("CREATE / EDIT / DELETE PROTECTED DATA").assertDoesNotExist()

        rule.onNodeWithTag(TAG_ADMIN_BACK).performClick()
        rule.waitUntil(timeoutMillis = 5_000) { rule.hasTag(CalculatorTags.STATUS) }
    }
}
