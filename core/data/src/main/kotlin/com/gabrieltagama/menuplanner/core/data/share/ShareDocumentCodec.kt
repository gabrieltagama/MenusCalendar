package com.gabrieltagama.menuplanner.core.data.share

import com.gabrieltagama.menuplanner.core.data.share.dto.ShareDocumentDto
import com.gabrieltagama.menuplanner.core.domain.common.DomainError
import com.gabrieltagama.menuplanner.core.domain.common.Outcome
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Encodes and decodes the share document. The schema version is read before full decoding so a
 * newer file reports UnsupportedImportVersion instead of a parse error. SerializationException
 * extends IllegalArgumentException, so a single catch covers malformed JSON and wrong shapes.
 */
internal class ShareDocumentCodec @Inject constructor(private val json: Json) {

    fun encode(document: ShareDocumentDto): String = json.encodeToString(ShareDocumentDto.serializer(), document)

    fun decode(text: String): Outcome<ShareDocumentDto> =
        try {
            if (schemaVersionOf(text) > ShareDocumentDto.CURRENT_SCHEMA_VERSION) Outcome.Failure(DomainError.UnsupportedImportVersion)
            else Outcome.Success(json.decodeFromString(ShareDocumentDto.serializer(), text))
        } catch (exception: IllegalArgumentException) {
            Outcome.Failure(DomainError.InvalidImportFile)
        }

    private fun schemaVersionOf(text: String): Int =
        json.parseToJsonElement(text).jsonObject[SCHEMA_VERSION_KEY]?.jsonPrimitive?.int
            ?: ShareDocumentDto.CURRENT_SCHEMA_VERSION

    private companion object {
        const val SCHEMA_VERSION_KEY = "schemaVersion"
    }
}
