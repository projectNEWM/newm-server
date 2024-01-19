package io.newm.chain.cardano

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.google.common.truth.Truth.assertThat
import io.newm.chain.config.Config
import io.newm.chain.database.entity.LedgerAsset
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.NativeAsset
import io.newm.chain.util.extractAssetMetadata
import io.newm.chain.util.toCreatedUtxoSet
import io.newm.kogmios.Client
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.createChainSyncClient
import io.newm.kogmios.protocols.model.GenesisEra
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.kogmios.protocols.model.PointDetail
import io.newm.kogmios.protocols.model.result.RollBackward
import io.newm.kogmios.protocols.model.result.RollForward
import io.newm.kogmios.protocols.model.result.ShelleyGenesisConfigResult
import io.newm.txbuilder.ktx.cborHexToPlutusData
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.IOException
import java.math.BigInteger

class Cip68DatumTest {

    @Test
    fun `test CIP68 datum`() {
        val createdUtxoSet = setOf(
            CreatedUtxo(
                address = "addr_test1xqyeshaz2w77449uepcap6p8pud9ju3rjckddxdv9qxf787m46qvthcky2x42khv4jyuen72ksjtqrapzw28jdy3vtdqnkrmz5",
                addressType = "00",
                stakeAddress = "stake_test17rd6aqx9mutz9r24ttk2ezwvel9tgf9sp7s389rexjgk9kssedugy",
                hash = "5916ad353cadca8cef2951bbdd47500325fb9d8d149237327f17edda8d283650",
                ix = 0L,
                lovelace = BigInteger("1706760"),
                datumHash = null,
                datum = "d8799fa3446e616d6548537061636542756445696d6167654b697066733a2f2f7465737445696d616765583061723a2f2f66355738525a6d4151696d757a5f7679744659396f66497a6439517047614449763255587272616854753401ff",
                scriptRef = null,
                nativeAssets = listOf(
                    NativeAsset(
                        policy = "169e3ad7038f23049f3ed7fff137b1b29bccaba4732035477c15b4c9",
                        name = "000643b00211c612bdc6ab89e8797f796fb3d83232b70139a484b25e022ab2",
                        amount = BigInteger.ONE
                    )
                ),
                cbor = null,
                paymentCred = null,
                stakeCred = null,
            )
        )

        val result = cip68UtxoOutputsTo721MetadataMap(createdUtxoSet)
        val ledgerAssetMetadata = result[0].first.extractAssetMetadata(result[0].second)
        assertThat(ledgerAssetMetadata).isNotEmpty()
    }

    @Test
    @Disabled("This test requires a running ogmios instance")
    fun testDerpBirdsOutpostsCIP68() = runBlocking {
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.level = Level.INFO

        createChainSyncClient(
            websocketHost = "localhost",
            websocketPort = 1337,
            secure = false,
            ogmiosCompact = false,
        ).use { client ->
            val connectResult = client.connect()
            if (!connectResult) {
                throw IOException("client.connect() was false!")
            }
            if (!client.isConnected) {
                throw IOException("client.isConnected was false!")
            }
            (client as StateQueryClient).genesisConfig(GenesisEra.SHELLEY).let { genesisConfig ->
                Config.genesis = genesisConfig.result as ShelleyGenesisConfigResult
            }

            val msgFindIntersectResponse = client.findIntersect(
                listOf(
                    PointDetail(
                        105659354L,
                        "7ebe1503ca9119343d06e56fd0048e0b0cd1873dbaaaf7d5039235558808219c"
                    )
                )
            )

            var shouldContinue = true
            while (shouldContinue) {
                val response = client.nextBlock(timeoutMs = Client.DEFAULT_REQUEST_TIMEOUT_MS)
                when (response.result) {
                    is RollBackward -> {
                        println("RollBackward: ${(response.result as RollBackward).point}")
                    }

                    is RollForward -> {
                        val rollForward = response.result as RollForward
                        val block = rollForward.block
                        val createdUtxos = block.toCreatedUtxoSet()
                        require(createdUtxos.isNotEmpty()) { "createdUtxos is empty!" }
                        // Save metadata for CIP-68 reference metadata appearing on createdUtxos datum values
                        val nativeAssetMetadataList = cip68UtxoOutputsTo721MetadataMap(createdUtxos)
                        require(nativeAssetMetadataList.isNotEmpty()) { "nativeAssetMetadataList is empty!" }
                        nativeAssetMetadataList.forEach { (metadataMap, assetList) ->
                            try {
                                val assetMetadatas = metadataMap.extractAssetMetadata(assetList)
                                println(assetMetadatas)
                            } catch (e: Throwable) {
                                println("metadataError at block ${block.height}, metadataMap: $metadataMap, assetList: $assetList")
                                throw e
                            }
                        }
                        shouldContinue = false
                    }
                }
            }
        }
    }

    private fun cip68UtxoOutputsTo721MetadataMap(createdUtxos: Set<CreatedUtxo>): List<Pair<MetadataMap, List<LedgerAsset>>> {
        var i = 1L
        return createdUtxos.filter { createdUtxo ->
            createdUtxo.nativeAssets.any { nativeAsset ->
                nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX).also {
                    if (it) {
                        println("found: ${nativeAsset.policy}.${nativeAsset.name}")
                    }
                }
            }
        }.mapNotNull { cip68CreatedUtxo ->
            println("cip68CreatedUtxo: $cip68CreatedUtxo")
            cip68CreatedUtxo.datum?.let { datum ->
                val cip68PlutusData = datum.cborHexToPlutusData()
                if (cip68PlutusData.hasConstr() && cip68PlutusData.constr == 0) {
                    cip68CreatedUtxo.nativeAssets.filter { nativeAsset ->
                        nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX)
                    }.map { nativeAsset ->
                        val metadataMap = cip68PlutusData.toMetadataMap(nativeAsset.policy, nativeAsset.name)
                        metadataMap to cip68CreatedUtxo.nativeAssets.map { na ->
                            // dummy in the data
                            LedgerAsset(
                                id = i++,
                                policy = na.policy,
                                name = na.name,
                                supply = BigInteger.ONE
                            )
                        }
                    }
                } else {
                    null
                }
            }
        }.flatten()
    }

    companion object {
        private val CIP68_REFERENCE_TOKEN_REGEX = Regex("^000643b0.*$") // (100)TokenName
    }
}
