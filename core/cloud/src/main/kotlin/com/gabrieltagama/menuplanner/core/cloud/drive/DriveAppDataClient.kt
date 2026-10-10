package com.gabrieltagama.menuplanner.core.cloud.drive

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.time.DateTimeException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Minimal Google Drive REST v3 client restricted to the hidden app data folder, built on
 * HttpURLConnection so no extra HTTP library is needed. HTTP 401 surfaces as
 * DriveUnauthorizedException; any other HTTP, network or unexpected response error as IOException.
 */
internal class DriveAppDataClient @Inject constructor() {

    suspend fun accountEmail(token: String): String =
        parsed(send(token, GET, "$API/about?fields=${encoded("user(emailAddress)")}")) { root ->
            root.jsonObject.getValue("user").jsonObject.getValue("emailAddress").jsonPrimitive.content
        }

    suspend fun findBackups(token: String): List<DriveFile> =
        parsed(send(token, GET, "$API/files?spaces=$APP_DATA_FOLDER&q=${encoded("name = '$BACKUP_NAME'")}&fields=${encoded("files(id,modifiedTime)")}")) { root ->
            root.jsonObject["files"]?.jsonArray.orEmpty()
                .map { it.jsonObject }
                .map { DriveFile(it.getValue("id").jsonPrimitive.content, Instant.parse(it.getValue("modifiedTime").jsonPrimitive.content)) }
                .sortedByDescending(DriveFile::modifiedTime)
        }

    suspend fun download(token: String, fileId: String): String = send(token, GET, "$API/files/$fileId?alt=media")

    suspend fun create(token: String, content: String) {
        send(token, POST, "$UPLOAD_API/files?uploadType=multipart", multipartBody(content))
    }

    suspend fun delete(token: String, fileId: String) {
        send(token, DELETE, "$API/files/$fileId")
    }

    private fun multipartBody(content: String): RequestBody {
        val metadata = buildJsonObject {
            put("name", BACKUP_NAME)
            put("mimeType", JSON_MIME_TYPE)
            putJsonArray("parents") { add(APP_DATA_FOLDER) }
        }
        val text = listOf(
            "--$BOUNDARY",
            "Content-Type: $JSON_MIME_TYPE; charset=UTF-8",
            "",
            metadata.toString(),
            "--$BOUNDARY",
            "Content-Type: $JSON_MIME_TYPE; charset=UTF-8",
            "",
            content,
            "--$BOUNDARY--"
        ).joinToString(separator = CRLF)
        return RequestBody(contentType = "multipart/related; boundary=$BOUNDARY", bytes = text.toByteArray(Charsets.UTF_8))
    }

    private suspend fun send(token: String, method: String, url: String, body: RequestBody? = null): String = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) connection.write(body)
            connection.readResponse()
        } finally {
            connection.disconnect()
        }
    }

    private fun HttpURLConnection.write(body: RequestBody) {
        doOutput = true
        setRequestProperty("Content-Type", body.contentType)
        setFixedLengthStreamingMode(body.bytes.size)
        outputStream.use { it.write(body.bytes) }
    }

    private fun HttpURLConnection.readResponse(): String {
        val code = responseCode
        if (code == HttpURLConnection.HTTP_UNAUTHORIZED) throw DriveUnauthorizedException()
        if (code !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) throw IOException("Drive request failed with HTTP $code")
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun <T> parsed(text: String, extract: (JsonElement) -> T): T =
        try {
            extract(Json.parseToJsonElement(text))
        } catch (exception: IllegalArgumentException) {
            throw IOException("Unexpected Drive response", exception)
        } catch (exception: NoSuchElementException) {
            throw IOException("Unexpected Drive response", exception)
        } catch (exception: DateTimeException) {
            throw IOException("Unexpected Drive response", exception)
        }

    private fun encoded(value: String): String = URLEncoder.encode(value, UTF_8)

    private class RequestBody(val contentType: String, val bytes: ByteArray)

    private companion object {
        const val API = "https://www.googleapis.com/drive/v3"
        const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
        const val APP_DATA_FOLDER = "appDataFolder"
        const val BACKUP_NAME = "menuplanner-recipes-backup.json"
        const val JSON_MIME_TYPE = "application/json"
        const val BOUNDARY = "menuplanner-backup-boundary"
        const val CRLF = "\r\n"
        const val UTF_8 = "UTF-8"
        const val GET = "GET"
        const val POST = "POST"
        const val DELETE = "DELETE"
        const val TIMEOUT_MILLIS = 30_000
    }
}

internal data class DriveFile(val id: String, val modifiedTime: Instant)

internal class DriveUnauthorizedException : IOException("Drive access token rejected")
