package com.gabrieltagama.menuplanner.core.domain.fake

import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthDinnersUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.AutoFillMonthLunchesUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.DinnerPlanCommands
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.LunchPlanCommands
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PlanCommandService
import com.gabrieltagama.menuplanner.core.domain.usecase.mealplan.PlanHistoryReader
import java.time.Clock
import kotlin.random.Random

/**
 * Builds the calendar commands of both meals over the in-memory repositories, wired as Hilt does,
 * so tests of the domain and of the screens share one setup.
 */
class TestPlanCommands(
    val lunches: FakeLunchPlanRepository = FakeLunchPlanRepository(),
    val dinners: FakeDinnerPlanRepository = FakeDinnerPlanRepository(),
    val dishes: FakeDishRepository = FakeDishRepository(),
    val settings: FakeSettingsRepository = FakeSettingsRepository(),
    val transaction: ImmediateTransactionRunner = ImmediateTransactionRunner(),
    seed: Int = 1,
    clock: Clock = TestDishes.fixedClock
) {
    private val history = PlanHistoryReader(lunches, dinners)

    val lunch = LunchPlanCommands(lunches, AutoFillMonthLunchesUseCase(dishes, lunches, history, settings, Random(seed), clock), clock)

    val dinner = DinnerPlanCommands(dinners, AutoFillMonthDinnersUseCase(dishes, dinners, history, settings, Random(seed), clock), clock)

    val service = PlanCommandService(lunch, dinner, transaction)
}
