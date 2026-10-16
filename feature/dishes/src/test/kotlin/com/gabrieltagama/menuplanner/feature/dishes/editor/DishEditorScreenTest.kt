package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.feature.dishes.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Stateless DishEditorScreen: top bar actions call their callbacks and form flags are rendered.
 */
@RunWith(AndroidJUnit4::class)
class DishEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun saveButtonTriggersCallback() {
        var saveCount = 0
        setScreen(DishEditorUiState(name = "Gazpacho"), DishEditorActions(onSave = { saveCount++ }))

        composeRule.onNodeWithText(context.getString(R.string.dish_editor_save)).performClick()

        assertEquals(1, saveCount)
    }

    @Test
    fun saveButtonIsDisabledWhileSaving() {
        setScreen(DishEditorUiState(name = "Gazpacho", isSaving = true), DishEditorActions())

        composeRule.onNodeWithText(context.getString(R.string.dish_editor_save)).assertIsNotEnabled()
    }

    @Test
    fun blankNameErrorIsShownAfterValidation() {
        setScreen(DishEditorUiState(showValidationErrors = true), DishEditorActions())

        composeRule.onNodeWithText(context.getString(R.string.dish_editor_name_required), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun editModeShowsDeleteActionThatTriggersCallback() {
        var deleteRequests = 0
        setScreen(
            DishEditorUiState(isEditMode = true, name = "Pollo asado", type = DishType.MAIN, heaviness = Heaviness.MEDIUM),
            DishEditorActions(onDeleteRequest = { deleteRequests++ })
        )

        composeRule.onNodeWithText(context.getString(R.string.dish_editor_title_edit)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.dish_editor_delete)).performClick()

        assertEquals(1, deleteRequests)
    }

    private fun setScreen(state: DishEditorUiState, actions: DishEditorActions) = composeRule.setContent {
        MaterialTheme {
            DishEditorScreen(state = state, actions = actions)
        }
    }
}
