package com.gabrieltagama.menuplanner.core.data.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.gabrieltagama.menuplanner.core.domain.common.IoDispatcher
import com.gabrieltagama.menuplanner.core.domain.port.ShareFileStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Share files kept in the app cache and exposed through the app FileProvider (authority
 * applicationId + ".fileprovider", declared in the app manifest). Exports are named after the
 * export time from the injected clock; picked files are read through the ContentResolver. I/O
 * errors and revoked permissions surface as null.
 */
internal class CacheShareFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ShareFileStore {

    override suspend fun write(json: String): String? = withContext(ioDispatcher) {
        try {
            val directory = File(context.cacheDir, SHARED_DIRECTORY).apply { mkdirs() }
            val file = File(directory, "menuplanner-${LocalDateTime.now(clock).format(FILE_DATE_FORMAT)}.json").apply { writeText(json) }
            FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file).toString()
        } catch (exception: IOException) {
            null
        }
    }

    override suspend fun read(location: String): String? = withContext(ioDispatcher) {
        try {
            context.contentResolver.openInputStream(Uri.parse(location))?.bufferedReader()?.use { it.readText() }
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
