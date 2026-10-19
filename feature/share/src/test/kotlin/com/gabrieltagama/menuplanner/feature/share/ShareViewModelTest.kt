package com.gabrieltagama.menuplanner.feature.share

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.fake.FakeDishRepository
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotCodec
import com.gabrieltagama.menuplanner.core.domain.fake.FakeMenuSnapshotRepository
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes
import com.gabrieltagama.menuplanner.core.domain.fake.TestDishes.dish
import com.gabrieltagama.menuplanner.core.domain.model.ImportSummary
import com.gabrieltagama.menuplanner.core.domain.model.share.MenuSnapshot
import com.gabrieltagama.menuplanner.core.domain.testing.MainDispatcherRule
import com.gabrieltagama.menuplanner.core.domain.usecase.dish.StandardizeRecipeBookUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ExportMenusUseCase
import com.gabrieltagama.menuplanner.core.domain.usecase.share.ImportMenusUseCase
import com.gabrieltagama.menuplanner.feature.share.files.ShareFiles
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests ShareViewModel with an in-memory ShareFiles: export writes the encoded snapshot and asks to
 * share the file, saving writes it into the chosen document, a write error reports ExportFailed, an import reports the merge summary and an
 * unreadable file reports InvalidImportFile.
 */
@RunWith(AndroidJUnit4::class)
class ShareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dishes = FakeDishRepository(listOf(dish(id = "a")))
    private val codec = FakeMenuSnapshotCodec()
    private val summary = ImportSummary(1, 0, 0, 2, 0, 0)
    private val snapshots = FakeMenuSnapshotRepository(dishes, mergeResult = summary)
    private val files = InMemoryShareFiles()

    @Test
    fun exportWritesTheFileAndAsksToShareIt() = runBlocking {
        val viewModel = viewModel()

        viewModel.onExport()

        assertEquals(ShareEvent.ShareFile(files.exportUri), withTimeout(TIMEOUT_MS) { viewModel.events.first() })
        assertEquals(listOf("dishes=1;lunches=0;dinners=0"), files.written)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun failedWriteReportsExportFailed() {
        files.failWrites = true
        val viewModel = viewModel()

        viewModel.onExport()

        assertEquals(ShareMessage.ExportFailed, viewModel.uiState.value.message)
    }

    @Test
    fun saveWritesTheExportIntoTheChosenDocument() {
        val viewModel = viewModel()

        viewModel.onSaveTo(files.saveUri)

        assertEquals(ShareMessage.Saved, viewModel.uiState.value.message)
        assertEquals("dishes=1;lunches=0;dinners=0", files.saved[files.saveUri])
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun failedSaveReportsExportFailed() {
        files.failSaves = true
        val viewModel = viewModel()

        viewModel.onSaveTo(files.saveUri)

        assertEquals(ShareMessage.ExportFailed, viewModel.uiState.value.message)
    }

    @Test
    fun savedCopyIsNamedAfterTheExportTime() =
        assertEquals("menuplanner-export.json", viewModel().saveFileName())

    @Test
    fun importReportsTheMergeSummary() {
        codec.register("payload", MenuSnapshot(emptyList()))
        files.contents[files.importUri] = "payload"
        val viewModel = viewModel()

        viewModel.onImport(files.importUri)

        assertEquals(ShareMessage.Imported(summary), viewModel.uiState.value.message)
    }

    @Test
    fun unreadableFileReportsInvalidImportFile() {
        val viewModel = viewModel()

        viewModel.onImport(files.importUri)

        assertEquals(ShareMessage.Failed(DomainError.InvalidImportFile), viewModel.uiState.value.message)
    }

    private fun viewModel() = ShareViewModel(
        exportMenus = ExportMenusUseCase(snapshots, codec, TestDishes.fixedClock),
        importMenus = ImportMenusUseCase(snapshots, codec, StandardizeRecipeBookUseCase(dishes)),
        files = files
    )

    private class InMemoryShareFiles : ShareFiles {
        val exportUri: Uri = Uri.parse("content://menuplanner/export.json")
        val importUri: Uri = Uri.parse("content://picked/menus.json")
        val saveUri: Uri = Uri.parse("content://downloads/menus.json")
        val saved = mutableMapOf<Uri, String>()
        var failSaves = false
        val written = mutableListOf<String>()
        val contents = mutableMapOf<Uri, String>()
        var failWrites = false

        override suspend fun write(content: String): Uri {
            if (failWrites) throw IOException("disk full")
            written += content
            return exportUri
        }

        override suspend fun save(uri: Uri, content: String): Boolean {
            if (failSaves) return false
            saved[uri] = content
            return true
        }

        override suspend fun read(uri: Uri): String? = contents[uri]

        override fun exportFileName(): String = "menuplanner-export.json"
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
