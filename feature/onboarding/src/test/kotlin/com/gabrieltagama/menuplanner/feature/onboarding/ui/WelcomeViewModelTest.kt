package com.gabrieltagama.menuplanner.feature.onboarding.ui

import com.gabrieltagama.menuplanner.core.domain.common.CloudError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import com.gabrieltagama.menuplanner.core.domain.fake.FakeBackupScheduler
import com.gabrieltagama.menuplanner.core.domain.fake.FakeCloudBackupRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeSettingsRepository
import com.gabrieltagama.menuplanner.core.domain.fake.ImmediateTransactionRunner
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.BackUpUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.ConnectCloudAccountUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.backup.RestoreBackupUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.settings.CompleteOnboardingUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests WelcomeViewModel over the real use cases and in-memory repositories: continuing without an
 * account and linking Google complete the onboarding, while a cancelled or failed link reports it,
 * stops the progress and leaves the onboarding pending so the user can try again.
 */
class WelcomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val dishes = FakeDishRepository()
    private val snapshots = FakeMenuSnapshotRepository(dishes)
    private val codec = FakeMenuSnapshotCodec()
    private val scheduler = FakeBackupScheduler()

    @Test
    fun continuingWithoutAccountCompletesTheOnboarding() = runBlocking {
        viewModel(FakeCloudBackupRepository()).onContinueWithoutAccount()

        assertTrue(settings.isOnboardingCompleted.first())
        assertFalse(scheduler.isScheduled)
    }

    @Test
    fun linkingGoogleRestoresAndCompletesTheOnboarding() = runBlocking {
        val viewModel = viewModel(FakeCloudBackupRepository())

        viewModel.onConnectStarted()
        assertTrue(viewModel.uiState.value.isConnecting)
        viewModel.onGoogleAuthorization("token")

        assertTrue(settings.isOnboardingCompleted.first())
        assertTrue(scheduler.isScheduled)
    }

    @Test
    fun cancelledLinkIsReportedAndTheOnboardingStaysPending() = runBlocking {
        val viewModel = viewModel(FakeCloudBackupRepository())

        viewModel.onConnectStarted()
        viewModel.onGoogleAuthorization(null)

        assertEquals(WelcomeUiState(message = WelcomeMessage.Failed(CloudError.AuthorizationCancelled)), viewModel.uiState.value)
        assertFalse(settings.isOnboardingCompleted.first())
    }

    @Test
    fun failedLinkIsReportedAndTheOnboardingStaysPending() = runBlocking {
        val viewModel = viewModel(FakeCloudBackupRepository(failure = Outcome.Failure(CloudError.CloudUnavailable)))

        viewModel.onConnectStarted()
        viewModel.onGoogleAuthorization("token")

        assertEquals(WelcomeUiState(message = WelcomeMessage.Failed(CloudError.CloudUnavailable)), viewModel.uiState.value)
        assertFalse(settings.isOnboardingCompleted.first())
    }

    private fun viewModel(cloud: FakeCloudBackupRepository): WelcomeViewModel {
        val backUp = BackUpUseCase(cloud, snapshots, codec, TestDishes.fixedClock)
        val restore = RestoreBackupUseCase(cloud, ImportMenusUseCase(snapshots, codec, StandardizeRecipeBookUseCase(dishes), ImmediateTransactionRunner()))
        return WelcomeViewModel(
            connectCloudAccount = ConnectCloudAccountUseCase(cloud, restore, backUp, scheduler),
            completeOnboarding = CompleteOnboardingUseCase(settings)
        )
    }
}
