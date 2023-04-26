package io.newm.server.features.minting

import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.SendMessageRequest
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.monitorPaymentAddressRequest
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.arweave.repo.ArweaveRepository
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.await
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.info
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.util.UUID
import java.util.concurrent.TimeUnit

class MintingMessageReceiver : SqsMessageReceiver {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val songRepository: SongRepository by inject()
    private val cardanoRepository: CardanoRepository by inject()
    private val arweaveRepository: ArweaveRepository by inject()
    private val mintingRepository: MintingRepository by inject()
    private val configRepository: ConfigRepository by inject()
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
                val paymentKey = cardanoRepository.getKey(song.paymentKeyId!!)
                val response = cardanoRepository.awaitPayment(
                    monitorPaymentAddressRequest {
                        this.address = paymentKey.address
                        this.lovelace = configRepository.getString("mint.price")
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
                    mintingStatus = MintingStatus.AwaitingCollaboratorApproval
                )
            }

            MintingStatus.AwaitingCollaboratorApproval -> {
                // TODO: Check to see if all collaborators have approved. Also check this each time we get a new approval in

                // Payment received. Move -> ReadyToDistribute
                updateSongStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.AwaitingCollaboratorApproval
                )
            }

            MintingStatus.ReadyToDistribute -> {
                songRepository.distribute(mintingStatusSqsMessage.songId)

                // Done submitting distributing. Move -> SubmittedForDistribution
                updateSongStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.SubmittedForDistribution
                )
            }

            MintingStatus.SubmittedForDistribution -> {
                // TODO: Add some monitoring job that will poll the distribution service for when the distribution
                // is done. Let that monitoring job move it to either Distributed or Declined status
            }

            MintingStatus.Distributed -> {
                // Upload 30-second clip, lyrics.txt, streamtokenagreement.pdf, coverArt to arweave and save those URLs
                // on the Song record
                val song = songRepository.get(mintingStatusSqsMessage.songId)
                arweaveRepository.uploadSongAssets(song)

                // Done with arweave. Move -> Pending for minting
                updateSongStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.Pending
                )
            }

            MintingStatus.Declined -> {
                // TODO: Notify the user and our support team via email that the distribution was declined.
                // We'll have to change the minting status manually to re-process things if necessary.
            }

            MintingStatus.Pending -> {
                // Create and submit the mint transaction
                val song = songRepository.get(mintingStatusSqsMessage.songId)
                mintingRepository.mint(song)

                // Done submitting mint transaction. Move -> Minted
                updateSongStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.Minted
                )
            }

            MintingStatus.Minted -> {
                // TODO: Notify the user that the process has been completed
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
