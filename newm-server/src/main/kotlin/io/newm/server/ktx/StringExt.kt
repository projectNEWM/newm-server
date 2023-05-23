package io.newm.server.ktx

import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.utxo
import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.isValidEmail
import io.newm.shared.ktx.isValidUrl

fun String.checkLength(name: String, max: Int = 64) {
    if (length > max) throw HttpUnprocessableEntityException("Field $name exceeds $max chars limit")
}

fun String?.asValidEmail(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing email")
    if (!isValidEmail()) throw HttpUnprocessableEntityException("Invalid email: $this")
    return this
}

fun String?.asValidUrl(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing url")
    if (!isValidUrl()) throw HttpUnprocessableEntityException("Invalid url: $this")
    return this
}

/**
 * Converts an s3://url into a bucket and key pair.
 */
fun String.toBucketAndKey(): Pair<String, String> {
    val bucket = substringAfter("s3://").substringBefore('/')
    val key = substringAfter(bucket).substringAfter('/')
    return bucket to key
}

fun String.getFileNameWithExtensionFromUrl(): String {
    return substringBefore('?', missingDelimiterValue = this).substringAfterLast('/')
}

fun String.toAudioContentType(): String {
    return when (lowercase().substringAfterLast('.', missingDelimiterValue = this)) {
        "flac" -> "audio/x-flac"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "aiff", "aif" -> "audio/aiff"
        "m4a" -> "audio/mp4"
        else -> "audio/*"
    }
}

fun String.toReferenceUtxo(): Utxo {
    val parts = this.split('#')
    return utxo {
        hash = parts[0]
        ix = parts[1].toLong()
    }
}
