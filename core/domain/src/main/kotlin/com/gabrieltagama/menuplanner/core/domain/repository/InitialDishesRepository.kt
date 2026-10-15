package com.gabrieltagama.menuplanner.core.domain.repository

/**
 * Port that loads the starter recipe book bundled with the app. loadIfFirstLaunch runs it only
 * once per installation and only while the recipe book is empty, so users who already have
 * dishes never receive it. Implemented in :core:data.
 */
interface InitialDishesRepository {
    suspend fun loadIfFirstLaunch()
}
