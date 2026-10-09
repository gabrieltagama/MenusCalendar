package com.gabrieltagama.menuplanner.core.domain.usecase.share

import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.repository.MenuShareRepository
import javax.inject.Inject

/**
 * Use cases to share the recipe book and calendar as JSON with other users.
 */
class ExportMenusUseCase @Inject constructor(private val repository: MenuShareRepository) {
    suspend operator fun invoke(): String = repository.exportAll()
}

class ImportMenusUseCase @Inject constructor(private val repository: MenuShareRepository) {
    suspend operator fun invoke(json: String): Outcome<ImportSummary> = repository.importAndMerge(json)
}
