package io.newm.server.features.song

import com.amazonaws.services.sqs.model.Message
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.features.song.model.AudioMessage
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.toUUID
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf

class AudioMessageReceiver : SqsMessageReceiver {
    private val repository: SongRepository by inject()
    private val environment: ApplicationEnvironment by inject()
    private val json: Json by inject()
    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun onMessageReceived(message: Message) {
        val msg: AudioMessage = json.decodeFromString(message.body)
        logger.debug { "Audio ${msg.transcodingType} job status: ${msg.status}" }

        if (msg.status != "COMPLETE") return

        val duration = msg.durationInMs ?: return
        val fullPath = msg.outputFilePath ?: return

        val shortPath =
            fullPath
                .substringAfter("://")
                .substringAfter('/')
                .replace("//", "/")

        val songId =
            shortPath
                .substringBefore('/')
                .toUUID()

        when (msg.transcodingType) {
            "stream" -> {
                val hostUrl = environment.getConfigString("aws.cloudFront.audioStream.hostUrl")
                repository.update(songId, Song(duration = duration, streamUrl = "$hostUrl/$shortPath"))
                repository.processAudioEncoding(songId)
            }

            "clip" -> {
                repository.update(songId, Song(clipUrl = fullPath))
                repository.processAudioEncoding(songId)
            }
        }
    }
}
