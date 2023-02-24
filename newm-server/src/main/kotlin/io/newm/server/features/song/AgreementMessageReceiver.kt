package io.newm.server.features.song

import com.amazonaws.services.sqs.model.Message
import io.ktor.util.logging.*
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.di.inject
import io.newm.server.ext.toUUID
import io.newm.server.features.song.model.AgreementMessage
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.MarkerFactory

class AgreementMessageReceiver : SqsMessageReceiver {
    private val repository: SongRepository by inject()
    private val json: Json by inject()
    private val logger: Logger by inject()
    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override fun onMessageReceived(message: Message) {
        val msg: AgreementMessage = json.decodeFromString(message.body)
        logger.debug(marker, "Agreement job config ID: ${msg.configurationId}")

        if (msg.configurationId != "agreement-created") return
        val key = msg.key ?: return

        val songId = key
            .substringBefore('/')
            .toUUID()

        runBlocking {
            repository.update(songId, Song(mintingStatus = MintingStatus.StreamTokenAgreementApproved))
        }
    }
}
