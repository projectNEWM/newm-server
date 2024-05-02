package io.newm.server.features.walletconnection.repo

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborTextString
import com.google.protobuf.kotlin.toByteString
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.core.toByteArray
import io.newm.chain.util.Bech32
import io.newm.chain.util.Constants
import io.newm.chain.util.toHexString
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeEntity
import io.newm.server.features.walletconnection.database.WalletConnectionEntity
import io.newm.server.features.walletconnection.model.AnswerChallengeRequest
import io.newm.server.features.walletconnection.model.AnswerChallengeResponse
import io.newm.server.features.walletconnection.model.GenerateChallengeRequest
import io.newm.server.features.walletconnection.model.GenerateChallengeResponse
import io.newm.server.features.walletconnection.model.ChallengeMethod
import io.newm.server.features.walletconnection.model.WalletConnection
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpNotFoundException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.getConfigLong
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import qrcode.QRCode
import qrcode.color.Colors
import java.util.UUID

internal class WalletConnectionRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val cardanoRepository: CardanoRepository,
) : WalletConnectionRepository {
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }
    private val challengeTimeToLive: Long by lazy {
        environment.getConfigLong("walletConnection.challengeTimeToLive")
    }
    private val connectionTimeToLive: Long by lazy {
        environment.getConfigLong("walletConnection.connectionTimeToLive")
    }
    private val logoBytes: ByteArray by lazy {
        javaClass.getResourceAsStream("/images/qrcode-logo.png")!!.readAllBytes()
    }

    override suspend fun generateChallenge(request: GenerateChallengeRequest): GenerateChallengeResponse {
        logger.debug { "generateChallenge: $request" }

        requireNotNull(Constants.stakeAddressRegex.matchEntire(request.stakeAddress)) {
            "Invalid stake address: ${request.stakeAddress}"
        }
        val challengeId = UUID.randomUUID()
        val challengeString = buildChallengeString(challengeId, request.stakeAddress)
        val payload =
            when (request.method) {
                ChallengeMethod.SignedData -> {
                    challengeString.toByteArray().toHexString()
                }

                ChallengeMethod.SignedTransaction -> {
                    requireNotNull(request.utxoCborHexList) { "Missing utxoCborHexList" }
                    requireNotNull(request.changeAddress) { "Missing changeAddress" }
                    val transactionBuilderResponse =
                        cardanoRepository.buildTransaction {
                            // add input utxos
                            sourceUtxos.addAll(request.utxos)

                            // change address
                            changeAddress = request.changeAddress

                            // ensures this tx is expired because of 1 time to live
                            ttlAbsoluteSlot = 1

                            // require the stake key to sign the transaction
                            requiredSigners.add(
                                Bech32.decode(request.stakeAddress).bytes.copyOfRange(1, 29).toByteString()
                            )

                            // put the challenge into the transaction metadata so it is displayed in the wallet
                            transactionMetadataCbor =
                                CborMap.create(
                                    mapOf(
                                        CborInteger.create(674) to
                                            CborMap.create(
                                                mapOf(
                                                    CborTextString.create("msg") to
                                                        CborArray.create().apply {
                                                            challengeString.chunked(64).forEach {
                                                                add(CborTextString.create(it))
                                                            }
                                                        }
                                                )
                                            )
                                    )
                                ).toCborByteArray().toByteString()
                        }

                    require(!transactionBuilderResponse.hasErrorMessage()) {
                        "Failed to build challenge transaction: ${transactionBuilderResponse.errorMessage}"
                    }
                    logger.debug {
                        "Generated challenge transactionId: ${transactionBuilderResponse.transactionId}, cbor: ${
                            transactionBuilderResponse.transactionCbor.toByteArray().toHexString()
                        }"
                    }
                    transactionBuilderResponse.transactionCbor.toByteArray().toHexString()
                }
            }

        val entity =
            transaction {
                WalletConnectionChallengeEntity.new(challengeId) {
                    method = request.method
                    stakeAddress = request.stakeAddress
                }
            }

        return GenerateChallengeResponse(
            challengeId = challengeId,
            expiresAt = entity.createdAt.plusSeconds(challengeTimeToLive),
            payload = payload
        )
    }

    override suspend fun answerChallenge(request: AnswerChallengeRequest): AnswerChallengeResponse {
        logger.debug { "answerChallenge: $request" }

        val (challengeId, method, stakeAddress) =
            transaction {
                WalletConnectionChallengeEntity.deleteAllExpired(challengeTimeToLive)
                WalletConnectionChallengeEntity[request.challengeId].run {
                    Triple(id.value, method, stakeAddress).also { delete() }
                }
            }

        val response =
            when (method) {
                ChallengeMethod.SignedData -> {
                    requireNotNull(request.key) { "Missing key" }
                    cardanoRepository.verifySignData(request.payload, request.key)
                }

                ChallengeMethod.SignedTransaction -> {
                    cardanoRepository.verifySignTransaction(request.payload)
                }
            }

        if (!response.verified || response.challenge != buildChallengeString(challengeId, stakeAddress)) {
            throw HttpForbiddenException("Challenge signature verification failed")
        }

        // we're ready to connect!!!
        val connection =
            transaction {
                WalletConnectionEntity.new {
                    this.stakeAddress = stakeAddress
                }
            }
        return AnswerChallengeResponse(
            connectionId = connection.id.value,
            expiresAt = connection.createdAt.plusSeconds(connectionTimeToLive)
        )
    }

    override suspend fun generateQRCode(connectionId: UUID): ByteArray {
        logger.debug { "generateQRCode: $connectionId" }
        if (transaction { !WalletConnectionEntity.existsHavingId(connectionId) }) {
            throw HttpNotFoundException("Wallet connection doesn't exist: $connectionId")
        }
        return QRCode.ofSquares()
            .withInnerSpacing(0)
            .withColor(Colors.BLACK)
            .withBackgroundColor(Colors.TRANSPARENT)
            .withLogo(logoBytes, 256, 256, clearLogoArea = false)
            .build("newm-$connectionId")
            .renderToBytes()
    }

    override suspend fun connect(
        connectionId: UUID,
        userId: UserId
    ): WalletConnection {
        logger.debug { "connect: connectionId = $connectionId, userId = $userId" }
        return transaction {
            WalletConnectionEntity.deleteAllExpired(connectionTimeToLive)
            WalletConnectionEntity[connectionId].also {
                if (it.userId != null) throw HttpForbiddenException("Already connected to another User")
                it.userId = EntityID(userId, UserTable)
                WalletConnectionEntity.deleteAllDuplicates(it)
            }
        }.toModel()
    }

    override suspend fun disconnect(
        connectionId: UUID,
        userId: UserId
    ) {
        logger.debug { "disconnect: connectionId = $connectionId, userId = $userId" }
        transaction {
            with(WalletConnectionEntity[connectionId]) {
                if (this.userId?.value != userId) throw HttpForbiddenException("User doesn't own connection")
                delete()
            }
        }
    }

    override suspend fun getUserConnections(userId: UserId): List<WalletConnection> {
        logger.debug { "getUserConnections: userId = $userId" }
        return transaction {
            WalletConnectionEntity.getAllByUserId(userId).map(WalletConnectionEntity::toModel)
        }
    }

    private fun buildChallengeString(
        challengeId: UUID,
        stakeAddress: String
    ): String = """{"connectTo":"NEWM Mobile $challengeId","stakeAddress":"$stakeAddress"}"""
}
