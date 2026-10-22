package com.gabrieltagama.menuplanner.feature.calendar.lunch

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MenuShape
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import com.gabrieltagama.menuplanner.feature.calendar.R
import com.gabrieltagama.menuplanner.feature.calendar.editor.EditorStatus
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Stateless LunchEditorScreen: a dish meant for dinner can be chosen for lunch and its card shows a
 * warning, while a lunch or any-time dish shows none.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class LunchEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val warning get() = context.getString(R.string.day_editor_dinner_dish_warning)

    @Test
    fun dinnerDishShowsWarning() {
        setScreen(single(name = "Tortilla francesa", mealTime = MealTime.DINNER))

        composeRule.onNodeWithText("Tortilla francesa").assertIsDisplayed()
        composeRule.onNodeWithText(warning).assertIsDisplayed()
    }

    @Test
    fun anyTimeDishShowsNoWarning() {
        setScreen(single(name = "Paella", mealTime = MealTime.ANY))

        composeRule.onNodeWithText("Paella").assertIsDisplayed()
        composeRule.onNodeWithText(warning).assertDoesNotExist()
    }

    private fun single(name: String, mealTime: MealTime) =
        Dish(id = DishId(name), name = name, type = DishType.SINGLE, heaviness = Heaviness.MEDIUM, mealTime = mealTime, updatedAt = Instant.EPOCH)

    private fun setScreen(dish: Dish) = composeRule.setContent {
        MaterialTheme {
            LunchEditorScreen(
                state = LunchEditorUiState(
                    date = LocalDate.of(2026, 10, 9),
                    form = LunchEditorForm(mode = MenuShape.SINGLE, selections = mapOf(MenuSlot.SINGLE to dish)),
                    status = EditorStatus(isLoading = false),
                    options = mapOf(DishType.SINGLE to listOf(dish)),
                    isOptionsLoading = false
                ),
                actions = LunchEditorActions()
            )
        }
    }
}
