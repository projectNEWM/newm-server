package io.newm.server.features.arweave.repo

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import io.ktor.server.application.*
import io.newm.server.features.arweave.model.WeaveFile
import io.newm.server.features.arweave.model.WeaveProps
import io.newm.server.features.arweave.model.WeaveRequest
import io.newm.server.features.song.model.Song
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.ktx.toBucketAndKey
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

object Util {
    val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val environment: ApplicationEnvironment by inject()
    private val json: Json by inject()
    private val amazonS3: AmazonS3 by inject()
    private val IMAGE_WEBP_REPLACE_REGEX = Regex("\\.(png|jpg|jpeg|bmp|gif|tiff)\$", RegexOption.IGNORE_CASE)

    suspend fun weaveRequest(song: Song) =
        WeaveRequest(
            json.encodeToString(
                WeaveProps(
                    arweaveWalletJson = environment.getSecureConfigString("arweave.walletJson"),
                    files =
                        listOfNotNull(
                            if (song.arweaveCoverArtUrl != null) {
                                log.info { "Song ${song.id} already has arweave cover art url: ${song.arweaveCoverArtUrl}" }
                                null
                            } else {
                                song.coverArtUrl.asValidUrl().replace(IMAGE_WEBP_REPLACE_REGEX, ".webp") to "image/webp"
                            },
                            if (song.arweaveTokenAgreementUrl != null) {
                                log.info { "Song ${song.id} already has arweave token agreement url: ${song.arweaveTokenAgreementUrl}" }
                                null
                            } else {
                                song.tokenAgreementUrl!! to "application/pdf"
                            },
                            if (song.arweaveClipUrl != null) {
                                log.info { "Song ${song.id} already has arweave clip url: ${song.arweaveClipUrl}" }
                                null
                            } else {
                                song.clipUrl!! to "audio/mpeg"
                            },
                            if (song.arweaveLyricsUrl != null) {
                                log.info { "Song ${song.id} already has arweave lyrics url: ${song.arweaveLyricsUrl}" }
                                null
                            } else {
                                song.lyricsUrl?.let { it to "text/plain" }
                            },
                        ).map { (inputUrl, contentType) ->
                            val downloadUrl =
                                if (inputUrl.startsWith("s3://")) {
                                    val (bucket, key) = inputUrl.toBucketAndKey()
                                    amazonS3.generatePresignedUrl(
                                        bucket,
                                        key,
                                        Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)),
                                        HttpMethod.GET
                                    ).toExternalForm()
                                } else {
                                    inputUrl
                                }
                            WeaveFile(
                                url = downloadUrl,
                                contentType = contentType,
                            )
                        },
                    checkAndFund = false,
                )
            )
        )
}
