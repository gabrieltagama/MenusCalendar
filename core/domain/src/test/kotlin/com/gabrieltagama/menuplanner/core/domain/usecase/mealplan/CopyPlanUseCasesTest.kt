package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Copy and paste of lunches and dinners: the gaps between copied days are kept from the target
 * date, empty copied days are skipped, existing days are overwritten, and undoing removes what
 * was pasted and puts back what was replaced.
 */
class CopyPlanUseCasesTest {
    private val monday = LocalDate.of(2026, 3, 2)
    private val wednesday = monday.plusDays(2)
    private val nextMonday = monday.plusDays(7)
    private val lentils = DailyMenu.Single(single = dish(name = "Lentejas", type = DishType.SINGLE))
    private val paella = DailyMenu.Single(single = dish(name = "Paella", type = DishType.SINGLE))
    private fun lunch(date: LocalDate, menu: DailyMenu) = LunchDay(date = date, menu = menu, updatedAt = Instant.EPOCH)

    @Test
    fun `pastes lunches keeping the gaps and overwriting the target`() = runBlocking {
        val existing = lunch(nextMonday.plusDays(2), lentils)
        val repository = FakeLunchPlanRepository(listOf(lunch(monday, lentils), lunch(wednesday, paella), existing))

        val paste = PasteLunchDaysUseCase(repository, TestDishes.fixedClock)((0L..2L).map(monday::plusDays).toSet(), nextMonday)

        assertEquals(listOf(nextMonday, nextMonday.plusDays(2)), paste.pasted.map(LunchDay::date))
        assertEquals(listOf(existing), paste.replaced)
        assertEquals(lentils, repository.current[nextMonday]?.menu)
        assertEquals(paella, repository.current[nextMonday.plusDays(2)]?.menu)
        assertEquals(TestDishes.FIXED_INSTANT, repository.current[nextMonday]?.updatedAt)
    }

    @Test
    fun `undoing a lunch paste restores the target`() = runBlocking {
        val existing = lunch(nextMonday.plusDays(2), lentils)
        val repository = FakeLunchPlanRepository(listOf(lunch(monday, lentils), lunch(wednesday, paella), existing))
        val paste = PasteLunchDaysUseCase(repository, TestDishes.fixedClock)(setOf(monday, wednesday), nextMonday)

        UndoPasteLunchDaysUseCase(repository)(paste)

        assertEquals(setOf(monday, wednesday, existing.date), repository.current.keys)
        assertEquals(existing, repository.current[existing.date])
    }

    @Test
    fun `pasting days without plan writes nothing`() = runBlocking {
        val repository = FakeLunchPlanRepository()

        val paste = PasteLunchDaysUseCase(repository, TestDishes.fixedClock)(setOf(monday), nextMonday)

        assertEquals(0, paste.count)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `pastes and undoes dinners`() = runBlocking {
        val soup = DinnerDay(date = monday, dish = dish(name = "Sopa", mealTime = MealTime.DINNER), updatedAt = Instant.EPOCH)
        val repository = FakeDinnerPlanRepository(listOf(soup))

        val paste = PasteDinnerDaysUseCase(repository, TestDishes.fixedClock)(setOf(monday), wednesday)
        assertEquals(soup.dish, repository.current[wednesday]?.dish)

        UndoPasteDinnerDaysUseCase(repository)(paste)
        assertEquals(setOf(monday), repository.current.keys)
    }
}
