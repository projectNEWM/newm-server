package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.grpc.NewmChainService
import io.newm.chain.grpc.TxSubmitClientPool
import io.newm.chain.grpc.verifySignDataRequest
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.kogmios.protocols.model.Block
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SignDataVerificationTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // initialize Koin dependency injection for tests
            startKoin {
                modules(
                    module {
                        // inject mocks
                        single<Logger> { LoggerFactory.getLogger("SignDataVerificationTest") }
                        single { mockk<LedgerRepository>(relaxed = true) }
                        single { mockk<TxSubmitClientPool>(relaxed = true) }
                        single { mockk<SubmittedTransactionCache>(relaxed = true) }
                        single(qualifier = named("confirmedBlockFlow")) { mockk<MutableSharedFlow<Block>>(relaxed = true) }
                    }
                )
            }
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            // close Koin
            stopKoin()
        }
    }

    @Test
    fun `test verify`() =
        runBlocking {
            val service = NewmChainService()
            // this signature is the result of the user signing the json message string bytes with their wallet
            // by calling: await walletApi.signData(stakeAddressHex, messageBytesHex);
            // where the pre-hexed message is:
            // {"newm_account": "stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv"}
            val request =
                verifySignDataRequest {
                    publicKeyHex = "a4010103272006215820155720fbed788992fe8c3b9a30efa4508b33eb73af473b48f15d6fae6feba60a"
                    signatureHex = "84582aa201276761646472657373581de053daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269a166686173686564f458557b226e65776d5f6163636f756e74223a20227374616b655f746573743175706661343263757a6674647a6b6734706d667838306b71736c6e327679796d676564737a353866757761357936676a6674377a76227d0a5840c0e1a7e359a0c89d53f3c8fcf28b173a9cf4458b0bc275dd44e8879dc52bb1892b865a78c64f755ed5449079bee115f390dc247ceee012044c907f4f3a4ab902"
                }
            val response = service.verifySignData(request)
            assertThat(response.verified).isTrue()
            assertThat(response.message).isEqualTo("stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv")
        }
}
