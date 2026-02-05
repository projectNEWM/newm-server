package io.newm.server.features.ethereum.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.ethereum.model.EthereumNftSong
import io.newm.server.features.ethereum.model.GetNftsByOwnerResponse
import io.newm.server.features.ethereum.parser.parseSong
import io.newm.server.features.walletconnection.database.WalletConnectionEntity
import io.newm.server.features.walletconnection.model.WalletChain
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.typealiases.UserId
import io.newm.shared.ktx.getConfigString
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

internal class EthereumRepositoryImpl(
    private val client: HttpClient,
    private val environment: ApplicationEnvironment
) : EthereumRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun getWalletNftSongs(userId: UserId): List<EthereumNftSong> {
        logger.debug { "getWalletNftSongs: userId = $userId" }

        val connections = newSuspendedTransaction {
            WalletConnectionEntity.getAllByUserIdAndWalletChain(userId, WalletChain.Ethereum).toList()
        }
        val apiUrl = environment.getConfigString("alchemy.apiUrl")
        val apiKey = environment.getSecureConfigString("alchemy.apiKey")
        val endpointUrl = "$apiUrl/nft/v3/$apiKey/getNFTsForOwner"
        return connections
            .map { connection ->
                connection.id.value to client
                    .get(endpointUrl) {
                        retry {
                            maxRetries = 2
                            delayMillis { 500L }
                        }
                        accept(ContentType.Application.Json)
                        parameter("owner", connection.address)
                    }.checkedBody<GetNftsByOwnerResponse>()
                    .ownedNfts
            }.flatMap { (connectionId, nfts) ->
                nfts.map { nft ->
                    Triple(nft.contract.address + nft.tokenId, nft, connectionId to nft.balance)
                }
            }.groupBy { it.first }
            .mapNotNull { (_, group) ->
                val nft = group.first().second
                val allocations = group
                    .map { it.third }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, balances) -> balances.sum() }
                nft.parseSong(allocations)
            }
    }
}
