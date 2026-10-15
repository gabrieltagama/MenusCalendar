package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.Dish
import com.gabrieltagama.menuplanner.core.domain.model.ImportContent
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.PlannedMeal
import com.gabrieltagama.menuplanner.core.domain.repository.DinnerPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.DishRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MealPlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.repository.PlanRepository
import com.gabrieltagama.menuplanner.core.domain.repository.ShareFileStore
import com.gabrieltagama.menuplanner.core.domain.repository.TransactionRunner
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use cases to share the recipe book and calendar as JSON. Export writes every dish and planned
 * day to a shareable file and returns its location (null when it could not be written); a file
 * that cannot be read imports as InvalidImportFile. Import is the single entry point for
 * received files, cloud restores and the starter recipe book: the file is validated first, then
 * dishes are merged with MergePolicy, then lunches and dinners (they may reference dishes from the
 * same file, and days breaking a calendar rule are skipped), and finally the recipe book is
 * converted to standard units, all inside one transaction so a failure leaves nothing half done.
 */
class ExportMenusUseCase @Inject constructor(
    private val repository: MenuShareRepository,
    private val files: ShareFileStore
) {
    suspend operator fun invoke(): String? = files.write(repository.exportAll())
}

class ImportMenusFileUseCase @Inject constructor(
    private val files: ShareFileStore,
    private val importMenus: ImportMenusUseCase
) {
    suspend operator fun invoke(location: String): Outcome<ImportSummary> =
        files.read(location)?.let { importMenus(it) } ?: Outcome.Failure(DomainError.InvalidImportFile)
}

class ImportMenusUseCase @Inject constructor(
    private val share: MenuShareRepository,
    private val dishes: DishRepository,
    private val lunches: MealPlanRepository,
    private val dinners: DinnerPlanRepository,
    private val transactions: TransactionRunner,
    private val standardizeRecipeBook: StandardizeRecipeBookUseCase
) {
    suspend operator fun invoke(json: String): Outcome<ImportSummary> = when (val decoded = share.decode(json)) {
        is Outcome.Failure -> decoded
        is Outcome.Success -> Outcome.Success(transactions.run { merge(decoded.value).also { standardizeRecipeBook() } })
    }

    private suspend fun merge(content: ImportContent): ImportSummary {
        val dishCount = mergeDishes(content.dishes)
        val known = dishes.getDishes().associateBy(Dish::id)
        val dayCount = mergePlan(lunches, content.lunches.map { it.resolve(known) }) +
            mergePlan(dinners, content.dinners.map { it.resolve(known) })
        return ImportSummary(
            dishesAdded = dishCount.added,
            dishesUpdated = dishCount.updated,
            dishesSkipped = dishCount.skipped,
            daysAdded = dayCount.added,
            daysUpdated = dayCount.updated,
            daysSkipped = dayCount.skipped
        )
    }

    private suspend fun mergeDishes(incoming: List<Dish>): MergeCount {
        val local = dishes.getDishes().associate { it.id to it.updatedAt }
        val decisions = incoming.map { it to MergePolicy.decide(local[it.id], it.updatedAt) }
        dishes.upsertAll(decisions.accepted())
        return MergeCount.of(decisions.map { it.second })
    }

    private suspend fun <T : PlannedMeal> mergePlan(repository: PlanRepository<T>, resolved: List<T?>): MergeCount {
        val valid = resolved.filterNotNull()
        val local = localVersions(repository, valid)
        val decisions = valid.map { it to MergePolicy.decide(local[it.date], it.updatedAt) }
        repository.saveAll(decisions.accepted())
        return MergeCount.of(decisions.map { it.second }, invalid = resolved.size - valid.size)
    }

    private suspend fun <T : PlannedMeal> localVersions(repository: PlanRepository<T>, days: List<T>): Map<LocalDate, Instant> =
        if (days.isEmpty()) emptyMap()
        else repository.getRange(days.minOf(PlannedMeal::date)..days.maxOf(PlannedMeal::date)).associate { it.date to it.updatedAt }

    private fun <T> List<Pair<T, MergeDecision>>.accepted(): List<T> = filter { it.second != MergeDecision.SKIP }.map { it.first }
}
