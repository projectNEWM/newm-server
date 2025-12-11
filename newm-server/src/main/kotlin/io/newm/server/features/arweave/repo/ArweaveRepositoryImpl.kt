package io.newm.server.features.arweave.repo

import co.upvest.arweave4s.adt.`Signable$`.`MODULE$` as signableApi
import co.upvest.arweave4s.adt.Tag.`Custom$`.`MODULE$` as tagApi
import co.upvest.arweave4s.adt.`Transaction$`.`MODULE$` as Tx
import co.upvest.arweave4s.adt.`Wallet$`.`MODULE$` as walletApi
import co.upvest.arweave4s.api.`address$`.`MODULE$` as addressApi
import co.upvest.arweave4s.api.`package`.`Config$`.`MODULE$` as configApi
import co.upvest.arweave4s.api.`package`.`future$`.`MODULE$` as future
import co.upvest.arweave4s.api.`price$`.`MODULE$` as priceApi
import co.upvest.arweave4s.api.`tx$`.`MODULE$` as txApi
import cats.Monad
import cats.arrow.FunctionK
import co.upvest.arweave4s.adt.Data
import co.upvest.arweave4s.adt.Signed
import co.upvest.arweave4s.adt.Tag
import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.adt.Wallet
import co.upvest.arweave4s.adt.Winston
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.SttpBackendOptions
import com.softwaremill.sttp.Uri
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.util.b64ToByteArray
import io.newm.server.features.arweave.ktx.toFiles
import io.newm.server.features.arweave.model.WeaveFile
import io.newm.server.features.arweave.model.WeaveProps
import io.newm.server.features.arweave.model.WeaveRequest
import io.newm.server.features.arweave.model.WeaveResponse
import io.newm.server.features.arweave.model.WeaveResponseItem
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.await
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.ktx.toBucketAndKey
import io.newm.shared.coroutine.SupervisorScope
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigBigDecimal
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.info
import io.newm.shared.ktx.warn
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import kotlinx.coroutines.future.await
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import scala.Option
import scala.collection.JavaConverters
import scala.collection.Seq
import scala.compat.java8.FutureConverters.toJava
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Source
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

class ArweaveRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val emailRepository: EmailRepository,
) : ArweaveRepository,
    SupervisorScope {
    override val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val json: Json by inject()
    private val songRepository: SongRepository by inject()
    private val s3Presigner: S3Presigner by inject()
    private val arweaveUri by lazy {
        Uri.apply(environment.getConfigString("arweave.scheme"), environment.getConfigString("arweave.host"))
    }
    private val executorService by lazy { Executors.newScheduledThreadPool(5) }
    private val executionContext by lazy { ExecutionContext.fromExecutorService(executorService) }
    private val arweaveBackend by lazy {
        AsyncHttpClientFutureBackend.apply(
            SttpBackendOptions.Default(),
            executionContext
        )
    }
    private val arweaveConfig by lazy { configApi.apply(arweaveUri, arweaveBackend) }
    private val minWalletBalance: BigDecimal by lazy {
        environment.getConfigBigDecimal("arweave.minWalletBalance")
    }

    private var nextWalletBalanceCheck = 0L

    private suspend fun arweaveWallet(): Wallet {
        val walletJson = environment.getSecureConfigString("arweave.walletJson")
        return walletApi.load(Source.fromString(walletJson)).get()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun calculatePrice(data: Data): Winston {
        val priceFuture =
            priceApi.dataTransaction(
                data,
                arweaveConfig,
                future.futureJsonHandlerEncodedStringHandler(executionContext)
            ) as Future<Winston>

        return toJava(priceFuture).await()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getLastWalletTransaction(): Option<Transaction.Id> {
        val lastTxFuture =
            addressApi.lastTx(
                arweaveWallet().address(),
                arweaveConfig,
                future.futureJsonHandlerEncodedStringHandler(executionContext)
            ) as Future<Option<Transaction.Id>>
        return toJava(lastTxFuture).await()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getWalletARBalance(): BigDecimal {
        val balanceFuture =
            addressApi.balance(
                arweaveWallet().address(),
                arweaveConfig,
                future.futureJsonHandlerEncodedStringHandler(executionContext)
            ) as Future<Winston>
        return toJava(balanceFuture)
            .await()
            .amount()
            .toString()
            .toBigDecimal()
            .movePointLeft(12)
    }

    private fun buildTags(tagMap: Map<String, String>): Option<Seq<Tag.Custom>> =
        Option.apply(
            JavaConverters
                .collectionAsScalaIterableConverter(
                    tagMap.map { (key, value) ->
                        tagApi.apply(key.toByteArray(), value.toByteArray())
                    }
                ).asScala()
                .toSeq()
        )

    private suspend fun signTransaction(transaction: Transaction): Signed<Transaction> = signableApi.SignableSyntax(transaction).sign(arweaveWallet().priv())

    private suspend fun submitTransaction(signedTransaction: Signed<Transaction>): Boolean {
        val submitFuture = txApi.submit(signedTransaction, arweaveConfig, handlerFunction)
        val submitResponse = toJava(submitFuture).await() as Response<*>
        when (submitResponse.code()) {
            200 -> {
                // no-op
            }

            208 -> {
                log.warn { "Transaction already processed: $signedTransaction" }
            }

            429 -> {
                log.error("Too many requests!")
            }

            400, 503 -> {
                log.error("Transaction verification failed!: $signedTransaction")
            }

            else -> {
                log.error("Unknown problem submitting transaction: $submitResponse")
            }
        }
        return submitResponse.isSuccess
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun submitAndAwaitTransactionSettlement(
        bodyBytes: ByteArray,
        mimeType: String
    ): String {
        var signedTransactionPair = createSignedArweaveTransaction(bodyBytes, mimeType)
        var signedTransaction = signedTransactionPair.first
        var transactionId = signedTransactionPair.second
        if (!submitTransaction(signedTransaction)) {
            throw IOException("Failure submitting transaction: $transactionId, $mimeType")
        }
        log.info { "Arweave Transaction submitted: $transactionId, $mimeType" }
        delay(Duration.ofSeconds(10L))

        var txId = Transaction.Id(transactionId.b64ToByteArray())
        var queryCount = 0
        var retryCount = 0
        withTimeout(Duration.ofHours(3L)) {
            while (true) {
                val txFuture = txApi.get(txId, evidenceMonad, arweaveConfig, getTxFunction) as Future<Response<*>>
                val txResponse = toJava(txFuture).await()
                when (txResponse.code()) {
                    200 -> {
                        log.info { "Success, transaction is on chain!: $transactionId, $mimeType" }
                        break // success, stop checking status
                    }

                    202 -> {
                        log.warn { "Pending, transaction is not yet on chain!: $transactionId, $mimeType" }
                    }

                    404 -> {
                        log.warn { "Not Found, transaction could not be found!: $transactionId, $mimeType" }
                        if (queryCount < 3) {
                            queryCount++
                        } else if (retryCount < 3) {
                            queryCount = 0
                            retryCount++
                            // Rebuild and resubmit the tx. Maybe there was a gateway error or something
                            signedTransactionPair = createSignedArweaveTransaction(bodyBytes, mimeType)
                            signedTransaction = signedTransactionPair.first
                            transactionId = signedTransactionPair.second
                            if (!submitTransaction(signedTransaction)) {
                                throw IOException("Failure submitting transaction (retry $retryCount): $transactionId, $mimeType")
                            }
                            log.info { "Arweave Transaction submitted (retry $retryCount): $transactionId, $mimeType" }
                            txId = Transaction.Id(transactionId.b64ToByteArray())
                        } else {
                            throw IOException("Not Found, transaction could not be found!: $transactionId, $mimeType")
                        }
                    }

                    502 -> {
                        log.warn { "Bad Gateway, transaction could not be found!: $transactionId, $mimeType" }
                    }

                    else -> {
                        throw IOException("Error, something bad happened!: $txResponse")
                    }
                }

                delay(Duration.ofMinutes(1L))
            }
        }
        return transactionId
    }

    override suspend fun getWalletAddress(): String = arweaveWallet().address().toString()

    private suspend fun checkWalletBalance() {
        if (Instant.now().toEpochMilli() < nextWalletBalanceCheck) {
            return
        }
        nextWalletBalanceCheck = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
        val balance = getWalletARBalance()
        if (balance < minWalletBalance) {
            log.warn { "Low Wallet Balance: $balance AR" }
            with(environment.getConfigChild("arweave.warningEmail")) {
                emailRepository.send(
                    to = getString("to"),
                    subject = getString("subject"),
                    messageUrl = getString("messageUrl"),
                    messageArgs = mapOf("balance" to balance)
                )
            }
        }
    }

    override suspend fun uploadSongAssets(song: Song) {
        log.info { "Uploading song assets for song: ${song.id}" }
        checkWalletBalance()

        var arweaveException: Throwable? = null

        val release = songRepository.getRelease(song.releaseId!!)

        val newmWeaveRequest =
            WeaveRequest(
                json.encodeToString(
                    WeaveProps(
                        arweaveWalletJson = environment.getSecureConfigString("arweave.walletJson"),
                        files =
                            song.toFiles(release).map { (inputUrl, contentType) ->
                                val downloadUrl =
                                    if (inputUrl.startsWith("s3://")) {
                                        val (bucket, key) = inputUrl.toBucketAndKey()
                                        val getObjectRequest = GetObjectRequest
                                            .builder()
                                            .bucket(bucket)
                                            .key(key)
                                            .build()

                                        val presignGetObjectRequest = GetObjectPresignRequest
                                            .builder()
                                            .signatureDuration(Duration.ofMinutes(30))
                                            .getObjectRequest(getObjectRequest)
                                            .build()

                                        s3Presigner.presignGetObject(presignGetObjectRequest).url().toExternalForm()
                                    } else {
                                        inputUrl
                                    }
                                WeaveFile(
                                    url = downloadUrl,
                                    contentType = contentType,
                                )
                            },
                        checkAndFund = false,
                    )
                )
            )

        val invokeRequest = InvokeRequest
            .builder()
            .functionName(environment.getConfigString("arweave.lambdaFunctionName"))
            .payload(SdkBytes.fromUtf8String(json.encodeToString(newmWeaveRequest)))
            .build()

        val invokeResponse = invokeRequest.await()
        val weaveResponsItems: List<WeaveResponseItem> =
            json.decodeFromString(
                json
                    .decodeFromString<WeaveResponse>(
                        invokeResponse.payload().asUtf8String()
                    ).body
            )

        weaveResponsItems.forEach { weaveResponse ->
            if (weaveResponse.error != null) {
                arweaveException =
                    IllegalStateException("Error uploading to Arweave song: ${song.id}: ${weaveResponse.error}")
            } else {
                val arweaveUrl = "ar://${weaveResponse.id}"
                val toUpdate: Any? =
                    when (weaveResponse.contentType) {
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
                            arweaveException = IllegalStateException("Unknown mime type: ${weaveResponse.contentType}")
                            null
                        }
                    }
                // We're on chain now. Update the song/release record
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
        }

        arweaveException?.let { throw it }
    }

    private suspend fun ArweaveRepositoryImpl.createSignedArweaveTransaction(
        bodyBytes: ByteArray,
        mimeType: String
    ): Pair<Signed<Transaction>, String> {
        val arweaveData = Data(bodyBytes)
        val reward = calculatePrice(arweaveData)
        val lastTx = getLastWalletTransaction()
        val tags = buildTags(mapOf("Content-Type" to mimeType))
        val transaction =
            Transaction.apply(
                lastTx,
                arweaveWallet().owner(),
                reward,
                Option.apply(arweaveData),
                tags,
                Option.empty(),
                Winston.Zero(),
            )
        val signedTransaction = signTransaction(transaction)
        val transactionId = Tx.SignedTransaction(signedTransaction).id().toString()
        return Pair(signedTransaction, transactionId)
    }

    companion object {
        private val handlerFunction: Function1<Future<Any>, Future<Any>> =
            object : Function1<Future<Any>, Future<Any>> {
                override fun invoke(p1: Future<Any>): Future<Any> = p1
            }

        private val evidenceMonad =
            object : Monad<Future<Any>> {
                override fun <A : Any, B : Any> flatMap(
                    fa: Future<Any>,
                    f: scala.Function1<A, Future<Any>>
                ): Future<Any> = fa

                override fun <A : Any, B : Any> tailRecM(
                    a: A,
                    f: scala.Function1<A, Future<Any>>
                ): Future<Any> {
                    // dummy unused function
                    return Future.successful(a)
                }

                override fun <A : Any> pure(x: A): Future<Any> {
                    // dummy unused function
                    return Future.successful(x)
                }
            }

        private val getTxFunction: FunctionK<*, Future<Any>> =
            object : FunctionK<Future<Any>, Future<Any>> {
                override fun <A : Any> apply(fa: Future<Any>): Future<Any> = fa
            }
    }
}
