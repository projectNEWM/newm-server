package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import io.newm.chain.config.Config
import io.newm.chain.util.toCreatedUtxoSet
import io.newm.chain.util.toSpentUtxoSet
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.createChainSyncClient
import io.newm.kogmios.protocols.model.BlockPraos
import io.newm.kogmios.protocols.model.GenesisEra
import io.newm.kogmios.protocols.model.PointDetail
import io.newm.kogmios.protocols.model.result.IntersectionFoundResult
import io.newm.kogmios.protocols.model.result.RollBackward
import io.newm.kogmios.protocols.model.result.RollForward
import io.newm.kogmios.protocols.model.result.ShelleyGenesisConfigResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("This test is for manual use only")
class DumpBlockTest {
    companion object {
        private const val TEST_HOST = "localhost"
        private const val TEST_PORT = 1338
        private const val TEST_SECURE = false
    }

    @Test
    fun `test dump block`() =
        runBlocking {
            createChainSyncClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                (client as StateQueryClient).genesisConfig(GenesisEra.SHELLEY).let { genesisConfig ->
                    Config.genesis = genesisConfig.result as ShelleyGenesisConfigResult
                }

                val response =
                    client.findIntersect(
                        listOf(
                            PointDetail(
                                slot = 116522871L,
                                id = "ca1589ad47edf4c11d15fadbdc81f57d7dc61e13af1cd146f63d9e3cbadbd26d",
                            ),
                        ),
                    )
                assertThat(response).isNotNull()
                assertThat(response.result).isInstanceOf(IntersectionFoundResult::class.java)
                assertThat((response.result.intersection as PointDetail).slot).isEqualTo(
                    116522871L,
                )

                val response1 = client.nextBlock()
                assertThat(response1).isNotNull()
                assertThat(response1.result).isInstanceOf(RollBackward::class.java)
                assertThat(((response1.result as RollBackward).point as PointDetail).slot).isEqualTo(
                    116522871L,
                )

                val response2 = client.nextBlock()
                assertThat(response2).isNotNull()
                assertThat(response2.result).isInstanceOf(RollForward::class.java)
                assertThat(((response2.result as RollForward).block as BlockPraos).slot).isEqualTo(
                    116522878L,
                )
                assertThat(((response2.result as RollForward).block as BlockPraos).height).isEqualTo(
                    9940326L,
                )
                val block = (response2.result as RollForward).block as BlockPraos
                println("--------createdUtxoSet--------")
                block.toCreatedUtxoSet().forEach { utxo ->
                    println(utxo)
                }
                println("--------spentUtxoSet--------")
                block.toSpentUtxoSet().forEach { utxo ->
                    println(utxo)
                }
            }
            Unit
        }
}
