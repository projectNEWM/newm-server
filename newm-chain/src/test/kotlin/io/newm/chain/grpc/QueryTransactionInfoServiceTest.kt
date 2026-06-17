package io.newm.chain.grpc

import com.google.common.truth.Truth.assertThat
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.database.repository.TransactionInfoIntegrityException
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.kogmios.protocols.model.Block
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.junit.jupiter.api.assertThrows

class QueryTransactionInfoServiceTest {
    @Test
    fun `queryTransactionInfo returns found false when repository returns null`() =
        runBlocking {
            val ledgerRepository = mockk<LedgerRepository>()
            every { ledgerRepository.queryTransactionInfo("missing-tx") } returns null

            val response = withService(ledgerRepository) { service ->
                service.queryTransactionInfo(
                    queryTransactionInfoRequest {
                        txId = "missing-tx"
                    }
                )
            }

            assertThat(response.found).isFalse()
            assertThat(response.blockNumber).isEqualTo(0L)
            assertThat(response.slotNumber).isEqualTo(0L)
            assertThat(response.spentUtxosCount).isEqualTo(0)
            assertThat(response.createdUtxosCount).isEqualTo(0)
        }

    @Test
    fun `queryTransactionInfo maps integrity failures to grpc internal`() =
        runBlocking {
            val ledgerRepository = mockk<LedgerRepository>()
            every { ledgerRepository.queryTransactionInfo("bad-tx") } throws TransactionInfoIntegrityException("broken")

            val exception =
                withService(ledgerRepository) { service ->
                    assertThrows<StatusRuntimeException> {
                        runBlocking {
                            service.queryTransactionInfo(
                                queryTransactionInfoRequest {
                                    txId = "bad-tx"
                                }
                            )
                        }
                    }
                }

            assertThat(exception.status.code).isEqualTo(Status.Code.INTERNAL)
            assertThat(exception.status.description).isEqualTo("broken")
        }

    private suspend fun <T> withService(
        ledgerRepository: LedgerRepository,
        block: suspend (NewmChainService) -> T,
    ): T {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockk<ChainRepository>(relaxed = true) }
                    single { ledgerRepository }
                    single { mockk<TxSubmitClientPool>(relaxed = true) }
                    single { mockk<StateQueryClientPool>(relaxed = true) }
                    single { mockk<SubmittedTransactionCache>(relaxed = true) }
                    single(qualifier = named("confirmedBlockFlow")) { mockk<MutableSharedFlow<Block>>(relaxed = true) }
                }
            )
        }

        return try {
            block(NewmChainService())
        } finally {
            stopKoin()
        }
    }
}
