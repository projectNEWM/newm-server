package io.newm.server.features.cardano

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.grpc.plutusDataMap
import io.newm.chain.grpc.plutusDataMapItem
import io.newm.chain.grpc.utxo
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.repo.CardanoRepositoryImpl
import io.newm.txbuilder.ktx.toPlutusData
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class OracleParseTests : BaseApplicationTests() {
    @Test
    fun `test parse charli3 oracle datum`() =
        runBlocking {
            val startTimestamp = System.currentTimeMillis() - 1000L
            val endTimestamp = startTimestamp + 3600000L // plus 1 hour
            val cardanoRepository = CardanoRepositoryImpl(mockk(), mockk(), "kmsKeyId", mockk(), mockk())
            cardanoRepository.isMainnet = false
            cardanoRepository.oracleUtxoCache.put(
                "newm",
                utxo {
                    datum = plutusData {
                        constr = 0
                        list = plutusDataList {
                            listItem.add(
                                plutusData {
                                    constr = 2
                                    list = plutusDataList {
                                        listItem.add(
                                            plutusData {
                                                map = plutusDataMap {
                                                    with(mapItem) {
                                                        add(
                                                            plutusDataMapItem {
                                                                mapItemKey = 0.toPlutusData()
                                                                mapItemValue = 2263.toPlutusData()
                                                            }
                                                        )
                                                        add(
                                                            plutusDataMapItem {
                                                                mapItemKey = 1.toPlutusData()
                                                                mapItemValue = startTimestamp.toPlutusData()
                                                            }
                                                        )
                                                        add(
                                                            plutusDataMapItem {
                                                                mapItemKey = 2.toPlutusData()
                                                                mapItemValue = endTimestamp.toPlutusData()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            )

            val price = cardanoRepository.queryNEWMUSDPrice()
            assertThat(price).isEqualTo(2263L)
        }
}
