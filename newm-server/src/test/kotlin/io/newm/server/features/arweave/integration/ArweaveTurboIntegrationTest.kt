package io.newm.server.features.arweave.integration

import com.google.common.truth.Truth.assertThat
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.newm.ardrive.turbo.RetryPolicy
import io.newm.ardrive.turbo.TurboClientFactory
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.TurboLogSeverity
import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.payment.ArweaveTokenTools
import io.newm.ardrive.turbo.payment.PollingOptions
import io.newm.ardrive.turbo.upload.model.ChunkingMode
import io.newm.ardrive.turbo.upload.model.UploadEvents
import io.newm.ardrive.turbo.upload.model.UploadProgress
import java.io.File
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Manual-only integration tests. Requires real Turbo wallet JSON and funded wallet.")
class ArweaveTurboIntegrationTest {
    private val log by lazy { KotlinLogging.logger { } }

    // NEVER commit real wallet JSON to source control. Paste locally only.
    private val walletJson =
        """"""

    private val config =
        TurboConfig(
            uploadBaseUrl = "https://upload.ardrive.io",
            paymentBaseUrl = "https://payment.ardrive.io",
            requestTimeout = 60.seconds,
            connectTimeout = 10.seconds,
            socketTimeout = 60.seconds,
            retryPolicy = RetryPolicy(maxAttempts = 3),
            logSeverity = TurboLogSeverity.INFO,
        )

    private val turboClient =
        TurboClientFactory.createAuthenticated(
            walletJson = walletJson,
            config = config,
            tokenTools = ArweaveTokenTools(
                gatewayUrl = "https://arweave.net",
                // 30 minutes total polling time
                pollingOptions = PollingOptions(
                    maxAttempts = 30,
                    initialBackoffMs = 60_000,
                    pollingIntervalMs = 60_000,
                )
            ),
        )

    @Test
    fun `upload small file`() =
        runBlocking {
            val tempFile = Files.createTempFile("newm-turbo-integration", ".txt")
            tempFile.writeText("hello turbo integration")

            val upload = turboClient.uploadFile(tempFile.toFile().readBytes(), "text/plain")
            assertThat(upload.id).isNotEmpty()
            log.info { "uploadFile response: $upload" }
            log.info { "Direct URL: https://arweave.net/${upload.id}" }
        }

    @Test
    fun `upload webp file`() =
        runBlocking {
            val tempFile = File("/home/westbam/Development/newm-server/tessellation.png")

            val upload = turboClient.uploadFile(
                data = tempFile.readBytes(),
                contentType = "image/png",
                chunkingMode = ChunkingMode.DISABLED
            )
            assertThat(upload.id).isNotEmpty()
            log.info { "uploadFile response: $upload" }
            log.info { "Direct URL: https://arweave.net/${upload.id}" }
        }

    @Test
    fun `fetch balance and price quotes`() =
        runBlocking {
            val walletAddress = turboClient.getWalletAddress()
            log.info { "Turbo wallet address: $walletAddress" }

            val arweaveBalance = fetchArweaveBalance(walletAddress)
            log.info { "arweave balance response: $arweaveBalance" }

            log.info { "Note: Turbo payment API may return 404 for new users; SDK treats that as zero balance." }
            val balance = turboClient.getPaymentBalance()
            assertThat(balance.winc).isNotEmpty()
            log.info { "getPaymentBalance response: $balance" }

//            val largeFilePath = "/home/westbam/Development/newm-server/tessellation.png"
//            val largeFile = File(largeFilePath)
//            require(largeFile.exists()) {
//                "Large test file not found at: $largeFilePath. " +
//                    "This test requires a 2.7GB video file for streaming upload testing."
//            }
//            val fileSizeBytes = largeFile.length()
            val fileSizeBytes = 1024L * 1024L // 1 MB

            val priceForBytes = turboClient.paymentService.getPriceForBytes(fileSizeBytes)
            assertThat(priceForBytes.winc).isNotEmpty()
            log.info { "getPriceForBytes response: $priceForBytes" }

            val priceForUpload =
                turboClient.paymentService.getUploadPrice(
                    token = TokenType.ARWEAVE,
                    byteCount = fileSizeBytes,
                )
            assertThat(priceForUpload.tokenPrice).isNotEmpty()
            log.info { "getUploadPrice response: $priceForUpload" }
        }

    @Test
    fun `sanity check payment service info`() =
        runBlocking {
            val info = turboClient.paymentService.getServiceInfo()
            assertThat(info.version).isNotEmpty()
            log.info { "payment service info response: $info" }
        }

    @Test
    fun `top up with AR tokens`() =
        runBlocking {
            val walletAddress = turboClient.getWalletAddress()
            val info = turboClient.paymentService.getServiceInfo()
            val fundingTarget = info.addresses[TokenType.ARWEAVE]

            log.info { "Turbo wallet address: $walletAddress" }
            log.info { "Turbo funding target (arweave): $fundingTarget" }
            log.info { "Note: Turbo payment API may return 404 for new users; SDK treats that as zero balance." }

            val response = turboClient.paymentService.topUpWithTokens(
                token = TokenType.ARWEAVE,
                tokenAmount = "0.2",
            )
            assertThat(response.status.name).isNotEmpty()
            log.info { "topUpWithTokens response: $response" }
            log.info { "topUpWithTokens txId=${response.id} status=${response.status} block=${response.block} reward=${response.reward} target=${response.target}" }
        }

    @Test
    fun `submit fund transaction after delayed confirmation`() =
        runBlocking {
            val txId = ""
            val response = turboClient.paymentService.submitFundTransaction(
                token = TokenType.ARWEAVE,
                transactionId = txId,
            )
            assertThat(response.status.name).isNotEmpty()
            log.info { "submitFundTransaction response: $response" }
            log.info { "submitFundTransaction txId=${response.id} status=${response.status} block=${response.block}" }
        }

    @Test
    fun `poll tx availability for known transaction`() =
        runBlocking {
            val txId = "Eru5Iz_GV7mUzHurAd3kuI6HL0DUyJd_BRmr3oqJ51g"
            log.info { "pollTxAvailability test with txId=$txId" }
            val tokenTools =
                ArweaveTokenTools(
                    gatewayUrl = "https://arweave.net",
                    pollingOptions = PollingOptions(
                        maxAttempts = 3,
                        initialBackoffMs = 3_000,
                        pollingIntervalMs = 3_000,
                    )
                )
            tokenTools.pollTxAvailability(txId)
            assertThat(true).isTrue()
        }

    private suspend fun fetchArweaveBalance(address: String): String =
        HttpClient(CIO).use { client ->
            client
                .get("https://arweave.net/wallet/$address/balance") {
                    accept(ContentType.Text.Plain)
                }.body<String>()
        }

    /**
     * Manual integration test for uploading a large 2.7GB file using streaming.
     *
     * This test validates the two-pass streaming upload implementation that:
     * - PASS 1: Streams through the file to compute the deep hash signature
     * - PASS 2: Streams through the file again to upload chunks
     *
     * Memory usage should stay at O(chunk size) ~5MB instead of O(file size) ~2.7GB.
     *
     * Prerequisites:
     * 1. Set walletJson to a valid funded Arweave wallet
     * 2. Ensure the file exists at the specified path
     * 3. Ensure sufficient Turbo credits (~$50+ for 2.7GB)
     *
     * Run with: ./gradlew :newm-server:test --tests "*.upload large 2.7GB file with streaming*"
     */
    @Test
    fun `upload large 2_7GB file with streaming`() =
        runBlocking {
            val largeFilePath = "/home/westbam/Development/newm-server/tessellation.png"
            val largeFile = File(largeFilePath)

            require(largeFile.exists()) {
                "Large test file not found at: $largeFilePath. " +
                    "This test requires a big file for streaming upload testing."
            }

            val fileSizeBytes = largeFile.length()
            val fileSizeMB = fileSizeBytes / (1024.0 * 1024.0)
            val fileSizeGB = fileSizeBytes / (1024.0 * 1024.0 * 1024.0)

            log.info { "=== Large File Streaming Upload Test ===" }
            log.info { "File: $largeFilePath" }
            log.info { "Size: ${String.format("%.2f", fileSizeGB)} GB (${String.format("%.2f", fileSizeMB)} MB)" }
            log.info { "Expected memory usage: ~5MB (streaming mode)" }
            log.info { "" }

            // Create a config with extended timeouts for large file upload
            val largeFileConfig = TurboConfig(
                uploadBaseUrl = "https://upload.ardrive.io",
                paymentBaseUrl = "https://payment.ardrive.io",
                requestTimeout = 30.minutes, // Extended for large chunks
                connectTimeout = 30.seconds,
                socketTimeout = 30.minutes, // Extended for large chunks
                retryPolicy = RetryPolicy(maxAttempts = 5), // More retries for reliability
                logSeverity = TurboLogSeverity.WARN,
                logLevel = LogLevel.NONE
            )

            val largeFileTurboClient = TurboClientFactory.createAuthenticated(
                walletJson = walletJson,
                config = largeFileConfig,
                tokenTools = ArweaveTokenTools(
                    gatewayUrl = "https://arweave.net",
                    pollingOptions = PollingOptions(
                        maxAttempts = 30,
                        initialBackoffMs = 60_000,
                        pollingIntervalMs = 60_000,
                    )
                ),
            )

            // Log balance before upload
            val balanceBefore = largeFileTurboClient.getPaymentBalance()
            log.info { "Balance before upload: ${balanceBefore.winc} winc" }

            // Track progress
            var lastProgressLog = 0L
            val startTime = System.currentTimeMillis()

            val uploadEvents = UploadEvents(
                onProgress = { progress: UploadProgress ->
                    val percent = if (progress.totalBytes > 0) {
                        (progress.processedBytes * 100.0 / progress.totalBytes)
                    } else {
                        0.0
                    }

                    // Log every 5% or every 30 seconds
                    val now = System.currentTimeMillis()
                    if (percent.toLong() / 5 > lastProgressLog / 5 || now - lastProgressLog > 30_000) {
                        val elapsedSec = (now - startTime) / 1000.0
                        val mbUploaded = progress.processedBytes / (1024.0 * 1024.0)
                        val speedMBps = if (elapsedSec > 0) mbUploaded / elapsedSec else 0.0

                        log.info {
                            "${progress.step}: ${String.format("%.1f", percent)}% " +
                                "(${String.format("%.1f", mbUploaded)} MB, " +
                                "${String.format("%.2f", speedMBps)} MB/s)"
                        }
                        lastProgressLog = now
                    }
                },
                onError = { error ->
                    log.error(error) { "Upload error: ${error.message}" }
                },
                onSuccess = {
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                    log.info { "Upload completed successfully in ${String.format("%.1f", elapsedSec)} seconds" }
                }
            )

            log.info { "Starting streaming upload..." }
            log.info { "Note: File will be read twice (once for signing, once for upload)" }

            val upload = largeFileTurboClient.uploadFile(
                streamFactory = { largeFile.inputStream() },
                sizeFactory = { largeFile.length() },
                contentType = "image/png",
                events = uploadEvents,
                chunkingMode = ChunkingMode.FORCE,
            )

            assertThat(upload.id).isNotEmpty()

            val elapsedMs = System.currentTimeMillis() - startTime
            val elapsedMin = elapsedMs / 60_000.0
            val avgSpeedMBps = fileSizeMB / (elapsedMs / 1000.0)

            log.info { "" }
            log.info { "=== Upload Complete ===" }
            log.info { "Transaction ID: ${upload.id}" }
            log.info { "Direct URL: https://arweave.net/${upload.id}" }
            log.info { "Total time: ${String.format("%.2f", elapsedMin)} minutes" }
            log.info { "Average speed: ${String.format("%.2f", avgSpeedMBps)} MB/s" }
            log.info { "Winc charged: ${upload.winc}" }

            // Log balance after upload
            val balanceAfter = largeFileTurboClient.getPaymentBalance()
            log.info { "Balance after upload: ${balanceAfter.winc} winc" }
        }
}
