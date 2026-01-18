package io.newm.ardrive.turbo.payment

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.newm.ardrive.turbo.util.toBase64Url
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.digests.SHA384Digest

class ArweaveTokenTools(
    private val gatewayUrl: String = DEFAULT_GATEWAY_URL,
    private val mintU: Boolean = true,
    private val pollingOptions: PollingOptions = PollingOptions(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
    },
) : TokenTools {
    private val log by lazy { KotlinLogging.logger { } }

    override suspend fun createAndSubmitTx(params: TokenCreateTxParams): TokenCreateTxResult {
        val reward = fetchReward(params.target, params.feeMultiplier)
        val lastTx = fetchTransactionAnchor()
        val owner = toBase64Url(params.signer.publicKeyModulus)
        val tags = buildTags(params.turboCreditDestinationAddress)
        val transaction = ArweaveTransactionRequest(
            format = TRANSACTION_FORMAT,
            id = "",
            lastTx = lastTx,
            owner = owner,
            tags = tags,
            target = params.target,
            quantity = normalizeTokenAmount(params.tokenAmount),
            data = "",
            dataSize = DATA_SIZE_ZERO,
            dataRoot = "",
            reward = reward,
            signature = "",
        )
        val signaturePayload = signaturePayload(transaction)
        val signature = params.signer.sign(signaturePayload)
        val id = toBase64Url(MessageDigest.getInstance("SHA-256").digest(signature))
        val signedTransaction = transaction.copy(
            id = id,
            signature = toBase64Url(signature),
        )
        submitTransaction(signedTransaction)
        return TokenCreateTxResult(
            id = id,
            target = params.target,
            reward = reward,
        )
    }

    override suspend fun pollTxAvailability(txId: String) {
        delay(pollingOptions.initialBackoffMs)
        var attempts = 0
        while (attempts < pollingOptions.maxAttempts) {
            attempts += 1
            val found = runCatching { fetchTransactionById(txId) }.getOrNull()
            if (found == true) {
                log.info { "pollTx($attempts of ${pollingOptions.maxAttempts}) found: $txId" }
                return
            }
            log.info { "pollTx($attempts of ${pollingOptions.maxAttempts}) not found. Wait ${pollingOptions.pollingIntervalMs} ms for: $txId" }
            delay(pollingOptions.pollingIntervalMs)
        }
        throw IllegalStateException("Transaction not found after polling, transaction id: $txId")
    }

    private suspend fun fetchTransactionAnchor(): String {
        val response = getPlainText("$gatewayUrl/tx_anchor")
        if (!response.matches(ANCHOR_REGEX)) {
            throw IllegalStateException("Unexpected tx_anchor response: $response")
        }
        return response
    }

    private suspend fun fetchReward(
        target: String,
        feeMultiplier: Double,
    ): String {
        val priceResponse = getPlainText("$gatewayUrl/price/0/$target")
        val reward = priceResponse.toBigDecimalOrNull()
            ?: throw IllegalStateException("Unexpected price response: $priceResponse")
        if (feeMultiplier == 1.0) {
            return reward.toPlainString()
        }
        return reward
            .multiply(BigDecimal.valueOf(feeMultiplier))
            .setScale(0, RoundingMode.CEILING)
            .toPlainString()
    }

    private fun buildTags(turboCreditDestinationAddress: String?): List<ArweaveTag> {
        val tags = mutableListOf<ArweaveTag>()
        if (mintU) {
            tags += ArweaveTag.fromPlain(APP_NAME_TAG, SMART_WEAVE_APP_NAME)
            tags += ArweaveTag.fromPlain(APP_VERSION_TAG, SMART_WEAVE_APP_VERSION)
            tags += ArweaveTag.fromPlain(CONTRACT_TAG, SMART_WEAVE_CONTRACT)
            tags += ArweaveTag.fromPlain(INPUT_TAG, SMART_WEAVE_INPUT)
        }
        if (!turboCreditDestinationAddress.isNullOrBlank()) {
            tags += ArweaveTag.fromPlain(TURBO_CREDIT_DESTINATION_TAG, turboCreditDestinationAddress)
        }
        return tags
    }

    private suspend fun submitTransaction(transaction: ArweaveTransactionRequest) {
        val payload = json.encodeToString(transaction)
        val response = postJson("$gatewayUrl/tx", payload)
        if (response.status.value != SUCCESS_STATUS) {
            throw IllegalStateException("Failed to post transaction -- status ${response.status.value} ${response.body<String>()}")
        }
    }

    private suspend fun fetchTransactionById(txId: String): Boolean {
        val response = postJson(
            "$gatewayUrl/graphql",
            json.encodeToString(GraphqlRequest(query = graphqlQuery(txId))),
        )
        if (response.status.value != SUCCESS_STATUS) {
            return false
        }
        return json
            .decodeFromString(GraphqlResponse.serializer(), response.body<String>())
            .data
            ?.transactions
            ?.edges
            ?.isNotEmpty() == true
    }

    private fun graphqlQuery(txId: String): String =
        """
        query {
          transactions(ids: ["$txId"]) {
            edges {
              node {
                recipient
                owner { address }
                quantity { winston }
              }
            }
          }
        }
        """.trimIndent()

    private suspend fun getPlainText(url: String): String =
        httpClient
            .get(url) {
                accept(ContentType.Text.Plain)
                timeout {
                    requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                }
            }.body<String>()
            .trim()

    private suspend fun postJson(
        url: String,
        body: String,
    ): HttpResponse =
        httpClient.post(url) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            header("User-Agent", "newm-ardrive-turbo")
            setBody(body)
            timeout {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }

    private fun signaturePayload(transaction: ArweaveTransactionRequest): ByteArray {
        val tagChunks = transaction.tags.map { tag ->
            DeepHashChunk.List(
                listOf(
                    DeepHashChunk.Blob(decodeBase64Url(tag.name)),
                    DeepHashChunk.Blob(decodeBase64Url(tag.value)),
                ),
            )
        }
        val chunks = listOf(
            DeepHashChunk.Blob(transaction.format.toString().toByteArray(StandardCharsets.UTF_8)),
            DeepHashChunk.Blob(decodeBase64Url(transaction.owner)),
            DeepHashChunk.Blob(decodeBase64Url(transaction.target)),
            DeepHashChunk.Blob(transaction.quantity.toByteArray(StandardCharsets.UTF_8)),
            DeepHashChunk.Blob(transaction.reward.toByteArray(StandardCharsets.UTF_8)),
            DeepHashChunk.Blob(decodeBase64Url(transaction.lastTx)),
            DeepHashChunk.List(tagChunks),
            DeepHashChunk.Blob(transaction.dataSize.toByteArray(StandardCharsets.UTF_8)),
            DeepHashChunk.Blob(decodeBase64Url(transaction.dataRoot)),
        )
        return deepHash(DeepHashChunk.List(chunks))
    }

    private fun deepHash(chunk: DeepHashChunk): ByteArray =
        when (chunk) {
            is DeepHashChunk.Blob -> {
                val tag = concatBytes(
                    "blob".toByteArray(StandardCharsets.UTF_8),
                    chunk.bytes.size
                        .toString()
                        .toByteArray(StandardCharsets.UTF_8),
                )
                val taggedHash = concatBytes(
                    sha384(tag),
                    sha384(chunk.bytes),
                )
                sha384(taggedHash)
            }

            is DeepHashChunk.List -> {
                val tag = concatBytes(
                    "list".toByteArray(StandardCharsets.UTF_8),
                    chunk.items.size
                        .toString()
                        .toByteArray(StandardCharsets.UTF_8),
                )
                deepHashChunks(chunk.items, sha384(tag))
            }
        }

    private fun deepHashChunks(
        chunks: List<DeepHashChunk>,
        acc: ByteArray,
    ): ByteArray {
        if (chunks.isEmpty()) {
            return acc
        }
        val headHash = deepHash(chunks.first())
        val combined = concatBytes(acc, headHash)
        val nextAcc = sha384(combined)
        return deepHashChunks(chunks.drop(1), nextAcc)
    }

    private fun sha384(data: ByteArray): ByteArray {
        val digest = SHA384Digest()
        digest.update(data, 0, data.size)
        val output = ByteArray(digest.digestSize)
        digest.doFinal(output, 0)
        return output
    }

    private fun concatBytes(
        first: ByteArray,
        second: ByteArray
    ): ByteArray {
        val combined = ByteArray(first.size + second.size)
        first.copyInto(combined, 0)
        second.copyInto(combined, first.size)
        return combined
    }

    private fun decodeBase64Url(value: String): ByteArray {
        if (value.isBlank()) {
            return ByteArray(0)
        }
        val paddingLength = if (value.length % 4 == 0) 0 else 4 - (value.length % 4)
        val normalized = value
            .replace('-', '+')
            .replace('_', '/')
            .plus("=".repeat(paddingLength))
        return Base64.getDecoder().decode(normalized)
    }

    private fun normalizeTokenAmount(tokenAmount: String): String {
        val amount = tokenAmount.toBigDecimalOrNull() ?: return tokenAmount
        return amount
            .movePointRight(TOKEN_AMOUNT_DECIMALS)
            .setScale(0, RoundingMode.CEILING)
            .toPlainString()
    }

    @Serializable
    private data class GraphqlRequest(
        val query: String,
    )

    @Serializable
    private data class GraphqlResponse(
        val data: GraphqlData? = null,
    )

    @Serializable
    private data class GraphqlData(
        val transactions: GraphqlTransactions? = null,
    )

    @Serializable
    private data class GraphqlTransactions(
        val edges: List<GraphqlEdge> = emptyList(),
    )

    @Serializable
    private data class GraphqlEdge(
        val node: GraphqlNode? = null,
    )

    @Serializable
    private data class GraphqlNode(
        val recipient: String? = null,
    )

    private sealed class DeepHashChunk {
        data class Blob(
            val bytes: ByteArray
        ) : DeepHashChunk()

        data class List(
            val items: kotlin.collections.List<DeepHashChunk>
        ) : DeepHashChunk()
    }

    private companion object {
        const val DEFAULT_GATEWAY_URL = "https://arweave.net"
        const val TRANSACTION_FORMAT = 2
        const val DATA_SIZE_ZERO = "0"
        const val SUCCESS_STATUS = 200
        const val REQUEST_TIMEOUT_MILLIS = 20_000L
        val ANCHOR_REGEX = Regex("^[A-Za-z0-9_-]{43,}")
        const val TOKEN_AMOUNT_DECIMALS = 12

        const val APP_NAME_TAG = "App-Name"
        const val APP_VERSION_TAG = "App-Version"
        const val CONTRACT_TAG = "Contract"
        const val INPUT_TAG = "Input"
        const val SMART_WEAVE_APP_NAME = "SmartWeaveAction"
        const val SMART_WEAVE_APP_VERSION = "0.3.0"
        const val SMART_WEAVE_CONTRACT = "KTzTXT_ANmF84fWEKHzWURD1LWd9QaFR9yfYUwH2Lxw"
        const val SMART_WEAVE_INPUT = "{\"function\":\"mint\"}"
        const val TURBO_CREDIT_DESTINATION_TAG = "Turbo-Credit-Destination-Address"
    }
}

@Serializable
internal data class ArweaveTag(
    val name: String,
    val value: String,
) {
    companion object {
        fun fromPlain(
            name: String,
            value: String,
        ): ArweaveTag =
            ArweaveTag(
                name = toBase64Url(name.toByteArray(StandardCharsets.UTF_8)),
                value = toBase64Url(value.toByteArray(StandardCharsets.UTF_8)),
            )
    }
}

@Serializable
internal data class ArweaveTransactionRequest(
    val format: Int,
    val id: String,
    @SerialName("last_tx")
    val lastTx: String,
    val owner: String,
    val tags: List<ArweaveTag>,
    val target: String,
    val quantity: String,
    val data: String,
    @SerialName("data_size")
    val dataSize: String,
    @SerialName("data_root")
    val dataRoot: String,
    val reward: String,
    val signature: String,
)

@Serializable
data class PollingOptions(
    val maxAttempts: Int = 10,
    val pollingIntervalMs: Long = 3_000,
    val initialBackoffMs: Long = 7_000,
)
