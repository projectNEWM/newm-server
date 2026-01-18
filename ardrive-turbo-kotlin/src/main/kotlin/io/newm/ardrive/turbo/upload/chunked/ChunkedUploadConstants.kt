package io.newm.ardrive.turbo.upload.chunked

internal object ChunkedUploadConstants {
    const val MIN_CHUNK_BYTE_COUNT: Long = 5L * 1024 * 1024
    const val MAX_CHUNK_BYTE_COUNT: Long = 500L * 1024 * 1024
    const val DEFAULT_CHUNK_BYTE_COUNT: Long = MIN_CHUNK_BYTE_COUNT
    const val DEFAULT_MAX_CHUNK_CONCURRENCY: Int = 5
    const val MAX_CHUNK_CONCURRENCY: Int = 256
    const val CHUNKING_HEADER_KEY = "x-chunking-version"
    const val CHUNKING_HEADER_VALUE = "2"
}
