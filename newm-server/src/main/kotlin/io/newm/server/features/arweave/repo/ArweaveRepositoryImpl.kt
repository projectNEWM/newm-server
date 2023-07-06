package io.newm.server.features.arweave.repo

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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.util.b64ToByteArray
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.coroutine.SupervisorScope
import io.newm.shared.exception.HttpStatusException.Companion.toException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import scala.Option
import scala.collection.JavaConverters
import scala.collection.Seq
import scala.compat.java8.FutureConverters.toJava
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Source
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.Executors
import co.upvest.arweave4s.adt.`Signable$`.`MODULE$` as signableApi
import co.upvest.arweave4s.adt.Tag.`Custom$`.`MODULE$` as tagApi
import co.upvest.arweave4s.adt.`Transaction$`.`MODULE$` as Tx
import co.upvest.arweave4s.adt.`Wallet$`.`MODULE$` as walletApi
import co.upvest.arweave4s.api.`address$`.`MODULE$` as addressApi
import co.upvest.arweave4s.api.`package`.`Config$`.`MODULE$` as configApi
import co.upvest.arweave4s.api.`package`.`future$`.`MODULE$` as future
import co.upvest.arweave4s.api.`price$`.`MODULE$` as priceApi
import co.upvest.arweave4s.api.`tx$`.`MODULE$` as txApi

class ArweaveRepositoryImpl(
    private val environment: ApplicationEnvironment,
) : ArweaveRepository, SupervisorScope {
    override val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val songRepository: SongRepository by inject()
    private val httpClient: HttpClient by inject()
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

    private suspend fun arweaveWallet(): Wallet {
        val walletJson = environment.getSecureConfigString("arweave.walletJson")
        return walletApi.load(Source.fromString(walletJson)).get()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun calculatePrice(data: Data): Winston {
        val priceFuture = priceApi.dataTransaction(
            data,
            arweaveConfig,
            future.futureJsonHandlerEncodedStringHandler(executionContext)
        ) as Future<Winston>

        return toJava(priceFuture).await()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun getLastWalletTransaction(): Option<Transaction.Id> {
        val lastTxFuture = addressApi.lastTx(
            arweaveWallet().address(),
            arweaveConfig,
            future.futureJsonHandlerEncodedStringHandler(executionContext)
        ) as Future<Option<Transaction.Id>>
        return toJava(lastTxFuture).await()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getWalletARBalance(): BigDecimal {
        val balanceFuture = addressApi.balance(
            arweaveWallet().address(),
            arweaveConfig,
            future.futureJsonHandlerEncodedStringHandler(executionContext)
        ) as Future<Winston>
        return toJava(balanceFuture).await().amount().toString().toBigDecimal().movePointLeft(12)
    }

    private fun buildTags(tagMap: Map<String, String>): Option<Seq<Tag.Custom>> {
        return Option.apply(
            JavaConverters.collectionAsScalaIterableConverter(
                tagMap.map { (key, value) ->
                    tagApi.apply(key.toByteArray(), value.toByteArray())
                }
            ).asScala().toSeq()
        )
    }

    private suspend fun signTransaction(transaction: Transaction): Signed<Transaction> {
        return signableApi.SignableSyntax(transaction).sign(arweaveWallet().priv())
    }

    private suspend fun submitTransaction(signedTransaction: Signed<Transaction>): Boolean {
        val submitFuture = txApi.submit(signedTransaction, arweaveConfig, handlerFunction)
        val submitResponse = toJava(submitFuture).await() as Response<*>
        when (submitResponse.code()) {
            200 -> {
                // no-op
            }

            208 -> log.warn("Transaction already processed: $signedTransaction")
            429 -> log.error("Too many requests!")
            400, 503 -> log.error("Transaction verification failed!: $signedTransaction")
            else -> log.error("Unknown problem submitting transaction: $submitResponse")
        }
        return submitResponse.isSuccess
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun awaitTransactionSettlement(transactionId: String) {
        val txId = Transaction.Id(transactionId.b64ToByteArray())
        withTimeout(Duration.ofHours(3L)) {
            while (true) {
                val txFuture = txApi.get(txId, evidenceMonad, arweaveConfig, getTxFunction) as Future<Response<*>>
                val txResponse = toJava(txFuture).await()
                when (txResponse.code()) {
                    200 -> {
                        log.info("Success, transaction is on chain!: $transactionId")
                        break // success, stop checking status
                    }

                    202 -> log.warn("Pending, transaction is not yet on chain!: $transactionId")
                    404 -> {
                        throw IOException("Not Found, transaction could not be found!: $transactionId")
                    }

                    else -> {
                        throw IOException("Error, something bad happened!: $txResponse")
                    }
                }

                delay(Duration.ofMinutes(1L))
            }
        }
    }

    override suspend fun getWalletAddress(): String {
        return arweaveWallet().address().toString()
    }

    override suspend fun uploadSongAssets(song: Song) {
        // TODO: CU-863h3zukj - Check the wallet balance and send admin warning email if below 0.5 AR
        val songUpdateMutex = Mutex()
        listOfNotNull(
            song.arweaveCoverArtUrl?.let { null } ?: (
                song.coverArtUrl.asValidUrl().replace(IMAGE_WEBP_REPLACE_REGEX, ".webp") to "image/webp"
                ),
            song.arweaveTokenAgreementUrl?.let { null } ?: (song.tokenAgreementUrl.asValidUrl() to "application/pdf"),
            song.arweaveClipUrl?.let { null } ?: (song.clipUrl.asValidUrl() to "audio/mpeg"),
            song.arweaveLyricsUrl?.let { null } ?: (song.lyricsUrl?.let { it.asValidUrl() to "text/plain" }),
        ).map { (url, mimeType) ->
            async {
                try {
                    val response = httpClient.get(url) {
                        accept(ContentType.parse(mimeType))
                    }

                    if (!response.status.isSuccess()) {
                        throw response.status.toException("Error downloading url: $url")
                    }

                    val bodyBytes: ByteArray = response.body()
                    val arweaveData = Data(bodyBytes)
                    val reward = calculatePrice(arweaveData)
                    val lastTx = getLastWalletTransaction()
                    val tags = buildTags(mapOf("Content-Type" to mimeType))
                    val transaction = Transaction.apply(
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
                    if (!submitTransaction(signedTransaction)) {
                        throw IOException("Failure submitting transaction: $transactionId")
                    }

                    awaitTransactionSettlement(transactionId)

                    songUpdateMutex.withLock {
                        val songToUpdate = when (mimeType) {
                            "image/webp" -> Song(arweaveCoverArtUrl = "ar://$transactionId")
                            "application/pdf" -> Song(arweaveTokenAgreementUrl = "ar://$transactionId")
                            "audio/mpeg" -> Song(arweaveClipUrl = "ar://$transactionId")
                            "text/plain" -> Song(arweaveLyricsUrl = "ar://$transactionId")
                            else -> throw IllegalStateException()
                        }

                        // We're on chain now. Update the song record
                        songRepository.update(song.id!!, songToUpdate)
                    }
                } catch (e: Throwable) {
                    // just log and swallow all exceptions so others can potentially succeed
                    log.error("Arweave Process Error", e)
                }
            }
        }.awaitAll()
    }

    companion object {
        private val IMAGE_WEBP_REPLACE_REGEX = Regex("\\.(png|jpg|jpeg|bmp|gif|tiff)\$", RegexOption.IGNORE_CASE)

        private val handlerFunction: Function1<Future<Any>, Future<Any>> =
            object : Function1<Future<Any>, Future<Any>> {
                override fun invoke(p1: Future<Any>): Future<Any> {
                    return p1
                }
            }

        private val evidenceMonad = object : Monad<Future<Any>> {
            override fun <A : Any, B : Any> flatMap(
                fa: Future<Any>,
                f: scala.Function1<A, Future<Any>>
            ): Future<Any> {
                return fa
            }

            override fun <A : Any, B : Any> tailRecM(a: A, f: scala.Function1<A, Future<Any>>): Future<Any> {
                // dummy unused function
                return Future.successful(a)
            }

            override fun <A : Any> pure(x: A): Future<Any> {
                // dummy unused function
                return Future.successful(x)
            }
        }

        private val getTxFunction: FunctionK<*, Future<Any>> = object : FunctionK<Future<Any>, Future<Any>> {
            override fun <A : Any> apply(fa: Future<Any>): Future<Any> {
                return fa
            }
        }
    }
}
