package com.gabrieltagama.menuplanner.feature.share.files

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Port for the files of the share flow: write puts an exported text in a file other apps can read
 * through the app FileProvider, save writes it into a document the user created with the system
 * picker (a copy that survives uninstalling the app; false when it cannot be written), read loads a
 * file picked by the user (null when it cannot be read) and exportFileName names exports after the
 * current time. CacheShareFiles keeps only the latest shared export in the cache.
 */
interface ShareFiles {
    suspend fun write(content: String): Uri
    suspend fun save(uri: Uri, content: String): Boolean
    suspend fun read(uri: Uri): String?
    fun exportFileName(): String
}

internal class CacheShareFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock
) : ShareFiles {

    override suspend fun write(content: String): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARED_DIRECTORY).apply { mkdirs() }
        directory.listFiles()?.forEach(File::delete)
        val file = File(directory, exportFileName()).apply { writeText(content) }
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    override suspend fun save(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, TRUNCATE_MODE)?.bufferedWriter()?.use { it.write(content) } != null
        } catch (expected: IOException) {
            false
        } catch (expected: SecurityException) {
            false
        }
    }

    override fun exportFileName(): String = "menuplanner-${LocalDateTime.now(clock).format(FILE_DATE_FORMAT)}.json"

    override suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (expected: IOException) {
            null
        } catch (expected: SecurityException) {
            null
        }
    }

    private companion object {
        const val SHARED_DIRECTORY = "shared"
        const val AUTHORITY_SUFFIX = ".fileprovider"
        const val TRUNCATE_MODE = "wt"
        val FILE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShareFilesModule {
    @Binds
    abstract fun bindShareFiles(implementation: CacheShareFiles): ShareFiles
}
