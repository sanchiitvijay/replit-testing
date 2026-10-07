package app.lastcalled.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.lastcalled.ui.screens.PermissionGate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionGateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contactsStepOffersContinueWithoutContacts() {
        composeRule.setContent {
            MaterialTheme {
                PermissionGate(
                    isCallLogStep = false,
                    permanentlyDenied = false,
                    onRequestCallLog = {},
                    onRequestContacts = {},
                    onOpenSettings = {},
                    onContinueWithoutContacts = {},
                )
            }
        }

        composeRule.onNodeWithText("Allow contacts").assertIsDisplayed()
        composeRule.onNodeWithText("Continue without contacts").assertIsDisplayed()
        composeRule.onNodeWithText(
            "calls still appear as unknown callers.",
            substring = true,
        )
            .assertExists()
    }
}
