package com.gabrieltagama.menuplanner.core.domain.usecase.backup

import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeBackupScheduler
import com.gabrieltagama.menuplanner.core.domain.fake.FakeCloudBackupRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuShareRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.BackupResult
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests of the recipe backup use cases: upload only on changes, recipes-only payload, restore,
 * connection flow and scheduling.
 */
class BackupUseCasesTest {
    private val connected = CloudAccount.Connected(FakeCloudBackupRepository.EMAIL)
    private val dishes = FakeDishRepository(listOf(dish(id = "a")))
    private val share = FakeMenuShareRepository(recipesJson = RECIPES_JSON)
    private val scheduler = FakeBackupScheduler()

    private fun backUp(cloud: FakeCloudBackupRepository) = BackUpRecipesUseCase(cloud, share, dishes)

    private fun restore(cloud: FakeCloudBackupRepository) = RestoreRecipesUseCase(cloud, share)

    private fun connect(cloud: FakeCloudBackupRepository) =
        ConnectCloudAccountUseCase(cloud, restore(cloud), backUp(cloud), scheduler)

    @Test
    fun `backup does nothing while disconnected`() = runBlocking {
        val cloud = FakeCloudBackupRepository()

        assertEquals(Outcome.Success(BackupResult.NOT_CONNECTED), backUp(cloud)(onlyIfChanged = false))
        assertTrue(cloud.uploads.isEmpty())
    }

    @Test
    fun `backup uploads only the recipes json`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)

        assertEquals(Outcome.Success(BackupResult.UPLOADED), backUp(cloud)(onlyIfChanged = true))
        assertEquals(listOf(RECIPES_JSON), cloud.uploads)
        assertEquals(0, share.exportCalls)
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
        assertTrue(share.importedJson.isEmpty())
    }

    @Test
    fun `restore merges the remote backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected, remoteJson = "remote")
        restore(cloud)()

        assertEquals(listOf("remote"), share.importedJson)
    }

    @Test
    fun `connect restores, uploads and schedules the daily backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(remoteJson = "remote")

        assertEquals(Outcome.Success(ImportSummary.EMPTY), connect(cloud)("token"))
        assertEquals(connected, cloud.account.value)
        assertEquals(listOf("remote"), share.importedJson)
        assertEquals(listOf(RECIPES_JSON), cloud.uploads)
        assertTrue(scheduler.isScheduled)
    }

    @Test
    fun `failed connection does not schedule anything`() = runBlocking {
        val failure = Outcome.Failure(DomainError.CloudUnavailable)
        val cloud = FakeCloudBackupRepository(failure = failure)

        assertEquals(failure, connect(cloud)("token"))
        assertFalse(scheduler.isScheduled)
    }

    @Test
    fun `disconnect cancels the daily backup`() = runBlocking {
        val cloud = FakeCloudBackupRepository(connected)
        scheduler.scheduleDaily()
        DisconnectCloudAccountUseCase(cloud, scheduler)()

        assertEquals(CloudAccount.Disconnected, cloud.account.value)
        assertFalse(scheduler.isScheduled)
    }

    private companion object {
        const val RECIPES_JSON = """{"dishes":[]}"""
    }
}
