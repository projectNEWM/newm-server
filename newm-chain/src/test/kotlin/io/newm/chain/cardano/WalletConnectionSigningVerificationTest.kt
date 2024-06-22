package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import io.mockk.mockk
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.grpc.NewmChainService
import io.newm.chain.grpc.TxSubmitClientPool
import io.newm.chain.grpc.submitTransactionRequest
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

class WalletConnectionSigningVerificationTest {
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
    fun `test verify signData`() =
        runBlocking {
            val service = NewmChainService()
            // this signature is the result of the user signing the json message string bytes with their wallet
            // Example:
            // const api = await window.cardano.eternl.enable();
            // await api.getRewardAddresses();
            // ['e053daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269']
            // const stakeAddressHex = 'e053daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269';
            // const stakeAddressBech32 = 'stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv';
            // const challengeUtf8 = `{"newm_account": "${stakeAddressBech32}", "uuid": "4bf6c35b-747d-48da-830b-9dfce57b15e4"}`;
            // console.log(challengeUtf8);
            // {"newm_account": "stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv", "uuid": "4bf6c35b-747d-48da-830b-9dfce57b15e4"}
            // const challengeHex = "7b226e65776d5f6163636f756e74223a20227374616b655f746573743175706661343263757a6674647a6b6734706d667838306b71736c6e327679796d676564737a353866757761357936676a6674377a76222c202275756964223a202234626636633335622d373437642d343864612d383330622d396466636535376231356534227d";
            // const result = await api.signData(stakeAddressHex, challengeHex);
            // console.log(JSON.stringify(result));
            // {
            //   "signature":"84582aa201276761646472657373581de053daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269a166686173686564f458847b226e65776d5f6163636f756e74223a20227374616b655f746573743175706661343263757a6674647a6b6734706d667838306b71736c6e327679796d676564737a353866757761357936676a6674377a76222c202275756964223a202234626636633335622d373437642d343864612d383330622d396466636535376231356534227d58408a3a8a8772bfd890a1c30f30f86f0f756c2969ef6367c2c97385a6673a4c88d98a494025eeeefb65357a1cec4676c7b08c3ed1e31bb6277dd7ec92fc8ba97907",
            //   "key":"a4010103272006215820155720fbed788992fe8c3b9a30efa4508b33eb73af473b48f15d6fae6feba60a"
            //  }
            val expectedChallenge = """{"newm_account": "stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv", "uuid": "4bf6c35b-747d-48da-830b-9dfce57b15e4"}"""
            val request =
                verifySignDataRequest {
                    publicKeyHex = "a4010103272006215820155720fbed788992fe8c3b9a30efa4508b33eb73af473b48f15d6fae6feba60a"
                    signatureHex =
                        "84582aa201276761646472657373581de053daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269a166686173686564f458847b226e65776d5f6163636f756e74223a20227374616b655f746573743175706661343263757a6674647a6b6734706d667838306b71736c6e327679796d676564737a353866757761357936676a6674377a76222c202275756964223a202234626636633335622d373437642d343864612d383330622d396466636535376231356534227d58408a3a8a8772bfd890a1c30f30f86f0f756c2969ef6367c2c97385a6673a4c88d98a494025eeeefb65357a1cec4676c7b08c3ed1e31bb6277dd7ec92fc8ba97907"
                }
            val response = service.verifySignData(request)
            assertThat(response.verified).isTrue()
            assertThat(response.challenge).isEqualTo(expectedChallenge)
            assertThat(response.hasErrorMessage()).isFalse()
        }

    @Test
    fun `test verify signed transaction`() =
        runBlocking {
            // this cborHex is the result of the user signing the transaction with their wallet
            // the transaction is generated with the

            val service = NewmChainService()
            val cborHex = "84a600818258206a6b53d93e01a597e825844d2e524479c17cc4cf2285e0ca819177566a9015cc010181a2005839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269011b0000000bd90759a2021a0002add9030107582076550301b648a4db0814750df8f03bf6898413cc03e0c77eb174b68b4b2a57a80e81581c53daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269a10082825820155720fbed788992fe8c3b9a30efa4508b33eb73af473b48f15d6fae6feba60a5840a5556d58b363a93cf85f1f3ad9265d6dac6542f38814fab4dbb5c12f9af40463db21befb9097ee449aa3ee9e4e1abfb58da69b5d9e345bf74665a69d3b63d70a825820e98bc049f662c60c3e1066df75f01e02688c3432765b0ec870463d1f932725235840b088825998c953757bf30fbb6a00d7724090cb60110fbcc9dba022f5a3f907bd3eb8b3d0ebca426cf6800135bbaaf292b1e84ce556e92a1bd936c502adb1b806f5d90103a100a11902a2a1636d73678378407b22636f6e6e656374546f223a224e45574d204d6f62696c652037303133613764322d303938312d343833322d386561622d336636623064343236663865222c7840227374616b6541646472657373223a227374616b655f746573743175706661343263757a6674647a6b6734706d667838306b71736c6e327679796d676564737a72353866757761357936676a6674377a76227d"
            val expectedChallenge = """{"connectTo":"NEWM Mobile 7013a7d2-0981-4832-8eab-3f6b0d426f8e","stakeAddress":"stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv"}"""
            val request =
                submitTransactionRequest {
                    cbor = ByteString.fromHex(cborHex)
                }
            val response = service.verifySignTransaction(request)
            println(response)
            assertThat(response.verified).isTrue()
            assertThat(response.challenge).isEqualTo(expectedChallenge)
            assertThat(response.hasErrorMessage()).isFalse()
        }
}
