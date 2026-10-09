package com.gabrieltagama.menuplanner.feature.auth.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.feature.auth.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.gabrieltagama.menuplanner.core.ui.R as CoreUiR

/**
 * Stateless LoginScreen: typed digits reach onPinChange, the submit button follows canSubmit
 * and a DomainError is rendered as its localized message.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun typingDigitsUpdatesPinAndEnablesSubmit() {
        val receivedPins = mutableListOf<String>()
        var submitCount = 0
        composeRule.setContent {
            var state by remember { mutableStateOf(LoginUiState()) }
            MaterialTheme {
                LoginScreen(
                    state = state,
                    onPinChange = { pin ->
                        receivedPins += pin
                        state = state.copy(pin = pin)
                    },
                    onSubmit = { submitCount++ }
                )
            }
        }
        val submit = composeRule.onNodeWithText(context.getString(R.string.login_submit))
        submit.assertIsNotEnabled()

        composeRule.onNode(hasSetTextAction()).performTextInput("1234")

        assertEquals("1234", receivedPins.last())
        submit.assertIsEnabled().performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun errorMessageIsShown() {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(
                    state = LoginUiState(error = DomainError.InvalidCredential),
                    onPinChange = {},
                    onSubmit = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(CoreUiR.string.error_invalid_credential)).assertIsDisplayed()
    }

    @Test
    fun pinFieldIsDisabledWhileLoading() {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(state = LoginUiState(pin = "1234", isLoading = true), onPinChange = {}, onSubmit = {})
            }
        }

        composeRule.onNode(hasSetTextAction()).assertIsNotEnabled()
    }
}
