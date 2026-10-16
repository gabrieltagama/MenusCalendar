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
 * through the app FileProvider, and read loads a file picked by the user (null when it cannot be
 * read). CacheShareFiles keeps only the latest export in the cache, named after the export time.
 */
interface ShareFiles {
    suspend fun write(content: String): Uri
    suspend fun read(uri: Uri): String?
}

internal class CacheShareFiles @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock
) : ShareFiles {

    override suspend fun write(content: String): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, SHARED_DIRECTORY).apply { mkdirs() }
        directory.listFiles()?.forEach(File::delete)
        val file = File(directory, "menuplanner-${LocalDateTime.now(clock).format(FILE_DATE_FORMAT)}.json").apply { writeText(content) }
        FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    override suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (exception: IOException) {
            null
        } catch (exception: SecurityException) {
            null
        }
    }

    private companion object {
        const val SHARED_DIRECTORY = "shared"
        const val AUTHORITY_SUFFIX = ".fileprovider"
        val FILE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShareFilesModule {
    @Binds
    abstract fun bindShareFiles(implementation: CacheShareFiles): ShareFiles
}
