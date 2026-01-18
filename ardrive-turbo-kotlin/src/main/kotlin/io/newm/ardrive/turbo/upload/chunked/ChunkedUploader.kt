package io.newm.ardrive.turbo.upload.chunked

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.ByteArrayContent
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.upload.model.ChunkingMode
import io.newm.ardrive.turbo.upload.model.CreateChunkedUploadResponse
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.MultiPartStatus
import io.newm.ardrive.turbo.upload.model.StreamingSignedDataItem
import io.newm.ardrive.turbo.upload.model.UploadDataItemResponse
import io.newm.ardrive.turbo.util.HttpClientFactory
import kotlin.math.ceil
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class ChunkedUploader(
    private val config: TurboConfig,
    private val httpClient: HttpClient = HttpClientFactory.create(config),
) {
    val defaultMaxConcurrency: Int = ChunkedUploadConstants.DEFAULT_MAX_CHUNK_CONCURRENCY

    suspend fun upload(
        token: String,
        dataItem: ByteArray,
        dataItemOptions: DataItemOptions?,
        chunkByteCount: Long = ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT,
        maxChunkConcurrency: Int = ChunkedUploadConstants.DEFAULT_MAX_CHUNK_CONCURRENCY,
        chunkingMode: ChunkingMode = ChunkingMode.AUTO,
        maxFinalizeMs: Long? = null,
    ): UploadDataItemResponse {
        val shouldChunk = shouldChunkUpload(chunkingMode, chunkByteCount, dataItem.size.toLong())
        if (!shouldChunk) {
            throw IllegalArgumentException("Chunking not required for this payload")
        }
        validateChunkParams(chunkByteCount, maxChunkConcurrency, maxFinalizeMs, chunkingMode)
        val uploadId = initUpload(token, chunkByteCount)
        uploadChunks(token, uploadId, dataItem, chunkByteCount, maxChunkConcurrency)
        return finalizeUpload(token, uploadId, dataItem.size.toLong(), dataItemOptions?.paidBy, maxFinalizeMs)
    }

    /**
     * Uploads a streaming signed data item in chunks.
     *
     * This is the memory-efficient alternative to [upload] for large files (>100MB).
     * The data is read from the stream factory during upload, never fully loaded into memory.
     *
     * Memory usage: O(chunk size) ~5MB, instead of O(file size)
     *
     * Note: Streaming uploads are sequential (not concurrent) because the data must be
     * read in order from the stream. This is a trade-off for memory efficiency.
     *
     * @param token The upload token (e.g., "arweave")
     * @param signedDataItem The streaming signed data item containing header and stream factory
     * @param dataItemOptions Optional data item options (for paidBy header)
     * @param chunkByteCount Size of each chunk in bytes
     * @param maxFinalizeMs Maximum time to wait for finalization
     * @param onChunkUploaded Optional callback invoked after each chunk is uploaded
     */
    suspend fun uploadStreaming(
        token: String,
        signedDataItem: StreamingSignedDataItem,
        dataItemOptions: DataItemOptions?,
        chunkByteCount: Long = ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT,
        maxFinalizeMs: Long? = null,
        onChunkUploaded: ((chunkIndex: Int, bytesUploaded: Long, totalBytes: Long) -> Unit)? = null,
    ): UploadDataItemResponse {
        require(chunkByteCount in ChunkedUploadConstants.MIN_CHUNK_BYTE_COUNT..ChunkedUploadConstants.MAX_CHUNK_BYTE_COUNT) {
            "Invalid chunk size. Must be between ${ChunkedUploadConstants.MIN_CHUNK_BYTE_COUNT} and ${ChunkedUploadConstants.MAX_CHUNK_BYTE_COUNT}."
        }
        val uploadId = initUpload(token, chunkByteCount)
        uploadChunksStreaming(token, uploadId, signedDataItem, chunkByteCount, onChunkUploaded)
        return finalizeUpload(token, uploadId, signedDataItem.totalSize, dataItemOptions?.paidBy, maxFinalizeMs)
    }

    /**
     * Uploads chunks by streaming from the signed data item.
     *
     * PASS 2: This reads the data stream a second time (after signing) to upload chunks.
     * Chunks are processed sequentially to maintain stream ordering.
     */
    private suspend fun uploadChunksStreaming(
        token: String,
        uploadId: String,
        signedDataItem: StreamingSignedDataItem,
        chunkByteCount: Long,
        onChunkUploaded: ((chunkIndex: Int, bytesUploaded: Long, totalBytes: Long) -> Unit)?,
    ) {
        val chunkSize = chunkByteCount.toInt()
        val totalSize = signedDataItem.totalSize
        val header = signedDataItem.header

        // PASS 2: Stream through data for upload
        signedDataItem.dataStreamFactory().use { dataStream ->
            var globalOffset = 0L
            var headerBytesUsed = 0
            var chunkIndex = 0

            while (globalOffset < totalSize) {
                // Keep remainingBytes as Long to avoid overflow for files > 2GB
                val remainingBytes = totalSize - globalOffset
                val thisChunkSize = minOf(chunkSize.toLong(), remainingBytes).toInt()
                val chunk = ByteArray(thisChunkSize)
                var chunkFilled = 0

                // Fill from header if not fully consumed
                if (headerBytesUsed < header.size) {
                    val headerBytesToCopy = minOf(header.size - headerBytesUsed, thisChunkSize)
                    System.arraycopy(header, headerBytesUsed, chunk, 0, headerBytesToCopy)
                    headerBytesUsed += headerBytesToCopy
                    chunkFilled = headerBytesToCopy
                }

                // Fill remainder from data stream
                while (chunkFilled < thisChunkSize) {
                    val bytesRead = dataStream.read(chunk, chunkFilled, thisChunkSize - chunkFilled)
                    if (bytesRead == -1) break
                    chunkFilled += bytesRead
                }

                // Upload chunk
                httpClient.post("${config.uploadBaseUrl}/v1/chunks/$token/$uploadId/$globalOffset") {
                    header(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                    header(ChunkedUploadConstants.CHUNKING_HEADER_KEY, ChunkedUploadConstants.CHUNKING_HEADER_VALUE)
                    setBody(ByteArrayContent(chunk))
                }

                globalOffset += chunkFilled
                onChunkUploaded?.invoke(chunkIndex, globalOffset, totalSize)
                chunkIndex++
            }
        }
    }

    private suspend fun initUpload(
        token: String,
        chunkByteCount: Long
    ): String {
        val response = httpClient
            .get("${config.uploadBaseUrl}/v1/chunks/$token/-1/-1?chunkSize=$chunkByteCount") {
                header(ChunkedUploadConstants.CHUNKING_HEADER_KEY, ChunkedUploadConstants.CHUNKING_HEADER_VALUE)
            }.body<CreateChunkedUploadResponse>()
        return response.id
    }

    /**
     * Uploads data in chunks using lazy chunk creation.
     *
     * Performance optimization: Instead of creating all chunk copies upfront,
     * chunks are created lazily within each coroutine. This reduces peak memory
     * usage from 2x file size to ~1x + one chunk at a time, and allows GC to
     * reclaim chunk memory as soon as each upload completes.
     */
    private suspend fun uploadChunks(
        token: String,
        uploadId: String,
        dataItem: ByteArray,
        chunkByteCount: Long,
        maxChunkConcurrency: Int,
    ) {
        val chunkSize = chunkByteCount.toInt()
        val chunkCount = (dataItem.size + chunkSize - 1) / chunkSize

        coroutineScope {
            val jobs = (0 until chunkCount).map { index ->
                val chunkOffset = index.toLong() * chunkByteCount
                val startOffset = index * chunkSize
                val endOffset = minOf(startOffset + chunkSize, dataItem.size)

                async(Dispatchers.IO) {
                    // Create chunk copy lazily, only when this coroutine executes
                    val chunk = dataItem.copyOfRange(startOffset, endOffset)
                    httpClient.post("${config.uploadBaseUrl}/v1/chunks/$token/$uploadId/$chunkOffset") {
                        header(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                        header(ChunkedUploadConstants.CHUNKING_HEADER_KEY, ChunkedUploadConstants.CHUNKING_HEADER_VALUE)
                        setBody(ByteArrayContent(chunk))
                    }
                }
            }
            if (maxChunkConcurrency <= 1) {
                jobs.forEach { it.await() }
            } else {
                jobs.chunked(maxChunkConcurrency).forEach { batch -> batch.awaitAll() }
            }
        }
    }

    private suspend fun finalizeUpload(
        token: String,
        uploadId: String,
        dataItemSize: Long,
        paidBy: List<String>?,
        maxFinalizeMs: Long?,
    ): UploadDataItemResponse {
        httpClient.post("${config.uploadBaseUrl}/v1/chunks/$token/$uploadId/finalize") {
            header(HttpHeaders.ContentType, ContentType.Application.OctetStream)
            header(ChunkedUploadConstants.CHUNKING_HEADER_KEY, ChunkedUploadConstants.CHUNKING_HEADER_VALUE)
            if (!paidBy.isNullOrEmpty()) {
                header("x-paid-by", paidBy.joinToString(","))
            }
            setBody(ByteArrayContent(ByteArray(0)))
        }

        val maxWaitTimeMs = maxFinalizeMs ?: defaultMaxFinalizeMs(dataItemSize)
        val deadline = System.currentTimeMillis() + maxWaitTimeMs
        while (System.currentTimeMillis() < deadline) {
            val status = httpClient.get("${config.uploadBaseUrl}/v1/chunks/$token/$uploadId/status").body<MultiPartStatus>()
            if (status.status == "FINALIZED" && status.receipt != null) {
                return UploadDataItemResponse(
                    dataCaches = status.receipt.dataCaches,
                    fastFinalityIndexes = status.receipt.fastFinalityIndexes,
                    id = status.receipt.id,
                    owner = status.receipt.owner,
                    winc = status.receipt.winc,
                )
            }
            if (status.status == "UNDERFUNDED") {
                throw IllegalStateException("Insufficient balance for chunked upload")
            }
            delay(calculateFinalizeDelay(dataItemSize))
        }
        throw IllegalStateException("Chunked upload finalization timed out")
    }

    private fun calculateFinalizeDelay(dataItemSize: Long): Long =
        if (dataItemSize < 100L * 1024 * 1024) {
            2000
        } else if (dataItemSize < 3L * 1024 * 1024 * 1024) {
            4000
        } else {
            max(1500L * ceil(dataItemSize / (1024.0 * 1024.0 * 1024.0)).toLong(), 15000)
        }

    private fun defaultMaxFinalizeMs(dataItemSize: Long): Long {
        val gib = ceil(dataItemSize / (1024.0 * 1024.0 * 1024.0)).toLong()
        return (gib * 2.5 * 60 * 1000).toLong()
    }

    fun shouldChunkUpload(
        chunkingMode: ChunkingMode,
        chunkByteCount: Long,
        dataItemSize: Long,
    ): Boolean =
        when (chunkingMode) {
            ChunkingMode.DISABLED -> false
            ChunkingMode.FORCE -> true
            ChunkingMode.AUTO -> dataItemSize > chunkByteCount * 2
        }

    private fun validateChunkParams(
        chunkByteCount: Long,
        maxChunkConcurrency: Int,
        maxFinalizeMs: Long?,
        chunkingMode: ChunkingMode,
    ) {
        require(chunkByteCount in ChunkedUploadConstants.MIN_CHUNK_BYTE_COUNT..ChunkedUploadConstants.MAX_CHUNK_BYTE_COUNT) {
            "Invalid chunk size. Must be between ${ChunkedUploadConstants.MIN_CHUNK_BYTE_COUNT} and ${ChunkedUploadConstants.MAX_CHUNK_BYTE_COUNT}."
        }
        require(maxChunkConcurrency in 1..ChunkedUploadConstants.MAX_CHUNK_CONCURRENCY) {
            "Invalid max chunk concurrency. Must be between 1 and ${ChunkedUploadConstants.MAX_CHUNK_CONCURRENCY}."
        }
        require(chunkingMode in ChunkingMode.entries) { "Invalid chunking mode." }
        if (maxFinalizeMs != null) {
            require(maxFinalizeMs >= 0) { "Invalid max finalization wait time. Must be non-negative." }
        }
    }
}
