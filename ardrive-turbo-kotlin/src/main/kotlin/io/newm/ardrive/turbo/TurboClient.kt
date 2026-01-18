package io.newm.ardrive.turbo

import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.payment.PaymentService
import io.newm.ardrive.turbo.payment.model.PaymentBalance
import io.newm.ardrive.turbo.upload.UploadService
import io.newm.ardrive.turbo.upload.model.ArweaveManifest
import io.newm.ardrive.turbo.upload.model.ChunkingMode
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.DataItemTag
import io.newm.ardrive.turbo.upload.model.ManifestFallback
import io.newm.ardrive.turbo.upload.model.ManifestIndex
import io.newm.ardrive.turbo.upload.model.ManifestPath
import io.newm.ardrive.turbo.upload.model.UploadDataItemResponse
import io.newm.ardrive.turbo.upload.model.UploadEvents
import io.newm.ardrive.turbo.upload.model.UploadFileDescriptor
import java.io.InputStream

/**
 * Authenticated client for Turbo upload and payment APIs.
 */
interface TurboClient {
    val config: TurboConfig
    val signer: ArweaveSigner
    val uploadService: UploadService
    val paymentService: PaymentService

    fun getWalletAddress(): String

    suspend fun getPaymentBalance(): PaymentBalance

    suspend fun uploadDataItem(
        data: ByteArray,
        contentType: String? = null,
        paidBy: List<String> = emptyList(),
    ): UploadDataItemResponse

    suspend fun uploadSignedDataItem(
        signedDataItem: ByteArray,
        paidBy: List<String> = emptyList(),
    ): UploadDataItemResponse

    suspend fun uploadFile(
        data: ByteArray,
        contentType: String,
        paidBy: List<String> = emptyList(),
        chunkByteCount: Long? = null,
        maxChunkConcurrency: Int? = null,
        chunkingMode: ChunkingMode = ChunkingMode.AUTO,
        events: UploadEvents? = null,
    ): UploadDataItemResponse

    suspend fun uploadFile(
        streamFactory: () -> InputStream,
        sizeFactory: () -> Long,
        contentType: String,
        paidBy: List<String> = emptyList(),
        chunkByteCount: Long? = null,
        maxChunkConcurrency: Int? = null,
        chunkingMode: ChunkingMode = ChunkingMode.AUTO,
        events: UploadEvents? = null,
    ): UploadDataItemResponse

    suspend fun uploadFolder(
        files: List<UploadFileDescriptor>,
        paidBy: List<String> = emptyList(),
        manifestEnabled: Boolean = true,
        indexFile: String? = null,
        fallbackFile: String? = null,
        events: UploadEvents? = null,
    ): UploadFolderResponse
}

/**
 * Result for a folder upload, including optional manifest output.
 */
data class UploadFolderResponse(
    val fileResponses: List<UploadDataItemResponse>,
    val manifestResponse: UploadDataItemResponse? = null,
    val manifest: ArweaveManifest? = null,
    val errors: List<Throwable> = emptyList(),
)

/**
 * Unauthenticated client for public payment endpoints.
 */
interface TurboClientUnauthenticated {
    val config: TurboConfig
    val paymentService: PaymentService
}

internal fun buildUploadOptions(
    contentType: String?,
    paidBy: List<String>,
): DataItemOptions {
    val tags = if (contentType == null) {
        emptyList()
    } else {
        listOf(DataItemTag("Content-Type", contentType))
    }
    return DataItemOptions(tags = tags, paidBy = paidBy)
}

internal fun buildManifest(
    files: List<UploadFileDescriptor>,
    responses: Map<String, UploadDataItemResponse>,
    indexFile: String?,
    fallbackFile: String?,
): ArweaveManifest {
    val paths = files.associate { file ->
        file.path to ManifestPath(responses.getValue(file.path).id)
    }
    val indexPath = when {
        indexFile != null && paths.containsKey(indexFile) -> indexFile
        paths.containsKey("index.html") -> "index.html"
        else -> paths.keys.firstOrNull() ?: "index.html"
    }
    val fallbackId = when {
        fallbackFile != null && paths.containsKey(fallbackFile) -> paths.getValue(fallbackFile).id
        paths.containsKey("404.html") -> paths.getValue("404.html").id
        else -> paths.getValue(indexPath).id
    }
    return ArweaveManifest(
        index = ManifestIndex(indexPath),
        paths = paths,
        fallback = ManifestFallback(fallbackId),
    )
}
