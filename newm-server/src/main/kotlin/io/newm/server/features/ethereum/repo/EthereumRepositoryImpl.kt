package io.newm.server.features.ethereum.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.ethereum.model.EthereumNft
import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.server.features.ethereum.model.GetNftsByOwnerResponse
import io.newm.server.features.ethereum.parser.parseSong
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.getConfigString

internal class EthereumRepositoryImpl(
    private val client: HttpClient,
    private val environment: ApplicationEnvironment
) : EthereumRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun getNftSongs(ownerAddress: String): List<EthereumNftSong> {
        logger.debug { "getNftSongs: ownerAddress = $ownerAddress" }

        val apiUrl = environment.getConfigString("alchemy.apiUrl")
        val apiKey = environment.getSecureConfigString("alchemy.apiKey")
        return client
            .get("$apiUrl/nft/v3/$apiKey/getNFTsForOwner") {
                retry {
                    maxRetries = 2
                    delayMillis { 500L }
                }
                accept(ContentType.Application.Json)
                parameter("owner", ownerAddress)
            }.checkedBody<GetNftsByOwnerResponse>()
            .ownedNfts
            .mapNotNull(EthereumNft::parseSong)
    }
}
