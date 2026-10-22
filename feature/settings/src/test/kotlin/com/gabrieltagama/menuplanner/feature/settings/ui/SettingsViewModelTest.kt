package com.gabrieltagama.menuplanner.feature.settings.ui

import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeBackupScheduler
import com.gabrieltagama.menuplanner.core.domain.fake.FakeCloudBackupRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestImports
import com.gabrieltagama.menuplanner.core.domain.model.CloudAccount
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.RepeatWindow
import com.gabrieltagama.menuplanner.core.domain.model.ThemeMode
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.BackUpUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ConnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.DisconnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.RestoreBackupUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.SetRepeatWindowUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.SetThemeModeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests SettingsViewModel over the real use cases and in-memory repositories: theme and repeat
 * window are saved, linking Google restores and schedules the daily backup, a cancelled link and a
 * failure are reported, backing up and unlinking report their result, and the busy flag is always
 * released.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val dishes = FakeDishRepository(listOf(TestDishes.dish(id = "a")))
    private val snapshots = FakeMenuSnapshotRepository(dishes)
    private val codec = FakeMenuSnapshotCodec()
    private val scheduler = FakeBackupScheduler()

    @Test
    fun themeAndRepeatWindowAreSaved() = runBlocking {
        val viewModel = viewModel(FakeCloudBackupRepository())

        viewModel.onThemeModeSelect(ThemeMode.DARK)
        viewModel.onRepeatWindowChange(3)

        assertEquals(ThemeMode.DARK, withTimeout(TIMEOUT_MS) { viewModel.themeMode.first { it == ThemeMode.DARK } })
        assertEquals(RepeatWindow.of(3), withTimeout(TIMEOUT_MS) { viewModel.repeatWindow.first { it == RepeatWindow.of(3) } })
    }

    @Test
    fun linkingGoogleRestoresSchedulesAndConnects() = runBlocking {
        val cloud = FakeCloudBackupRepository()
        val viewModel = viewModel(cloud)

        viewModel.onConnectStarted()
        assertTrue(viewModel.backupState.value.isBusy)
        viewModel.onGoogleAuthorization("token")

        assertEquals(BackupUiState(message = BackupMessage.Restored(ImportSummary.EMPTY)), viewModel.backupState.value)
        assertTrue(scheduler.isScheduled)
        assertEquals(CloudAccount.Connected(FakeCloudBackupRepository.EMAIL), withTimeout(TIMEOUT_MS) { viewModel.cloudAccount.first { it is CloudAccount.Connected } })
    }

    @Test
    fun cancelledLinkIsReportedAndReleasesTheBusyFlag() {
        val viewModel = viewModel(FakeCloudBackupRepository())

        viewModel.onConnectStarted()
        viewModel.onGoogleAuthorization(null)

        assertEquals(BackupUiState(message = BackupMessage.Failed(CloudError.AuthorizationCancelled)), viewModel.backupState.value)
    }

    @Test
    fun backingUpUploadsAndReportsIt() {
        val cloud = FakeCloudBackupRepository(CloudAccount.Connected(FakeCloudBackupRepository.EMAIL))
        val viewModel = viewModel(cloud)

        viewModel.onBackUpNow()

        assertEquals(BackupUiState(message = BackupMessage.BackedUp), viewModel.backupState.value)
        assertEquals(1, cloud.uploads.size)
    }

    @Test
    fun failedOperationIsReportedAndReleasesTheBusyFlag() {
        val failure = Outcome.Failure(CloudError.CloudUnavailable)
        val cloud = FakeCloudBackupRepository(CloudAccount.Connected(FakeCloudBackupRepository.EMAIL), failure = failure)
        val viewModel = viewModel(cloud)

        viewModel.onRestore()

        assertEquals(BackupUiState(message = BackupMessage.Failed(CloudError.CloudUnavailable)), viewModel.backupState.value)
    }

    @Test
    fun unlinkingCancelsTheDailyBackupAndReportsIt() = runBlocking {
        val cloud = FakeCloudBackupRepository()
        val viewModel = viewModel(cloud)
        viewModel.onGoogleAuthorization("token")

        viewModel.onDisconnect()

        assertEquals(BackupUiState(message = BackupMessage.Disconnected), viewModel.backupState.value)
        assertFalse(scheduler.isScheduled)
        assertEquals(CloudAccount.Disconnected, withTimeout(TIMEOUT_MS) { viewModel.cloudAccount.first { it == CloudAccount.Disconnected } })
    }

    @Test
    fun shownMessageIsCleared() {
        val viewModel = viewModel(FakeCloudBackupRepository())
        viewModel.onGoogleAuthorization(null)

        viewModel.onMessageShown()

        assertEquals(BackupUiState(), viewModel.backupState.value)
    }

    private fun viewModel(cloud: FakeCloudBackupRepository): SettingsViewModel {
        val backUp = BackUpUseCase(cloud, snapshots, codec, TestDishes.fixedClock)
        val restore = RestoreBackupUseCase(cloud, TestImports.importMenus(dishes, codec))
        return SettingsViewModel(
            preferences = settings,
            cloud = cloud,
            setThemeMode = SetThemeModeUseCase(settings),
            setRepeatWindow = SetRepeatWindowUseCase(settings),
            connectCloudAccount = ConnectCloudAccountUseCase(cloud, restore, backUp, scheduler),
            backUp = backUp,
            restoreBackup = restore,
            disconnectCloudAccount = DisconnectCloudAccountUseCase(cloud, scheduler)
        )
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
