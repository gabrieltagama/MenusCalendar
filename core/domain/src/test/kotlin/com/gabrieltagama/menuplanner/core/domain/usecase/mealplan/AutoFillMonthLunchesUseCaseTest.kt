package com.gabrieltagama.menuplanner.core.domain.usecase.mealplan

import com.gabrieltagama.menuplanner.core.domain.fake.FakeDinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeLunchPlanRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.AutoFillResult
import com.gabrieltagama.menuplanner.core.domain.model.DailyMenu
import com.gabrieltagama.menuplanner.core.domain.model.DinnerDay
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.DishType
import com.gabrieltagama.menuplanner.core.domain.model.Heaviness
import com.gabrieltagama.menuplanner.core.domain.model.LunchDay
import com.gabrieltagama.menuplanner.core.domain.model.MealTime
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Property-style tests of AutoFillMonthLunchesUseCase (and MenuPicker) over many seeds: only empty days are
 * filled, menu shapes are mixed, desserts are never planned, no dish repeats within 7 days (also across both month
 * boundaries), dinner-only dishes are never used, no VERY_HIGH main dish follows a heavy day, and the fallback order when dishes are scarce
 * is "repeat first, break the heavy rule last". Lunches also avoid the dinner of the same day and the
 * dinners within the RepeatWindow, whose size comes from the settings, and the current month is filled
 * from today on.
 */
class AutoFillMonthLunchesUseCaseTest {
    private val month = YearMonth.of(2026, 3)
    private val seeds = 1..50

    private class Run(val result: AutoFillResult, val repository: FakeLunchPlanRepository) {
        val plan: Map<LocalDate, DailyMenu> get() = repository.current.mapValues { it.value.menu }
    }

    private fun autoFill(
        dishes: List<Dish>,
        seed: Int,
        planned: List<LunchDay> = emptyList(),
        dinners: List<DinnerDay> = emptyList(),
        window: RepeatWindow = RepeatWindow.DEFAULT,
        clock: Clock = TestDishes.fixedClock
    ): Run = runBlocking {
        val repository = FakeLunchPlanRepository(planned)
        Run(useCase(dishes, repository, seed, dinners, window, clock)(month), repository)
    }

    private fun useCase(
        dishes: List<Dish>,
        repository: FakeLunchPlanRepository,
        seed: Int,
        dinners: List<DinnerDay> = emptyList(),
        window: RepeatWindow = RepeatWindow.DEFAULT,
        clock: Clock = TestDishes.fixedClock
    ) = AutoFillMonthLunchesUseCase(
        FakeDishRepository(dishes),
        repository,
        FakeDinnerPlanRepository(dinners),
        FakeSettingsRepository(initialRepeatWindow = window),
        Random(seed),
        clock
    )

    private fun dishesOf(type: DishType, light: Int, heavy: Int): List<Dish> =
        (1..light).map { dish(id = "$type-light-$it", type = type, heaviness = if (it % 2 == 0) Heaviness.MEDIUM else Heaviness.VERY_LOW) } +
            (1..heavy).map { dish(id = "$type-heavy-$it", type = type, heaviness = Heaviness.VERY_HIGH) }

    private val richCatalog: List<Dish> = DishType.entries.flatMap { dishesOf(it, light = 8, heavy = 3) }

    private fun DailyMenu.isHeavy(): Boolean = dishes.any { it.heaviness == Heaviness.VERY_HIGH }

    private fun monthDays(): List<LocalDate> = (1..month.lengthOfMonth()).map(month::atDay)

    private fun assertNoRepeatWithinWeek(plan: Map<LocalDate, DailyMenu>, seed: Int) {
        val dates = plan.keys.sorted()
        for (first in dates) {
            for (second in dates) {
                val gap = ChronoUnit.DAYS.between(first, second)
                if (gap !in 1..6) continue
                val shared = plan.getValue(first).dishes.map(Dish::id).intersect(plan.getValue(second).dishes.map(Dish::id).toSet())
                if (shared.isNotEmpty()) fail("seed $seed: $first and $second share $shared")
            }
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
    }

    private fun heavyDay(date: LocalDate) =
        LunchDay(date, DailyMenu.Single(dish(id = "planned-heavy-$date", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)), TestDishes.OLD_INSTANT)

    @Test
    fun `fills every day of an empty month and reports no fallbacks with a rich catalog`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed)

        assertEquals(AutoFillResult(filledDays = 31, repeatedDishDays = 0, heavyStreakDays = 0, unfilledDays = 0), run.result)
        assertEquals(monthDays().toSet(), run.plan.keys)
    }

    @Test
    fun `only empty days are filled and planned days are untouched`() = seeds.forEach { seed ->
        val planned = listOf(
            LunchDay(month.atDay(3), DailyMenu.Single(dish(id = "planned-3", type = DishType.SINGLE)), TestDishes.OLD_INSTANT),
            LunchDay(month.atDay(15), DailyMenu.Courses(dish(id = "planned-15s", type = DishType.STARTER), dish(id = "planned-15m")), TestDishes.OLD_INSTANT)
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
        val planned = listOf(LunchDay(month.atDay(10), DailyMenu.Single(dish(type = DishType.SINGLE)), TestDishes.OLD_INSTANT))

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
    fun `desserts are never planned`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed)

        assertEquals(31, run.result.filledDays)
        assertTrue("seed $seed", run.plan.values.flatMap(DailyMenu::dishes).none { it.type == DishType.DESSERT })
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
        val previousMonth = month.minusMonths(1)
        val planned = (0 until 6).map { offset ->
            val date = previousMonth.atEndOfMonth().minusDays(offset.toLong())
            LunchDay(date, DailyMenu.Single(singles[offset]), TestDishes.OLD_INSTANT)
        }

        val run = autoFill(richCatalog, seed, planned)

        assertNoRepeatWithinWeek(run.plan, seed)
        assertEquals(0, run.result.repeatedDishDays)
        assertEquals(31, run.result.filledDays)
    }

    @Test
    fun `no dish repeats within seven days across the next month boundary`() = seeds.forEach { seed ->
        val singles = richCatalog.filter { it.type == DishType.SINGLE && it.heaviness != Heaviness.VERY_HIGH }
        val nextMonth = month.plusMonths(1)
        val planned = (0 until 6).map { offset ->
            LunchDay(nextMonth.atDay(1).plusDays(offset.toLong()), DailyMenu.Single(singles[offset]), TestDishes.OLD_INSTANT)
        }

        val run = autoFill(richCatalog, seed, planned)

        assertNoRepeatWithinWeek(run.plan, seed)
        assertEquals(0, run.result.repeatedDishDays)
        assertEquals(31, run.result.filledDays)
    }

    @Test
    fun `last day of the month before a planned heavy day of the next month is not heavy`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed, listOf(heavyDay(month.plusMonths(1).atDay(1))))

        assertFalse("seed $seed", run.plan.getValue(month.atEndOfMonth()).isHeavy())
    }

    @Test
    fun `days older than six days of the previous month do not block dishes`() = seeds.forEach { seed ->
        val only = dish(id = "only-single", type = DishType.SINGLE)
        val planned = listOf(LunchDay(month.minusMonths(1).atEndOfMonth().minusDays(6), DailyMenu.Single(only), TestDishes.OLD_INSTANT))

        val run = autoFill(listOf(only), seed, planned)

        assertEquals(only, (run.plan.getValue(month.atDay(1)) as DailyMenu.Single).single)
        assertEquals(30, run.result.repeatedDishDays)
    }

    @Test
    fun `filled days do not repeat dishes of planned days later in the window`() = seeds.forEach { seed ->
        val planned = listOf(LunchDay(month.atDay(10), DailyMenu.Single(richCatalog.first { it.type == DishType.SINGLE }), TestDishes.OLD_INSTANT))

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
    fun `heavy starter counts as a heavy day`() = seeds.forEach { seed ->
        val heavyStarterDay = LunchDay(
            month.atDay(5),
            DailyMenu.Courses(dish(id = "hs", type = DishType.STARTER, heaviness = Heaviness.VERY_HIGH), dish(id = "lm")),
            TestDishes.OLD_INSTANT
        )
        val heavySingle = dish(id = "heavy-single", type = DishType.SINGLE, heaviness = Heaviness.VERY_HIGH)
        val lightSingle = dish(id = "light-single", type = DishType.SINGLE)

        val run = autoFill(listOf(heavySingle, lightSingle), seed, listOf(heavyStarterDay))

        assertFalse(run.plan.getValue(month.atDay(6)).isHeavy())
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
        val singles = monthDays().map { (run.plan.getValue(it) as DailyMenu.Single).single.id.value }
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
        val planned = listOf(LunchDay(lastDay.minusDays(1), DailyMenu.Courses(starter, main), TestDishes.OLD_INSTANT), heavyDay(lastDay))

        val run = autoFill(listOf(starter, main, heavySingle), seed, planned)

        assertFalse("seed $seed: day 1 broke the heavy rule", run.plan.getValue(month.atDay(1)).isHeavy())
    }

    @Test
    fun `dinner only dishes are never planned`() = seeds.forEach { seed ->
        val dinnerOnly = DishType.entries.flatMap { type -> (1..5).map { dish(id = "$type-dinner-$it", type = type, mealTime = MealTime.DINNER) } }
        val lunchCatalog = richCatalog.map { it.copy(mealTime = if (it.id.hashCode() % 2 == 0) MealTime.LUNCH else MealTime.ANY) }

        val run = autoFill(lunchCatalog + dinnerOnly, seed)

        assertEquals(31, run.result.filledDays)
        assertTrue("seed $seed: a dinner-only dish was planned", run.plan.values.flatMap(DailyMenu::dishes).none { it.mealTime == MealTime.DINNER })
    }

    @Test
    fun `days stay unfilled when every dish is dinner only`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog.map { it.copy(mealTime = MealTime.DINNER) }, seed)

        assertEquals(AutoFillResult(filledDays = 0, repeatedDishDays = 0, heavyStreakDays = 0, unfilledDays = 31), run.result)
    }

    @Test
    fun `filling given dates fills only those that are empty`() = seeds.forEach { seed ->
        val planned = LunchDay(date = month.atDay(3), menu = DailyMenu.Single(single = dish(id = "kept", type = DishType.SINGLE)), updatedAt = Instant.EPOCH)
        val repository = FakeLunchPlanRepository(listOf(planned))
        val useCase = useCase(richCatalog, repository, seed)

        val result = runBlocking { useCase(setOf(month.atDay(2), month.atDay(3), month.atDay(4))) }

        assertEquals("seed $seed", 2, result.filledDays)
        assertEquals("seed $seed", setOf(month.atDay(2), month.atDay(3), month.atDay(4)), repository.current.keys)
        assertEquals("seed $seed", planned, repository.current[month.atDay(3)])
    }

    @Test
    fun `a lunch never uses the dinner of the same day`() = seeds.forEach { seed ->
        val singles = (1..3).map { dish(id = "single-$it", type = DishType.SINGLE) }
        val dinners = monthDays().map { DinnerDay(date = it, dish = singles[it.dayOfMonth % singles.size], updatedAt = Instant.EPOCH) }

        val run = autoFill(singles, seed, dinners = dinners)

        assertEquals(31, run.result.filledDays)
        dinners.forEach { dinner -> assertFalse("seed $seed: ${dinner.date}", dinner.dish in run.plan.getValue(dinner.date).dishes) }
    }

    @Test
    fun `a lunch avoids the dinners within the repeat window`() = seeds.forEach { seed ->
        val singles = richCatalog.filter { it.type == DishType.SINGLE && it.heaviness != Heaviness.VERY_HIGH }
        val dinnerDate = month.atDay(15)
        val dinner = DinnerDay(date = dinnerDate, dish = singles.first(), updatedAt = Instant.EPOCH)

        val run = autoFill(singles, seed, dinners = listOf(dinner))

        val nearby = (-6L..6L).map(dinnerDate::plusDays)
        assertTrue("seed $seed", nearby.none { dinner.dish in run.plan.getValue(it).dishes })
    }

    @Test
    fun `the repeat window comes from the settings`() = seeds.forEach { seed ->
        val run = autoFill(richCatalog, seed, window = RepeatWindow.of(2))

        assertEquals(0, run.result.repeatedDishDays)
        for (date in monthDays()) {
            val ids = run.plan.getValue(date).dishes.map(Dish::id)
            (1..2L).forEach { back -> assertTrue("seed $seed: $date", run.plan[date.minusDays(back)]?.dishes.orEmpty().none { it.id in ids }) }
        }
    }

    @Test
    fun `a zero day window allows repeating every day without reporting it`() {
        val single = dish(id = "only-single", type = DishType.SINGLE)

        val run = autoFill(listOf(single), seed = 1, window = RepeatWindow.of(0))

        assertEquals(AutoFillResult(filledDays = 31, repeatedDishDays = 0, heavyStreakDays = 0, unfilledDays = 0), run.result)
    }

    @Test
    fun `the current month is filled from today on`() {
        val today = month.atDay(20)
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

        val run = autoFill(richCatalog, seed = 1, clock = clock)

        assertEquals(12, run.result.filledDays)
        assertTrue(run.plan.keys.none { it.isBefore(today) })
        assertTrue(today in run.plan.keys)
    }

    @Test
    fun `given dates in the past are still filled`() {
        val today = month.atDay(20)
        val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        val repository = FakeLunchPlanRepository()

        val result = runBlocking { useCase(richCatalog, repository, seed = 1, clock = clock)(setOf(month.atDay(2))) }

        assertEquals(1, result.filledDays)
    }
}
