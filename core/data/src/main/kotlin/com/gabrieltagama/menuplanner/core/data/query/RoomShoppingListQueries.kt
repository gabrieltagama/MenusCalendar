package com.gabrieltagama.menuplanner.core.data.query

import com.gabrieltagama.menuplanner.core.data.database.dao.PlanQueryDao
import com.gabrieltagama.menuplanner.core.data.mapper.toDomain
import com.gabrieltagama.menuplanner.core.domain.model.Ingredient
import com.gabrieltagama.menuplanner.core.domain.model.Meal
import com.gabrieltagama.menuplanner.core.domain.query.ShoppingListQueries
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Room-backed read side of the shopping list: the ingredient lines of the planned dishes come
 * straight from SQL, without building dishes or planned days.
 */
internal class RoomShoppingListQueries @Inject constructor(private val dao: PlanQueryDao) : ShoppingListQueries {

    override fun observeIngredientLines(dates: ClosedRange<LocalDate>, meal: Meal): Flow<List<Ingredient>> {
        val rows = when (meal) {
            Meal.LUNCH -> dao.observeLunchIngredientLines(dates.start, dates.endInclusive)
            Meal.DINNER -> dao.observeDinnerIngredientLines(dates.start, dates.endInclusive)
        }
        return rows.map { lines -> lines.map { it.toDomain() } }.distinctUntilChanged()
    }
}
