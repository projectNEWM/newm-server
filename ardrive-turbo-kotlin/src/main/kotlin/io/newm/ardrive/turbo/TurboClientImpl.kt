package io.newm.ardrive.turbo

import io.ktor.client.HttpClient
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.payment.PaymentService
import io.newm.ardrive.turbo.payment.PaymentServiceImpl
import io.newm.ardrive.turbo.payment.TokenTools
import io.newm.ardrive.turbo.payment.model.PaymentBalance
import io.newm.ardrive.turbo.upload.UploadService
import io.newm.ardrive.turbo.upload.UploadServiceImpl
import io.newm.ardrive.turbo.upload.chunked.ChunkedUploadConstants
import io.newm.ardrive.turbo.upload.chunked.ChunkedUploader
import io.newm.ardrive.turbo.upload.model.ChunkingMode
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.UploadDataItemResponse
import io.newm.ardrive.turbo.upload.model.UploadEvents
import io.newm.ardrive.turbo.upload.model.UploadFileDescriptor
import io.newm.ardrive.turbo.upload.model.UploadProgress
import io.newm.ardrive.turbo.upload.model.UploadStep
import io.newm.ardrive.turbo.upload.util.DataItemSigner
import io.newm.ardrive.turbo.upload.util.SignedDataItemStreamFactory
import io.newm.ardrive.turbo.upload.util.StreamingDataItemSigner
import io.newm.ardrive.turbo.util.HttpClientFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlinx.serialization.json.Json

class TurboClientImpl(
    override val config: TurboConfig,
    override val signer: ArweaveSigner,
    tokenTools: TokenTools? = null,
    httpClient: HttpClient? = null,
) : TurboClient {
    companion object {
        /**
         * Threshold above which streaming mode is used to conserve memory.
         * Files >= 100MB will use two-pass streaming to avoid loading the entire file into memory.
         */
        const val STREAMING_THRESHOLD_BYTES = 100L * 1024 * 1024 // 100MB
    }

    private val effectiveConfig = httpClient?.let { config.copy(httpClient = it) } ?: config
    private val client = effectiveConfig.httpClient ?: HttpClientFactory.create(effectiveConfig)

    override val uploadService: UploadService =
        UploadServiceImpl(effectiveConfig, signer, client)

    override val paymentService: PaymentService =
        PaymentServiceImpl(effectiveConfig, signer, tokenTools, client)

    override fun getWalletAddress(): String = signer.getAddress()

    override suspend fun getPaymentBalance(): PaymentBalance = paymentService.getBalance()

    override suspend fun uploadDataItem(
        data: ByteArray,
        contentType: String?,
        paidBy: List<String>,
    ): UploadDataItemResponse =
        uploadService.uploadDataItem(
            dataItem = data,
            dataItemOptions = buildUploadOptions(contentType, paidBy),
            token = null,
        )

    override suspend fun uploadSignedDataItem(
        signedDataItem: ByteArray,
        paidBy: List<String>,
    ): UploadDataItemResponse =
        uploadService.uploadSignedDataItem(
            signedDataItem = signedDataItem,
            dataItemOptions = DataItemOptions(paidBy = paidBy),
            token = null,
        )

    override suspend fun uploadFile(
        data: ByteArray,
        contentType: String,
        paidBy: List<String>,
        chunkByteCount: Long?,
        maxChunkConcurrency: Int?,
        chunkingMode: ChunkingMode,
        events: UploadEvents?,
    ): UploadDataItemResponse =
        uploadFile(
            streamFactory = { ByteArrayInputStream(data) },
            sizeFactory = { data.size.toLong() },
            contentType = contentType,
            paidBy = paidBy,
            chunkByteCount = chunkByteCount,
            maxChunkConcurrency = maxChunkConcurrency,
            chunkingMode = chunkingMode,
            events = events,
        )

    /**
     * Uploads a file with optional chunking support and automatic streaming for large files.
     *
     * For files >= 100MB, uses two-pass streaming to avoid loading the entire file into memory:
     * - Pass 1: Stream through file to compute signature hash
     * - Pass 2: Stream through file again to upload chunks
     *
     * This allows uploading multi-GB files on memory-constrained machines.
     *
     * For smaller files, uses the faster in-memory approach with concurrent chunk uploads.
     */
    override suspend fun uploadFile(
        streamFactory: () -> InputStream,
        sizeFactory: () -> Long,
        contentType: String,
        paidBy: List<String>,
        chunkByteCount: Long?,
        maxChunkConcurrency: Int?,
        chunkingMode: ChunkingMode,
        events: UploadEvents?,
    ): UploadDataItemResponse {
        val dataSize = sizeFactory()
        val options = buildUploadOptions(contentType, paidBy)
        val chunker = ChunkedUploader(effectiveConfig, client)
        val chunkSize = chunkByteCount ?: ChunkedUploadConstants.DEFAULT_CHUNK_BYTE_COUNT

        // Use streaming for large files to avoid OOM
        val useStreaming = dataSize >= STREAMING_THRESHOLD_BYTES &&
            chunker.shouldChunkUpload(chunkingMode, chunkSize, dataSize)

        return try {
            if (useStreaming) {
                uploadFileStreaming(streamFactory, dataSize, options, chunker, chunkSize, events)
            } else {
                uploadFileInMemory(streamFactory, dataSize, options, chunker, chunkSize, chunkingMode, maxChunkConcurrency, events)
            }
        } catch (ex: Throwable) {
            events?.onError?.invoke(ex)
            throw ex
        }
    }

    /**
     * Uploads a large file using two-pass streaming.
     *
     * Memory usage: O(chunk size) ~5MB, instead of O(file size)
     *
     * Trade-off: File is read twice (once for signing, once for upload),
     * and chunks are uploaded sequentially (not concurrently).
     */
    private suspend fun uploadFileStreaming(
        streamFactory: () -> InputStream,
        dataSize: Long,
        options: DataItemOptions,
        chunker: ChunkedUploader,
        chunkSize: Long,
        events: UploadEvents?,
    ): UploadDataItemResponse {
        events?.onProgress?.invoke(UploadProgress(0, dataSize, UploadStep.SIGNING))

        // PASS 1: Stream through file to compute signature
        val streamingSigner = StreamingDataItemSigner(signer)
        val signed = streamingSigner.signStreaming(streamFactory, dataSize, options)

        events?.onProgress?.invoke(UploadProgress(dataSize, dataSize, UploadStep.SIGNING))
        events?.onProgress?.invoke(UploadProgress(0, signed.totalSize, UploadStep.UPLOADING))

        // PASS 2: Stream through file again to upload chunks
        val response = chunker.uploadStreaming(
            token = "arweave",
            signedDataItem = signed,
            dataItemOptions = options,
            chunkByteCount = chunkSize,
            onChunkUploaded = { _, bytesUploaded, totalBytes ->
                events?.onProgress?.invoke(UploadProgress(bytesUploaded, totalBytes, UploadStep.UPLOADING))
            },
        )

        events?.onProgress?.invoke(UploadProgress(signed.totalSize, signed.totalSize, UploadStep.UPLOADING))
        events?.onSuccess?.invoke()
        return response
    }

    /**
     * Uploads a file using the in-memory approach (original behavior).
     *
     * Memory usage: O(file size) - file is fully loaded into memory
     *
     * Advantage: Faster for small files, supports concurrent chunk uploads.
     */
    private suspend fun uploadFileInMemory(
        streamFactory: () -> InputStream,
        dataSize: Long,
        options: DataItemOptions,
        chunker: ChunkedUploader,
        chunkSize: Long,
        chunkingMode: ChunkingMode,
        maxChunkConcurrency: Int?,
        events: UploadEvents?,
    ): UploadDataItemResponse {
        val data = streamFactory().readBytes()
        events?.onProgress?.invoke(UploadProgress(data.size.toLong(), data.size.toLong(), UploadStep.SIGNING))
        val signed = SignedDataItemStreamFactory.fromBytes(
            data,
            DataItemSigner(signer),
            options,
        )
        val dataItemSize = signed.dataItemSizeFactory().toLong()

        return if (chunker.shouldChunkUpload(chunkingMode, chunkSize, dataItemSize)) {
            val dataItemBytes = signed.getBytes()
            events?.onProgress?.invoke(UploadProgress(0, dataItemSize, UploadStep.UPLOADING))
            val response = chunker.upload(
                token = "arweave",
                dataItem = dataItemBytes,
                dataItemOptions = options,
                chunkByteCount = chunkSize,
                maxChunkConcurrency = maxChunkConcurrency ?: chunker.defaultMaxConcurrency,
                chunkingMode = chunkingMode,
            )
            events?.onProgress?.invoke(UploadProgress(dataItemSize, dataItemSize, UploadStep.UPLOADING))
            events?.onSuccess?.invoke()
            response
        } else {
            events?.onProgress?.invoke(UploadProgress(0, dataItemSize, UploadStep.UPLOADING))
            val response = uploadService.uploadSignedDataItem(
                signedDataItem = signed.getBytes(),
                dataItemOptions = options,
                token = null,
            )
            events?.onProgress?.invoke(UploadProgress(dataItemSize, dataItemSize, UploadStep.UPLOADING))
            events?.onSuccess?.invoke()
            response
        }
    }

    override suspend fun uploadFolder(
        files: List<UploadFileDescriptor>,
        paidBy: List<String>,
        manifestEnabled: Boolean,
        indexFile: String?,
        fallbackFile: String?,
        events: UploadEvents?,
    ): UploadFolderResponse {
        val responses = mutableMapOf<String, UploadDataItemResponse>()
        val errors = mutableListOf<Throwable>()
        val fileResponses = mutableListOf<UploadDataItemResponse>()
        for (file in files) {
            try {
                val response = uploadFile(
                    streamFactory = file.streamFactory,
                    sizeFactory = file.sizeFactory,
                    contentType = file.contentType,
                    paidBy = paidBy,
                    events = events,
                )
                responses[file.path] = response
                fileResponses += response
            } catch (ex: Throwable) {
                errors += ex
                events?.onError?.invoke(ex)
            }
        }
        if (!manifestEnabled) {
            return UploadFolderResponse(fileResponses = fileResponses, errors = errors)
        }
        val manifest = buildManifest(files, responses, indexFile, fallbackFile)
        val manifestBytes = Json.encodeToString(manifest).toByteArray()
        val manifestResponse = uploadFile(
            data = manifestBytes,
            contentType = "application/x.arweave-manifest+json",
            paidBy = paidBy,
            events = events,
        )
        return UploadFolderResponse(
            fileResponses = fileResponses,
            manifestResponse = manifestResponse,
            manifest = manifest,
            errors = errors,
        )
    }
}

class TurboClientUnauthenticatedImpl(
    override val config: TurboConfig,
    httpClient: HttpClient? = null,
) : TurboClientUnauthenticated {
    private val effectiveConfig = httpClient?.let { config.copy(httpClient = it) } ?: config

    override val paymentService: PaymentService =
        PaymentServiceImpl(
            effectiveConfig,
            null,
            null,
            effectiveConfig.httpClient ?: HttpClientFactory.create(effectiveConfig)
        )
}
