package io.newm.server.features.walletconnection.repo

import io.newm.server.features.walletconnection.model.AnswerChallengeRequest
import io.newm.server.features.walletconnection.model.AnswerChallengeResponse
import io.newm.server.features.walletconnection.model.WalletConnection
import io.newm.server.features.walletconnection.model.GenerateChallengeRequest
import io.newm.server.features.walletconnection.model.GenerateChallengeResponse
import java.util.UUID

interface WalletConnectionRepository {
    suspend fun generateChallenge(request: GenerateChallengeRequest): GenerateChallengeResponse

    suspend fun answerChallenge(request: AnswerChallengeRequest): AnswerChallengeResponse

    suspend fun generateQRCode(connectionId: UUID): ByteArray

    suspend fun connect(
        connectionId: UUID,
        userId: UUID
    ): WalletConnection

    suspend fun disconnect(
        connectionId: UUID,
        userId: UUID
    )

    suspend fun getUserConnections(userId: UUID): List<WalletConnection>
}
