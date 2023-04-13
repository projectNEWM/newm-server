package io.newm.server.features.minting

import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.SendMessageRequest
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.monitorPaymentAddressRequest
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.ktx.await
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.info
import io.newm.shared.koin.inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.TimeUnit

class MintingMessageReceiver : SqsMessageReceiver {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val songRepository: SongRepository by inject()
    private val cardanoRepository: CardanoRepository by inject()
    private val json: Json by inject()
    private val environment: ApplicationEnvironment by inject()
    private val queueUrl by lazy { environment.getConfigString("aws.sqs.minting.queueUrl") }

    override suspend fun onMessageReceived(message: Message) {
        val mintingStatusSqsMessage: MintingStatusSqsMessage = json.decodeFromString(message.body)
        log.info { "received: $mintingStatusSqsMessage" }

        when (mintingStatusSqsMessage.mintingStatus) {
            MintingStatus.Undistributed -> {
                TODO()
            }

            MintingStatus.StreamTokenAgreementApproved -> {
                TODO()
            }

            MintingStatus.MintingPaymentRequested -> {
                val song = songRepository.get(mintingStatusSqsMessage.songId)
                val paymentKey = cardanoRepository.get(song.paymentKeyId!!)
                val response = cardanoRepository.awaitPayment(
                    monitorPaymentAddressRequest {
                        this.address = paymentKey.address
                        this.lovelace = "6000000" // FIXME: do not hardcode payment amount
                        this.timeoutMs = TimeUnit.HOURS.toMillis(1L) // FIXME: do not hardcode duration to await payment
                    }
                )
                if (response.success) {
                    // We got paid!!! Move -> MintingPaymentReceived
                    updateSongStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.MintingPaymentReceived
                    )
                }
            }

            MintingStatus.MintingPaymentReceived -> {
                // Payment received. Move -> ReadyToDistribute
                updateSongStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.ReadyToDistribute
                )
            }

            MintingStatus.ReadyToDistribute -> {
                TODO()
            }

            MintingStatus.SubmittedForDistribution -> {
                TODO()
            }

            MintingStatus.Distributed -> {
                TODO()
            }

            MintingStatus.Declined -> {
                TODO()
            }

            MintingStatus.Pending -> {
                TODO()
            }

            MintingStatus.Minted -> {
                TODO()
            }
        }
    }

    private suspend fun updateSongStatus(songId: UUID, mintingStatus: MintingStatus) {
        // Update DB
        songRepository.update(
            songId,
            Song(mintingStatus = mintingStatus)
        )

        // Update SQS
        val messageToSend = MintingStatusSqsMessage(
            songId = songId,
            mintingStatus = mintingStatus
        )
        SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody(json.encodeToString(messageToSend))
            .await()
        log.info { "sent: $messageToSend" }
    }
}
