package com.gabrieltagama.menuplanner.core.domain.usecase.backup

import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.common.ShareError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeBackupScheduler
import com.gabrieltagama.menuplanner.core.domain.fake.FakeCloudBackupRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.fake.ImmediateTransactionRunner
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.BackupResult
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.DishId
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.DinnerEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.LunchEntry
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.model.share.SnapshotScope
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests of the backup use cases: upload only on changes, recipes-only payload, restore without the
 * calendar of older backups, connection flow and scheduling.
 */
class BackupUseCasesTest {
    private val connected = CloudAccount.Connected(FakeCloudBackupRepository.EMAIL)
    private val dishes = FakeDishRepository(listOf(dish(id = "a")))
    private val date = LocalDate.of(2026, 3, 10)
    private val calendar = MenuSnapshot(
        dishes = emptyList(),
        lunches = listOf(LunchEntry(date, singleId = DishId("a"), updatedAt = TestDishes.OLD_INSTANT)),
        dinners = listOf(DinnerEntry(date, DishId("a"), TestDishes.OLD_INSTANT))
    )
    private val snapshots = FakeMenuSnapshotRepository(dishes, calendar)
    private val codec = FakeMenuSnapshotCodec()
    private val scheduler = FakeBackupScheduler()
    private val recipesText = "dishes=1;lunches=0;dinners=0"
    private val remoteSnapshot = MenuSnapshot(listOf(dish(id = "remote")))

    private fun backUp(cloud: FakeCloudBackupRepository) = BackUpUseCase(cloud, snapshots, codec, TestDishes.fixedClock)

    private fun restore(cloud: FakeCloudBackupRepository) =
        RestoreBackupUseCase(cloud, ImportMenusUseCase(snapshots, codec, StandardizeRecipeBookUseCase(dishes), ImmediateTransactionRunner()))

    private fun connect(cloud: FakeCloudBackupRepository) =
        ConnectCloudAccountUseCase(cloud, restore(cloud), backUp(cloud), scheduler)

    @Test
    fun `backup does nothing while disconnected`() = runBlocking {
        val cloud = FakeCloudBackupRepository()

        assertEquals(Outcome.Success(BackupResult.NOT_CONNECTED), backUp(cloud)(onlyIfChanged = false))
        assertTrue(cloud.uploads.isEmpty())
    }

    @Test
    fun `backup uploads only the recipes, never the calendar`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)

        assertEquals(Outcome.Success(BackupResult.UPLOADED), backUp(cloud)(onlyIfChanged = true))
        assertEquals(listOf(recipesText), cloud.uploads)
        assertEquals(listOf(SnapshotScope.RECIPES), snapshots.readScopes)
    }

    @Test
    fun `automatic backup skips when recipes did not change`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)
        backUp(cloud)(onlyIfChanged = true)

        assertEquals(Outcome.Success(BackupResult.NO_CHANGES), backUp(cloud)(onlyIfChanged = true))
        assertEquals(1, cloud.uploads.size)
    }

    @Test
    fun `automatic backup uploads again after a recipe changes`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)
        backUp(cloud)(onlyIfChanged = true)
        dishes.upsert(dish(id = "a", updatedAt = TestDishes.FIXED_INSTANT))

        assertEquals(Outcome.Success(BackupResult.UPLOADED), backUp(cloud)(onlyIfChanged = true))
        assertEquals(2, cloud.uploads.size)
    }

    @Test
    fun `manual backup uploads even without changes`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)
        backUp(cloud)(onlyIfChanged = true)

        assertEquals(Outcome.Success(BackupResult.UPLOADED), backUp(cloud)(onlyIfChanged = false))
        assertEquals(2, cloud.uploads.size)
    }

    @Test
    fun `restore without remote backup returns an empty summary`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)

        assertEquals(Outcome.Success(ImportSummary.EMPTY), restore(cloud)())
        assertTrue(snapshots.merged.isEmpty())
    }

    @Test
    fun `restore merges the remote backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected, remoteJson = "remote")
        codec.register("remote", remoteSnapshot)
        restore(cloud)()

        assertEquals(listOf(remoteSnapshot), snapshots.merged)
    }

    @Test
    fun `restore ignores the calendar of an older backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected, remoteJson = "old")
        codec.register("old", remoteSnapshot.copy(lunches = calendar.lunches, dinners = calendar.dinners))
        restore(cloud)()

        assertEquals(listOf(remoteSnapshot), snapshots.merged)
    }

    @Test
    fun `connect restores, uploads and schedules the daily backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(remoteJson = "remote")
        codec.register("remote", remoteSnapshot)

        assertEquals(Outcome.Success(ImportSummary.EMPTY), connect(cloud)("token"))
        assertEquals(connected, cloud.account.first())
        assertEquals(listOf(remoteSnapshot), snapshots.merged)
        assertEquals(listOf(recipesText), cloud.uploads)
        assertTrue(scheduler.isScheduled)
    }

    @Test
    fun `failed connection does not schedule anything`() = runBlocking {
        val failure = Outcome.Failure(CloudError.CloudUnavailable)
        val cloud = FakeCloudBackupRepository(failure = failure)

        assertEquals(failure, connect(cloud)("token"))
        assertFalse(scheduler.isScheduled)
    }

    @Test
    fun `failed restore does not schedule the daily backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(remoteJson = "not a backup")

        assertEquals(Outcome.Failure(ShareError.InvalidImportFile), connect(cloud)("token"))
        assertTrue(cloud.uploads.isEmpty())
        assertFalse(scheduler.isScheduled)
    }

    @Test
    fun `disconnect cancels the daily backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)
        scheduler.scheduleDaily()
        DisconnectCloudAccountUseCase(cloud, scheduler)()

        assertEquals(CloudAccount.Disconnected, cloud.account.first())
        assertFalse(scheduler.isScheduled)
    }
}
