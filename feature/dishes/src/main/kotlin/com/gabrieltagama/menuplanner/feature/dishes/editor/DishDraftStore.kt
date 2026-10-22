package com.gabrieltagama.menuplanner.feature.dishes.editor

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.json.Json

/**
 * Keeps the dish form in the SavedStateHandle, so a recipe being typed survives the system killing
 * the app: the form as it is now (current) and as it was opened (opened), which tells whether there
 * are unsaved changes. A stored text that cannot be read, such as one written by an older version,
 * counts as no form.
 */
internal class DishDraftStore(private val handle: SavedStateHandle) {

    var current: DishForm?
        get() = read(KEY_FORM)
        set(value) = write(KEY_FORM, value)

    var opened: DishForm?
        get() = read(KEY_OPENED_FORM)
        set(value) = write(KEY_OPENED_FORM, value)

    private fun read(key: String): DishForm? =
        handle.get<String>(key)?.let { text ->
            try {
                json.decodeFromString(DishForm.serializer(), text)
            } catch (expected: IllegalArgumentException) {
                null
            }
        }

    private fun write(key: String, form: DishForm?) {
        handle[key] = form?.let { json.encodeToString(DishForm.serializer(), it) }
    }

    private companion object {
        const val KEY_FORM = "dish_form"
        const val KEY_OPENED_FORM = "dish_form_opened"
        val json = Json { ignoreUnknownKeys = true }
    }
}
