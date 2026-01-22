package io.newm.server.features.arweave.repo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.ardrive.turbo.TurboClient
import io.newm.ardrive.turbo.TurboClientFactory
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.TurboLogSeverity
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.payment.ArweaveTokenTools
import io.newm.ardrive.turbo.payment.PollingOptions
import io.newm.ardrive.turbo.payment.TokenCreateTxParams
import io.newm.ardrive.turbo.payment.model.FundTransactionStatus
import io.newm.ardrive.turbo.upload.model.UploadEvents
import io.newm.server.features.arweave.ktx.toFiles
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.ktx.toBucketAndKey
import io.newm.server.typealiases.SongId
import io.newm.shared.coroutine.SupervisorScope
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getBigDecimal
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.info
import io.newm.shared.ktx.warn
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

class ArweaveRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val emailRepository: EmailRepository,
) : ArweaveRepository,
    SupervisorScope {
    override val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val httpClient: HttpClient by inject()
    private val songRepository: SongRepository by inject()
    private val s3Presigner: S3Presigner by inject()
    private val minWalletBalance: BigDecimal by lazy {
        environment.getConfigChild("arweave").getBigDecimal("minWalletBalance")
    }
    private val checkAndFundThreshold: BigDecimal by lazy {
        environment.getConfigChild("arweave").getBigDecimal("checkAndFundThresholdAr")
    }
    private val turboConfig by lazy {
        val turboConfig = environment.getConfigChild("arweave.turbo")
        TurboConfig(
            uploadBaseUrl = turboConfig.getString("uploadBaseUrl"),
            paymentBaseUrl = turboConfig.getString("paymentBaseUrl"),
            requestTimeout = turboConfig.getInt("requestTimeoutSeconds").seconds,
            connectTimeout = turboConfig.getInt("connectTimeoutSeconds").seconds,
            socketTimeout = turboConfig.getInt("socketTimeoutSeconds").seconds,
            retryPolicy = io.newm.ardrive.turbo.RetryPolicy(
                maxAttempts = turboConfig.getInt("retryMaxAttempts"),
            ),
            logSeverity = TurboLogSeverity.INFO,
            logLevel = LogLevel.NONE,
        )
    }
    private val walletJson: String by lazy {
        runBlocking { environment.getSecureConfigString("arweave.walletJson") }
    }
    private val signer: ArweaveSigner by lazy {
        ArweaveSigner(walletJson)
    }
    private val tokenTools: ArweaveTokenTools by lazy {
        ArweaveTokenTools(
            gatewayUrl = arweaveGateway,
            // 30 minutes total polling time
            pollingOptions = PollingOptions(
                maxAttempts = 30,
                initialBackoffMs = 60_000,
                pollingIntervalMs = 60_000,
            )
        )
    }
    private val turboClient: TurboClient by lazy {
        TurboClientFactory.createAuthenticated(
            walletJson = walletJson,
            config = turboConfig,
            tokenTools = tokenTools,
        )
    }

    private var nextWalletBalanceCheck = 0L

    override suspend fun getWalletAddress(): String = turboClient.getWalletAddress()

    override suspend fun getWalletARBalance(): BigDecimal = getWalletArBalance()

    override suspend fun checkAndFundTurboBalance() {
        if (Instant.now().toEpochMilli() < nextWalletBalanceCheck) {
            return
        }
        nextWalletBalanceCheck = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
        val paymentBalance = turboClient.getPaymentBalance()
        val wincBalance = paymentBalance.winc.toBigDecimalOrNull()
            ?: throw IllegalStateException("Unexpected payment balance winc value: ${paymentBalance.winc}")
        val conversionRate = getWincPerArRate()
        val balanceAr = wincBalance.divide(conversionRate, MathContext.DECIMAL128)
        if (balanceAr >= checkAndFundThreshold) {
            return
        }
        val walletBalance = getWalletArBalance()
        if (walletBalance < minWalletBalance) {
            log.warn { "Low Wallet Balance: $walletBalance AR" }
            with(environment.getConfigChild("arweave.warningEmail")) {
                emailRepository.send(
                    to = getString("to"),
                    subject = getString("subject"),
                    messageUrl = getString("messageUrl"),
                    messageArgs = mapOf("balance" to walletBalance)
                )
            }
            return
        }
        submitTopUp()
    }

    private suspend fun getWalletArBalance(): BigDecimal {
        val walletAddress = turboClient.getWalletAddress()
        val response = httpClient.get("$arweaveGateway/wallet/$walletAddress/balance") {
            accept(ContentType.Text.Plain)
        }
        val responseBody = response.body<String>()
        val balance = responseBody.trim().toBigDecimalOrNull()
            ?: throw IllegalStateException("Unexpected wallet balance response: $responseBody")
        return balance.movePointLeft(WINC_DECIMALS)
    }

    private suspend fun submitTopUp() {
        log.info { "Turbo balance below threshold; submitting top-up." }
        val target = getTargetWalletForFund(TokenType.ARWEAVE)
        val txResult = tokenTools.createAndSubmitTx(
            TokenCreateTxParams(
                target = target,
                tokenAmount = checkAndFundThreshold.toPlainString(),
                feeMultiplier = 1.0,
                signer = signer,
            ),
        )
        runCatching { tokenTools.pollTxAvailability(txResult.id) }
            .onFailure { exception ->
                log.warn(exception) { "Top-up polling timed out; submitting anyway." }
            }
        val submitResponse = turboClient.paymentService.submitFundTransaction(
            token = TokenType.ARWEAVE,
            transactionId = txResult.id,
        )
        if (submitResponse.status != FundTransactionStatus.CONFIRMED) {
            log.warn { "Top-up submitted but not confirmed: $submitResponse" }
        }
    }

    private suspend fun getTargetWalletForFund(token: TokenType): String {
        val info = turboClient.paymentService.getServiceInfo()
        val address = info.addresses[token]
        if (address.isNullOrBlank()) {
            throw IllegalStateException("No wallet address found for token type: $token")
        }
        return address
    }

    private suspend fun getWincPerArRate(): BigDecimal {
        val priceForBytes = turboClient.paymentService.getPriceForBytes(CONVERSION_RATE_BYTES)
        val uploadPrice = turboClient.paymentService.getUploadPrice(TokenType.ARWEAVE, CONVERSION_RATE_BYTES)
        val winc = priceForBytes.winc.toBigDecimalOrNull()
            ?: throw IllegalStateException("Unexpected priceForBytes winc value: ${priceForBytes.winc}")
        val ar = uploadPrice.tokenPrice.toBigDecimalOrNull()
            ?: throw IllegalStateException("Unexpected upload price AR value: ${uploadPrice.tokenPrice}")
        return calculateWincPerArRate(winc, ar)
    }

    override suspend fun uploadSongAssets(
        song: Song,
        testMode: Boolean,
    ) {
        log.info { "Uploading song assets for song: ${song.id}" }
        if (!testMode) {
            checkAndFundTurboBalance()
        }

        val release = songRepository.getRelease(song.releaseId!!)
        val files = song.toFiles(release)
        val responses = mutableListOf<ArweaveUploadResponse>()
        val errors = mutableListOf<Throwable>()

        for ((inputUrl, contentType) in files) {
            try {
                val responseId =
                    if (testMode) {
                        buildTestUploadId(song.id!!, inputUrl, contentType)
                    } else {
                        val downloadUrl = resolveDownloadUrl(inputUrl)
                        val tempFile = downloadToTemp(downloadUrl)
                        try {
                            val response = turboClient.uploadFile(
                                streamFactory = { tempFile.inputStream() },
                                sizeFactory = { tempFile.length() },
                                contentType = contentType,
                                paidBy = emptyList(),
                                events = UploadEvents(),
                            )
                            response.id
                        } finally {
                            if (tempFile.exists()) {
                                tempFile.delete()
                            }
                        }
                    }
                responses += ArweaveUploadResponse(inputUrl, contentType, responseId, null)
            } catch (ex: Throwable) {
                log.error("Error uploading file $inputUrl", ex)
                responses += ArweaveUploadResponse(inputUrl, contentType, "", ex)
                errors += ex
            }
        }

        responses.forEach { response ->
            response.error?.let {
                return@forEach
            }
            val arweaveUrl = "ar://${response.id}"
            val toUpdate: Any? =
                when (response.contentType) {
                    "image/webp" -> {
                        Release(arweaveCoverArtUrl = arweaveUrl)
                    }

                    "application/pdf" -> {
                        Song(arweaveTokenAgreementUrl = arweaveUrl)
                    }

                    "audio/mpeg" -> {
                        Song(arweaveClipUrl = arweaveUrl)
                    }

                    "text/plain" -> {
                        Song(arweaveLyricsUrl = arweaveUrl)
                    }

                    else -> {
                        errors += IllegalStateException("Unknown mime type: ${response.contentType}")
                        null
                    }
                }
            when (toUpdate) {
                is Song -> {
                    songRepository.update(song.id!!, toUpdate)
                    log.info { "Song ${song.id} updated: $toUpdate" }
                }

                is Release -> {
                    songRepository.update(song.releaseId, toUpdate)
                    log.info { "Release ${song.releaseId} updated: $toUpdate" }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw IllegalStateException("Error uploading to Arweave song: ${song.id}", errors.first())
        }
    }

    private val arweaveGateway: String by lazy {
        with(environment.getConfigChild("arweave")) {
            val scheme = getString("scheme")
            val host = getString("host")
            "$scheme://$host"
        }
    }

    companion object {
        private const val WINC_DECIMALS = 12
        private const val WINC_RATE_SCALE = 18
        private const val CONVERSION_RATE_BYTES = 5L * 1024L * 1024L

        internal fun calculateWincPerArRate(
            winc: BigDecimal,
            ar: BigDecimal,
        ): BigDecimal {
            if (ar <= BigDecimal.ZERO) {
                throw IllegalStateException("Unexpected upload price AR value: $ar")
            }
            return winc.divide(ar, WINC_RATE_SCALE, RoundingMode.HALF_UP)
        }
    }

    private fun buildTestUploadId(
        songId: SongId,
        inputUrl: String,
        contentType: String,
    ): String {
        val key = "$songId|$contentType|$inputUrl"
        return "test_${key.hashCode().toString().replace('-', '0')}"
    }

    private fun resolveDownloadUrl(inputUrl: String): String =
        if (inputUrl.startsWith("s3://")) {
            val (bucket, key) = inputUrl.toBucketAndKey()
            val getObjectRequest =
                GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key)
                    .build()

            val presignGetObjectRequest =
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(Duration.ofMinutes(30))
                    .getObjectRequest(getObjectRequest)
                    .build()

            s3Presigner.presignGetObject(presignGetObjectRequest).url().toExternalForm()
        } else {
            inputUrl
        }

    private suspend fun downloadToTemp(url: String): File =
        withContext(Dispatchers.IO) {
            val tempFile = kotlin.io.path
                .createTempFile()
                .toFile()
            tempFile.outputStream().use { output ->
                URI(url).toURL().openStream().use { input ->
                    input.copyTo(output)
                }
            }
            tempFile
        }
}

data class ArweaveUploadResponse(
    val url: String,
    val contentType: String,
    val id: String,
    val error: Throwable?,
)
