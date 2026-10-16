package com.gabrieltagama.menuplanner.core.data.di

import com.gabrieltagama.menuplanner.core.data.query.RoomCalendarQueries
import com.gabrieltagama.menuplanner.core.data.query.RoomShoppingListQueries
import com.gabrieltagama.menuplanner.core.domain.query.CalendarQueries
import com.gabrieltagama.menuplanner.core.domain.query.ShoppingListQueries
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the read ports (CQRS query side) to their Room implementations; write ports stay in
 * RepositoryModule.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class QueryModule {

    @Binds
    @Singleton
    abstract fun bindCalendarQueries(implementation: RoomCalendarQueries): CalendarQueries

    @Binds
    @Singleton
    abstract fun bindShoppingListQueries(implementation: RoomShoppingListQueries): ShoppingListQueries
}
