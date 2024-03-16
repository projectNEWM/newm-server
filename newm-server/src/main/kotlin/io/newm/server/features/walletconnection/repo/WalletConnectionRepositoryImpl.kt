package io.newm.server.features.walletconnection.repo

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.core.toByteArray
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.chain.util.toHexString
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.walletconnection.database.WalletConnectionChallengeEntity
import io.newm.server.features.walletconnection.database.WalletConnectionEntity
import io.newm.server.features.walletconnection.model.AnswerChallengeRequest
import io.newm.server.features.walletconnection.model.AnswerChallengeResponse
import io.newm.server.features.walletconnection.model.GenerateChallengeRequest
import io.newm.server.features.walletconnection.model.GenerateChallengeResponse
import io.newm.server.features.walletconnection.model.ChallengeMethod
import io.newm.server.features.walletconnection.model.ConnectResponse
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpNotFoundException
import io.newm.shared.exception.HttpUnprocessableEntityException
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
    private val client: NewmChainCoroutineStub
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

        val challengeId = UUID.randomUUID()
        val payload =
            when (request.method) {
                ChallengeMethod.SignedData -> {
                    """{ "connectTo": "NEWM Mobile $challengeId" }""".toByteArray().toHexString()
                }

                ChallengeMethod.SignedTransaction -> {
                    // TODO: call newm-chain, passing request.utxos to generate challenge transaction and
                    // then encode returned transaction as cbor hex string
                    throw HttpUnprocessableEntityException("SignedTransaction method not supported yet!!!")
                }
            }

        val entity =
            transaction {
                WalletConnectionChallengeEntity.new(challengeId) {
                    method = request.method
                    stakeAddress = request.stakeAddress
                    this.payload = payload
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

        val challenge =
            transaction {
                WalletConnectionChallengeEntity.deleteAllExpired(challengeTimeToLive)
                WalletConnectionChallengeEntity[request.challengeId]
            }

        when (challenge.method) {
            ChallengeMethod.SignedData -> {
                // TODO: call newm-chain to verify signed data, if verification fails throw HttpForbiddenException
            }

            ChallengeMethod.SignedTransaction -> {
                // TODO: call newm-chain to verify signed transaction, if verification fails throw HttpForbiddenException
            }
        }

        // we're ready to connect!!!
        val connection =
            transaction {
                WalletConnectionEntity.new {
                    stakeAddress = challenge.stakeAddress
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
        userId: UUID
    ): ConnectResponse {
        logger.debug { "connect: connectionId = $connectionId, userId = $userId" }
        val connection =
            transaction {
                WalletConnectionEntity.deleteAllExpired(connectionTimeToLive)
                WalletConnectionEntity[connectionId].apply {
                    if (this.userId != null) throw HttpForbiddenException("Already connected")
                    this.userId = EntityID(userId, UserTable)
                }
            }
        return ConnectResponse(
            connectionId = connectionId,
            stakeAddress = connection.stakeAddress
        )
    }

    override suspend fun disconnect(
        connectionId: UUID,
        userId: UUID
    ) {
        logger.debug { "disconnect: connectionId = $connectionId, userId = $userId" }
        transaction {
            with(WalletConnectionEntity[connectionId]) {
                if (this.userId?.value != userId) throw HttpForbiddenException("User doesn't own connection")
                delete()
            }
        }
    }
}
