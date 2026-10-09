package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.MealDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Property-style tests of AutoFillMonthUseCase (and MenuPicker) over many seeds: only empty days are
 * filled, menu shapes and desserts are mixed, no dish repeats within 7 days (also across the month
 * boundary), no VERY_HIGH main dish follows a heavy day, and the fallback order when dishes are scarce
 * is "repeat first, break the heavy rule last".
 */
class AutoFillMonthUseCaseTest {
    private val month = YearMonth.of(2026, 3)
    private val seeds = 1..50

    private class Run(val result: AutoFillResult, val repository: FakeMealPlanRepository) {
        val plan: Map<LocalDate, DailyMenu> get() = repository.current.mapValues { it.value.menu }
    }

    private fun autoFill(dishes: List<Dish>, seed: Int, planned: List<MealDay> = emptyList()): Run = runBlocking {
        val repository = FakeMealPlanRepository(planned)
        val useCase = AutoFillMonthUseCase(FakeDishRepository(dishes), repository, Random(seed), TestDishes.fixedClock)
        Run(useCase(month), repository)
    }

    private fun dishesOf(type: DishType, light: Int, heavy: Int): List<Dish> =
        (1..light).map { dish(id = "$type-light-$it", type = type, heaviness = if (it % 2 == 0) Heaviness.MEDIUM else Heaviness.VERY_LOW) } +
            (1..heavy).map { dish(id = "$type-heavy-$it", type = type, heaviness = Heaviness.VERY_HIGH) }

    private val richCatalog: List<Dish> = DishType.entries.flatMap { dishesOf(it, light = 8, heavy = 3) }

    private fun DailyMenu.isHeavy(): Boolean = mainDishes.any { it.heaviness == Heaviness.VERY_HIGH }

    private fun monthDays(): List<LocalDate> = (1..month.lengthOfMonth()).map(month::atDay)

    private fun assertNoRepeatWithinWeek(plan: Map<LocalDate, DailyMenu>, seed: Int) {
        val dates = plan.keys.sorted()
        for (first in dates) for (second in dates) {
            val gap = ChronoUnit.DAYS.between(first, second)
            if (gap !in 1..6) continue
            val shared = plan.getValue(first).dishes.map(Dish::id).intersect(plan.getValue(second).dishes.map(Dish::id).toSet())
            if (shared.isNotEmpty()) fail("seed $seed: $first and $second share $shared")
        }
    }

    private fun assertNoHeavyStreak(plan: Map<LocalDate, DailyMenu>, seed: Int, days: List<LocalDate> = monthDays()) {
        for (date in days) {
            val previous = plan[date.minusDays(1)] ?: continue
            val current = plan[date] ?: continue
            if (previous.isHeavy() && current.isHeavy()) fail("seed $seed: heavy streak on $date")
        }
    }

    private fun countRepeatedDays(plan: Map<LocalDate, DailyMenu>): Int = monthDays().count { date ->
        val ids = plan[date]?.dishes.orEmpty().map(Dish::id)
        (1..6L).any { back -> plan[date.minusDays(back)]?.dishes.orEmpty().any { it.id in ids } }
    }

    private fun assertSlotsMatchTypes(menu: DailyMenu) {
        when (menu) {
            is DailyMenu.Courses -> {
                assertEquals(DishType.STARTER, menu.starter.type)
                assertEquals(DishType.MAIN, menu.main.type)
            }
            is DailyMenu.Single -> assertEquals(DishType.SINGLE, menu.single.type)
        }
        menu.dessert?.let { assertEquals(DishType.DESSERT, it.type) }
    }

    private fun heavyDay(date: LocalDate) =
        MealDay(date, DailyMenu.Single(dish(id = "planned-heavy-$date", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)), TestDishes.OLD_INSTANT)

    @Test
    fun `fills every day of an empty month and reports no fallbacks with a rich catalog`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed)

        assertEquals(AutoFillResult(filledDays = 31, repeatedDishDays = 0, heavyStreakDays = 0, unfilledDays = 0), run.result)
        assertEquals(monthDays().toSet(), run.plan.keys)
    }

    @Test
    fun `only empty days are filled and planned days are untouched`() = seeds.forEach { seed ->
        val planned = listOf(
            MealDay(month.atDay(3), DailyMenu.Single(dish(id = "planned-3", type = DishType.SINGLE)), TestDishes.OLD_INSTANT),
            MealDay(month.atDay(15), DailyMenu.Courses(dish(id = "planned-15s", type = DishType.STARTER), dish(id = "planned-15m")), TestDishes.OLD_INSTANT)
        )

        val run = autoFill(richCatalog, seed, planned)

        assertEquals(29, run.result.filledDays)
        assertEquals(0, run.result.unfilledDays)
        assertEquals(29, run.repository.saved.size)
        assertTrue(run.repository.saved.none { day -> planned.any { it.date == day.date } })
        planned.forEach { assertEquals(it, run.repository.current[it.date]) }
    }

    @Test
    fun `saved days use the injected clock and slots match dish types`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed)

        run.repository.saved.forEach { day ->
            assertEquals(TestDishes.FIXED_INSTANT, day.updatedAt)
            assertSlotsMatchTypes(day.menu)
        }
    }

    @Test
    fun `both menu shapes are mixed when both are available`() = seeds.forEach { seed ->
        val menus = autoFill(richCatalog, seed).plan.values

        assertTrue("seed $seed: no Courses", menus.any { it is DailyMenu.Courses })
        assertTrue("seed $seed: no Single", menus.any { it is DailyMenu.Single })
    }

    @Test
    fun `only single shape is used when there are no starters`() = seeds.forEach { seed ->
        val dishes = richCatalog.filter { it.type != DishType.STARTER }

        val run = autoFill(dishes, seed)

        assertEquals(31, run.result.filledDays)
        assertTrue(run.plan.values.all { it is DailyMenu.Single })
    }

    @Test
    fun `only courses shape is used when there are no single dishes`() = seeds.forEach { seed ->
        val dishes = richCatalog.filter { it.type != DishType.SINGLE }

        val run = autoFill(dishes, seed)

        assertEquals(31, run.result.filledDays)
        assertTrue(run.plan.values.all { it is DailyMenu.Courses })
    }

    @Test
    fun `days stay empty when no shape can be built`() = seeds.forEach { seed ->
        val dishes = richCatalog.filter { it.type == DishType.DESSERT || it.type == DishType.MAIN }
        val planned = listOf(MealDay(month.atDay(10), DailyMenu.Single(dish(type = DishType.SINGLE)), TestDishes.OLD_INSTANT))

        val run = autoFill(dishes, seed, planned)

        assertEquals(AutoFillResult(filledDays = 0, repeatedDishDays = 0, heavyStreakDays = 0, unfilledDays = 30), run.result)
        assertTrue(run.repository.saved.isEmpty())
    }

    @Test
    fun `empty catalog leaves the whole month unfilled`() {
        val run = autoFill(emptyList(), seed = 1)

        assertEquals(AutoFillResult(0, 0, 0, 31), run.result)
    }

    @Test
    fun `dessert is added on some days but not all`() = seeds.forEach { seed ->
        val menus = autoFill(richCatalog, seed).plan.values

        assertTrue("seed $seed: no dessert", menus.any { it.dessert != null })
        assertTrue("seed $seed: always dessert", menus.any { it.dessert == null })
    }

    @Test
    fun `no dessert is ever added when there are no desserts`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog.filter { it.type != DishType.DESSERT }, seed)

        assertEquals(31, run.result.filledDays)
        assertTrue(run.plan.values.all { it.dessert == null })
    }

    @Test
    fun `no dish repeats within seven days`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed)

        assertNoRepeatWithinWeek(run.plan, seed)
        assertEquals(0, run.result.repeatedDishDays)
    }

    @Test
    fun `no dish repeats within seven days across the previous month boundary`() = seeds.forEach { seed ->
        val singles = richCatalog.filter { it.type == DishType.SINGLE && it.heaviness != Heaviness.VERY_HIGH }
        val desserts = richCatalog.filter { it.type == DishType.DESSERT }
        val previousMonth = month.minusMonths(1)
        val planned = (0 until 6).map { offset ->
            val date = previousMonth.atEndOfMonth().minusDays(offset.toLong())
            MealDay(date, DailyMenu.Single(singles[offset], desserts[offset]), TestDishes.OLD_INSTANT)
        }

        val run = autoFill(richCatalog, seed, planned)

        assertNoRepeatWithinWeek(run.plan, seed)
        assertEquals(0, run.result.repeatedDishDays)
        assertEquals(31, run.result.filledDays)
    }

    @Test
    fun `days older than six days of the previous month do not block dishes`() = seeds.forEach { seed ->
        val only = dish(id = "only-single", type = DishType.SINGLE)
        val planned = listOf(MealDay(month.minusMonths(1).atEndOfMonth().minusDays(6), DailyMenu.Single(only), TestDishes.OLD_INSTANT))

        val run = autoFill(listOf(only), seed, planned)

        assertEquals(only, (run.plan.getValue(month.atDay(1)) as DailyMenu.Single).single)
        assertEquals(30, run.result.repeatedDishDays)
    }

    @Test
    fun `filled days do not repeat dishes of planned days later in the window`() = seeds.forEach { seed ->
        val planned = listOf(MealDay(month.atDay(10), DailyMenu.Single(richCatalog.first { it.type == DishType.SINGLE }), TestDishes.OLD_INSTANT))

        val run = autoFill(richCatalog, seed, planned)

        assertNoRepeatWithinWeek(run.plan, seed)
    }

    @Test
    fun `filled day before a planned heavy day is not heavy`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed, listOf(heavyDay(month.atDay(12))))

        assertFalse("seed $seed", run.plan.getValue(month.atDay(11)).isHeavy())
    }

    @Test
    fun `no very heavy main dish follows a heavy day`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed)

        assertNoHeavyStreak(run.plan, seed)
        assertEquals(0, run.result.heavyStreakDays)
    }

    @Test
    fun `heavy days are actually planned so the heavy rule is exercised`() {
        val heavyDays = seeds.sumOf { seed -> autoFill(richCatalog, seed).plan.values.count { it.isHeavy() } }

        assertTrue(heavyDays > 0)
    }

    @Test
    fun `heavy rule holds after planned heavy days and across the month boundary`() = seeds.forEach { seed ->
        val planned = listOf(heavyDay(month.minusMonths(1).atEndOfMonth()), heavyDay(month.atDay(12)))

        val run = autoFill(richCatalog, seed, planned)

        assertFalse(run.plan.getValue(month.atDay(1)).isHeavy())
        assertFalse(run.plan.getValue(month.atDay(13)).isHeavy())
        assertNoHeavyStreak(run.plan, seed, monthDays() - month.atDay(12))
        assertEquals(0, run.result.heavyStreakDays)
    }

    @Test
    fun `heavy starter counts as a heavy day while heavy dessert does not`() = seeds.forEach { seed ->
        val heavyStarterDay = MealDay(
            month.atDay(5),
            DailyMenu.Courses(dish(id = "hs", type = DishType.STARTER, heaviness = Heaviness.VERY_HIGH), dish(id = "lm")),
            TestDishes.OLD_INSTANT
        )
        val heavyDessertDay = MealDay(
            month.atDay(20),
            DailyMenu.Single(dish(id = "ls", type = DishType.SINGLE), dish(id = "hd", type = DishType.DESSERT, heaviness = Heaviness.VERY_HIGH)),
            TestDishes.OLD_INSTANT
        )
        val heavySingle = dish(id = "heavy-single", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)
        val lightSingle = dish(id = "light-single", type = DishType.SINGLE)

        val run = autoFill(listOf(heavySingle, lightSingle), seed, listOf(heavyStarterDay, heavyDessertDay))

        assertFalse(run.plan.getValue(month.atDay(6)).isHeavy())
    }

    @Test
    fun `heavy dessert on the previous day does not force a light day`() {
        val heavySingle = dish(id = "heavy-single", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)
        val heavyDessertDay = MealDay(
            month.atDay(20),
            DailyMenu.Single(dish(id = "ls", type = DishType.SINGLE), dish(id = "hd", type = DishType.DESSERT, heaviness = Heaviness.VERY_HIGH)),
            TestDishes.OLD_INSTANT
        )

        val run = autoFill(listOf(heavySingle), seed = 1, planned = listOf(heavyDessertDay))

        assertTrue(run.plan.getValue(month.atDay(21)).isHeavy())
        assertEquals(31 - 1 - 1 - 1, run.result.heavyStreakDays)
    }

    @Test
    fun `scarce dishes are repeated and every day is still filled`() = seeds.forEach { seed ->
        val dishes = listOf(dish(id = "s1", type = DishType.SINGLE), dish(id = "s2", type = DishType.SINGLE), dish(id = "s3", type = DishType.SINGLE))

        val run = autoFill(dishes, seed)

        assertEquals(31, run.result.filledDays)
        assertEquals(0, run.result.unfilledDays)
        assertEquals(0, run.result.heavyStreakDays)
        assertEquals(countRepeatedDays(run.plan), run.result.repeatedDishDays)
        assertTrue(run.result.repeatedDishDays >= 4)
        val singles = monthDays().map { (run.plan.getValue(it) as DailyMenu.Single).single.id }
        assertEquals(setOf("s1", "s2", "s3"), singles.take(3).toSet())
    }

    @Test
    fun `repeated dish days count days with a repeated dish`() = seeds.forEach { seed ->
        val run = autoFill(listOf(dish(id = "only", type = DishType.SINGLE)), seed)

        assertEquals(AutoFillResult(filledDays = 31, repeatedDishDays = 30, heavyStreakDays = 0, unfilledDays = 0), run.result)
    }

    @Test
    fun `heavy rule is broken only when nothing else is possible`() = seeds.forEach { seed ->
        val run = autoFill(listOf(dish(id = "only-heavy", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)), seed)

        assertEquals(AutoFillResult(filledDays = 31, repeatedDishDays = 30, heavyStreakDays = 30, unfilledDays = 0), run.result)
    }

    @Test
    fun `a light dish is repeated before following a heavy day with another heavy dish`() = seeds.forEach { seed ->
        val dishes = listOf(
            dish(id = "light", type = DishType.SINGLE, heaviness = Heaviness.VERY_LOW),
            dish(id = "heavy-1", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH),
            dish(id = "heavy-2", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH),
            dish(id = "heavy-3", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)
        )

        val run = autoFill(dishes, seed)

        assertEquals(31, run.result.filledDays)
        assertEquals(0, run.result.heavyStreakDays)
        assertNoHeavyStreak(run.plan, seed)
        assertTrue(run.result.repeatedDishDays > 0)
    }

    @Test
    fun `repeating within the other shape is preferred over breaking the heavy rule`() = seeds.forEach { seed ->
        val starter = dish(id = "starter", type = DishType.STARTER)
        val main = dish(id = "main")
        val heavySingle = dish(id = "heavy-single", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)
        val lastDay = month.minusMonths(1).atEndOfMonth()
        val planned = listOf(MealDay(lastDay.minusDays(1), DailyMenu.Courses(starter, main), TestDishes.OLD_INSTANT), heavyDay(lastDay))

        val run = autoFill(listOf(starter, main, heavySingle), seed, planned)

        assertFalse("seed $seed: day 1 broke the heavy rule", run.plan.getValue(month.atDay(1)).isHeavy())
    }
}
