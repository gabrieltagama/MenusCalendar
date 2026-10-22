package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.PlanningError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.fake.TestPlanCommands
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.MenuSlot
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DayPlanCommands of each meal: reading and clearing one day, saving a lunch with the slot rules and
 * the clock's time, deleting only the given planned dates, pasting copied days keeping their gaps and
 * overwriting the target, and undoing both so the plan is back as it was.
 */
class DayPlanCommandsTest {
    private val monday = LocalDate.of(2026, 3, 2)
    private val wednesday = monday.plusDays(2)
    private val nextMonday = monday.plusDays(7)
    private val starter = dish(type = DishType.STARTER)
    private val main = dish(type = DishType.MAIN)
    private val lentils = DailyMenu.Single(single = dish(name = "Lentejas", type = DishType.SINGLE))
    private val paella = DailyMenu.Single(single = dish(name = "Paella", type = DishType.SINGLE))

    private fun lunch(date: LocalDate, menu: DailyMenu = lentils) = LunchDay(date = date, menu = menu, updatedAt = Instant.EPOCH)

    private fun lunchCommands(vararg days: LunchDay): Pair<LunchPlanCommands, FakeLunchPlanRepository> {
        val repository = FakeLunchPlanRepository(days.toList())
        return TestPlanCommands(lunches = repository).lunch to repository
    }

    @Test
    fun `get and clear one day`() = runBlocking {
        val (commands, repository) = lunchCommands(lunch(monday))

        assertEquals(lunch(monday), commands.getDay(monday))
        assertNull(commands.getDay(wednesday))

        commands.clearDay(monday)

        assertEquals(listOf(monday), repository.cleared)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `saves a valid lunch with the time of the clock`() = runBlocking {
        val (commands, repository) = lunchCommands()
        val menu = DailyMenu.Courses(starter, main)

        val expected = LunchDay(monday, menu, TestDishes.FIXED_INSTANT)
        assertEquals(Outcome.Success(expected), commands.save(monday, menu))
        assertEquals(listOf(expected), repository.saved)
    }

    @Test
    fun `rejects a lunch with a dish out of its slot and saves nothing`() = runBlocking {
        val (commands, repository) = lunchCommands()

        assertEquals(Outcome.Failure(PlanningError.WrongDishType(MenuSlot.STARTER, DishType.STARTER)), commands.save(monday, DailyMenu.Courses(main, main)))
        assertEquals(Outcome.Failure(PlanningError.WrongDishType(MenuSlot.SINGLE, DishType.SINGLE)), commands.save(monday, DailyMenu.Single(main)))
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `deletes only the planned selected days and undo restores them`() = runBlocking {
        val untouched = monday.plusDays(1)
        val days = listOf(lunch(monday), lunch(wednesday), lunch(untouched))
        val (commands, repository) = lunchCommands(*days.toTypedArray())

        val change = commands.delete(setOf(monday, wednesday, LocalDate.of(2026, 3, 20)))

        assertEquals(2, change.count)
        assertEquals(setOf(untouched), repository.current.keys)

        change.undo()

        assertEquals(days.associateBy(LunchDay::date), repository.current)
    }

    @Test
    fun `deleting nothing touches nothing`() = runBlocking {
        val (commands, repository) = lunchCommands(lunch(monday))

        assertEquals(0, commands.delete(emptySet()).count)
        assertTrue(repository.cleared.isEmpty())
    }

    @Test
    fun `pastes lunches keeping the gaps and overwriting the target, and undo restores the target`() = runBlocking {
        val existing = lunch(nextMonday.plusDays(2), lentils)
        val (commands, repository) = lunchCommands(lunch(monday, lentils), lunch(wednesday, paella), existing)

        val change = commands.paste((0L..2L).map(monday::plusDays).toSet(), nextMonday)

        assertEquals(2, change.count)
        assertEquals(lentils, repository.current[nextMonday]?.menu)
        assertEquals(paella, repository.current[nextMonday.plusDays(2)]?.menu)
        assertEquals(TestDishes.FIXED_INSTANT, repository.current[nextMonday]?.updatedAt)

        change.undo()

        assertEquals(setOf(monday, wednesday, existing.date), repository.current.keys)
        assertEquals(existing, repository.current[existing.date])
    }

    @Test
    fun `pasting days without plan writes nothing`() = runBlocking {
        val (commands, repository) = lunchCommands()

        assertEquals(0, commands.paste(setOf(monday), nextMonday).count)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `deletes, pastes and undoes dinners`() = runBlocking {
        val soup = DinnerDay(date = monday, dish = dish(name = "Sopa", mealTime = MealTime.DINNER), updatedAt = Instant.EPOCH)
        val repository = FakeDinnerPlanRepository(listOf(soup))
        val commands = TestPlanCommands(dinners = repository).dinner

        val paste = commands.paste(setOf(monday), wednesday)
        assertEquals(soup.dish, repository.current[wednesday]?.dish)
        paste.undo()
        assertEquals(setOf(monday), repository.current.keys)

        val deletion = commands.delete(setOf(monday))
        assertTrue(repository.current.isEmpty())
        deletion.undo()
        assertEquals(soup, repository.current[monday])
    }
}
