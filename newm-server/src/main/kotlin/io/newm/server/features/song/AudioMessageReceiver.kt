package io.newm.server.features.song

import com.amazonaws.services.sqs.model.Message
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.di.inject
import io.newm.server.ext.getConfigString
import io.newm.server.ext.toUUID
import io.newm.server.features.song.model.AudioMessage
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.MarkerFactory

class AudioMessageReceiver : SqsMessageReceiver {
    private val repository: SongRepository by inject()
    private val environment: ApplicationEnvironment by inject()
    private val json: Json by inject()
    private val logger: Logger by inject()
    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override fun onMessageReceived(message: Message) {
        val msg: AudioMessage = json.decodeFromString(message.body)
        logger.debug(marker, "Audio job status: ${msg.status}")

        if (msg.status != "COMPLETE") return

        val duration = msg.durationInMs ?: return
        val fullPath = msg.outputFilePath ?: return

        val shortPath = fullPath
            .substringAfter("://")
            .substringAfter('/')
            .replace("//", "/")

        val songId = shortPath
            .substringBefore('/')
            .toUUID()

        val hostUrl = environment.getConfigString("aws.cloudFront.audioStream.hostUrl")
        val streamUrl = "$hostUrl/$shortPath"

        runBlocking {
            repository.update(songId, Song(duration = duration, streamUrl = streamUrl))
        }
    }
}
