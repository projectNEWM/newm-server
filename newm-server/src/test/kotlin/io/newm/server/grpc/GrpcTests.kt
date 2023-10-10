package io.newm.server.grpc

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.newm.chain.grpc.NewmChainGrpcKt
import io.newm.chain.grpc.OutputUtxo
import io.newm.chain.grpc.QueryDatumByHashResponse
import io.newm.chain.grpc.QueryTransactionConfirmationCountResponse
import io.newm.chain.grpc.QueryUtxosResponse
import io.newm.chain.grpc.SnapshotNativeAssetsResponse
import io.newm.chain.grpc.SubmitTransactionResponse
import io.newm.chain.grpc.acquireMutexRequest
import io.newm.chain.grpc.datumOrNull
import io.newm.chain.grpc.monitorAddressRequest
import io.newm.chain.grpc.monitorNativeAssetsRequest
import io.newm.chain.grpc.monitorPaymentAddressRequest
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.queryByNativeAssetRequest
import io.newm.chain.grpc.queryDatumByHashRequest
import io.newm.chain.grpc.queryTransactionConfirmationCountRequest
import io.newm.chain.grpc.queryUtxosOutputRefRequest
import io.newm.chain.grpc.queryUtxosRequest
import io.newm.chain.grpc.releaseMutexRequest
import io.newm.chain.grpc.snapshotNativeAssetsRequest
import io.newm.chain.grpc.submitTransactionRequest
import io.newm.chain.grpc.walletRequest
import io.newm.chain.util.Constants
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class GrpcTests {

    companion object {
//        private const val TEST_HOST = "localhost"
//        private const val TEST_PORT = 3737
//        private const val TEST_SECURE = false

        private const val TEST_HOST = "newm-chain.cardanostakehouse.com"
        private const val TEST_PORT = 3737
        private const val TEST_SECURE = true

        // DO NOT COMMIT THIS TOKEN
        private const val JWT_TOKEN = "<JWT_TOKEN_HERE_DO_NOT_COMMIT>"
    }

    private fun buildClient(): NewmChainGrpcKt.NewmChainCoroutineStub {
        val channel = ManagedChannelBuilder.forAddress(TEST_HOST, TEST_PORT).apply {
            if (TEST_SECURE) {
                useTransportSecurity()
            } else {
                usePlaintext()
            }
        }.build()
        return NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(
                Metadata().apply {
                    put(
                        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "Bearer $JWT_TOKEN"
                    )
                }
            )
        )
    }

    @Test
    @Disabled
    fun `test queryUtxos`() = runBlocking {
        val client = buildClient()
        val request = queryUtxosRequest {
            address = "addr_test1wzrkjfmne72vg4ppqk2xmt470tltjyqwldappq2c5tuhf8qflnumr"
        }

        val response = client.queryUtxos(request)
        assertThat(response).isInstanceOf(QueryUtxosResponse::class.java)
        assertThat(response.utxosList.size).isGreaterThan(4)
        val testUtxo =
            response.utxosList.first { it.hash == "23024af80670da4b8d4c192f63b5d7119dc59e7b3ae83ef2cd798350f9b59cd4" }
        assertThat(testUtxo).isNotNull()
        assertThat(testUtxo.ix).isEqualTo(1L)
        assertThat(testUtxo.datum).isEqualTo("d8799f581c602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55464e45574d5f31581c9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d8040ff")
        assertThat(testUtxo.nativeAssetsList.size).isEqualTo(1)
        assertThat(testUtxo.nativeAssetsList[0].policy).isEqualTo("3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55")
        assertThat(testUtxo.nativeAssetsList[0].name).isEqualTo("4e45574d5f31")
        assertThat(testUtxo.nativeAssetsList[0].amount.toLong()).isEqualTo(1L)
    }

    @Test
    @Disabled
    fun `test queryUtxosByStakeAddress`() = runBlocking {
        val client = buildClient()
        val request = queryUtxosRequest {
            address = "stake_test1uqagcu8t578mglz6q9lr3vzz2j6wj4zmgcvuec42ddc58vcacpguy"
        }
        val response = client.queryUtxosByStakeAddress(request)
        println(response.toString())
        assertThat(response).isInstanceOf(QueryUtxosResponse::class.java)
        assertThat(response.utxosList.size).isGreaterThan(4)
        val testUtxo =
            response.utxosList.first { it.hash == "bfbf405d57b198594b5731eb502a377489020bd48e84b5344289ea1c6b3a4646" }
        assertThat(testUtxo).isNotNull()
        assertThat(testUtxo.ix).isEqualTo(1L)
        assertThat(testUtxo.datum.cborHex).isEqualTo("")
        assertThat(testUtxo.nativeAssetsList.size).isEqualTo(17)
        assertThat(testUtxo.nativeAssetsList[0].policy).isEqualTo("52366a9f74840bb47d0509393c18343f376250de1a01e0a43619e471")
        assertThat(testUtxo.nativeAssetsList[0].name).isEqualTo("74426967546f6b656e4e616d653038")
        assertThat(testUtxo.nativeAssetsList[0].amount.toLong()).isEqualTo(8L)
    }

    @Test
    @Disabled
    fun `test queryUtxosByOutputRef`() = runBlocking {
        val client = buildClient()
        val request = queryUtxosOutputRefRequest {
            hash = "23024af80670da4b8d4c192f63b5d7119dc59e7b3ae83ef2cd798350f9b59cd4"
            ix = 1L
        }
        val response = client.queryUtxosByOutputRef(request)
        println(response.toString())
        assertThat(response).isInstanceOf(QueryUtxosResponse::class.java)
        assertThat(response.utxosList.size).isEqualTo(1)
        val testUtxo =
            response.utxosList.first { it.hash == "23024af80670da4b8d4c192f63b5d7119dc59e7b3ae83ef2cd798350f9b59cd4" }
        assertThat(testUtxo).isNotNull()
        assertThat(testUtxo.ix).isEqualTo(1L)
        assertThat(testUtxo.datumHash).isEqualTo("2ecb792a1da27a4e8a3c7ff126eb4b721ea7d6708f84162a82c097f4ace8f4d1")
        assertThat(testUtxo.datum.cborHex).isEqualTo("d8799f581c602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55464e45574d5f31581c9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d8040ff")
        assertThat(testUtxo.nativeAssetsList.size).isEqualTo(1)
        assertThat(testUtxo.nativeAssetsList[0].policy).isEqualTo("3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55")
        assertThat(testUtxo.nativeAssetsList[0].name).isEqualTo("4e45574d5f31")
        assertThat(testUtxo.nativeAssetsList[0].amount.toLong()).isEqualTo(1L)
    }

    @Test
    @Disabled
    fun `test queryDatumByHash`() = runBlocking {
        val client = buildClient()
        val request =
            queryDatumByHashRequest { datumHash = "2ecb792a1da27a4e8a3c7ff126eb4b721ea7d6708f84162a82c097f4ace8f4d1" }
        val response = client.queryDatumByHash(request)
        assertThat(response).isInstanceOf(QueryDatumByHashResponse::class.java)
        assertThat(response.datum.cborHex).isEqualTo("d8799f581c602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55464e45574d5f31581c9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d8040ff")
    }

    @Test
    @Disabled
    fun `test submitTx`() = runBlocking {
        val client = buildClient()
        val request = submitTransactionRequest {
            cbor =
                ByteString.fromHex("84a400858258205549f543da473f1053596d00091e6973ea949077ca639f155a143894bdd25a9a008258209927e5de056c92e771aeafbe50af39d9bd9e429e863662aa8a5f90be8520c19900825820c5b42d5f94b847d857626a5d8134607539b918f42d6b234b9e5a37f340fc14ce00825820cc53d6ed905708f5a6db78e20d37070b91ab558d77a4d91dc6d3865246a7bb2500825820d5c9ab6f807c69fc6713f19ef2c49cf7b1b7c80d028a7d56bb86a4f18b9d07e4010181a200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b011b0000008c3a140b9c021a0002cd21031a013eecb7a100818258204499320a77997987955eadba91721d5be54ca36536c5448009e822ba3f882d69584031b7356b14c7d6510f07582bc17987cf09fbe47744f71a9ad8d3c04ce04956a315c45f162e5fb5824dac38f78093ac8993f92bce1e19abe77833a859a4b70104f5f6")
        }

        val response = client.submitTransaction(request)
        assertThat(response).isInstanceOf(SubmitTransactionResponse::class.java)
        assertThat(response.result).isEqualTo("MsgAcceptTx")
        assertThat(response.txId).isEqualTo("70a3b6e119004981d275ec637ccc4b2e6afacdf70fede259cd00e34fb75ef433")
    }

    @Test
    @Disabled
    fun `test queryLiveUtxos`() = runBlocking {
        val client = buildClient()
        val queryUtxosRequest = queryUtxosRequest {
            address = "addr_test1vrdqad0dwcg5stk9pzdknkrsurzkc8z9rqp9vyfrnrsgxkc4r8za2"
        }

        var queryUtxosResponse = client.queryUtxos(queryUtxosRequest)
        assertThat(queryUtxosResponse).isInstanceOf(QueryUtxosResponse::class.java)

        var queryLiveUtxosResponse = client.queryLiveUtxos(queryUtxosRequest)
        assertThat(queryLiveUtxosResponse).isEqualTo(queryUtxosResponse)

        // submit a tx to change the live utxos
        val request = submitTransactionRequest {
            cbor =
                ByteString.fromHex("84a400868258202a9ed5c889796567d9c0d6607634eb80fc5ba149ddc19481767d1820aa58665c008258207f8f8d0ccbe5abbeefb916561076608e8c3d50a3d41381792b0f005f9983eeff018258208c1169b97131b9fc5f9db8b6841a96bf6f116b3561900f2f306a053f3dd0011d008258208d4549456c34ead1d1c156bc2a5058d86470ae24d2c3868c99202fefc158b30800825820a2df6b85b0fa6b1cff992da297d6df58d4f0870630ee8ab7b014086e4ad5cc3600825820f9028bd3398e3f931790bca5c8a36237b00811c193ebe8a906fd9870d85a0c8f000182a200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b011b0000009c4490304ca200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b01821a00106026a1581c48664e8d76f2b15606677bd117a3eac9929c378ac547ed295518dfd5a14f74426967546f6b656e4e616d65303202021a0002f4a9031a014c1ce5a100818258204499320a77997987955eadba91721d5be54ca36536c5448009e822ba3f882d695840bb5489659b0e81ac1c9c13e0a4f7338dc86668b3a7962d5748faee6dbf32aee34e2dbc7ed830c2eb3ee6701130897cefe7578c8c2d891eb67f0b2473a547950df5f6")
        }

        val response = client.submitTransaction(request)
        assertThat(response).isInstanceOf(SubmitTransactionResponse::class.java)
        assertThat(response.result).isEqualTo("MsgAcceptTx")
        assertThat(response.txId).isEqualTo("b2d31a7c9e86eff32d5c00842bdc316cedf9d01dbb60af0420430eb8cdf0d311")

        queryUtxosResponse = client.queryUtxos(queryUtxosRequest)
        queryLiveUtxosResponse = client.queryLiveUtxos(queryUtxosRequest)
        assertThat(queryLiveUtxosResponse).isNotEqualTo(queryUtxosResponse)

        println("utxos: $queryUtxosResponse")
        println("liveUtxos: $queryLiveUtxosResponse")
    }

    @Test
    @Disabled
    fun `test queryTransactionConfirmations`() = runBlocking {
        val client = buildClient()
        val request = queryTransactionConfirmationCountRequest {
            with(txIds) {
                add("a8ebbe98a73d733dd1664fddab025582900bf9bf64b1999128e53534a864ea24")
                add("21024c6f9aec5d227b33dedf380b3e05777364c8dcd010511ed16450b9164507")
                add("0000000000000000000000000000000000000000000000000000000000000000")
            }
        }

        val response = client.queryTransactionConfirmationCount(request)
        println(response.toString())
        assertThat(response).isInstanceOf(QueryTransactionConfirmationCountResponse::class.java)
        assertThat(response.txIdToConfirmationCountMap["0000000000000000000000000000000000000000000000000000000000000000"]).isEqualTo(
            0L
        )
        assertThat(response.txIdToConfirmationCountMap["a8ebbe98a73d733dd1664fddab025582900bf9bf64b1999128e53534a864ea24"]).isGreaterThan(
            response.txIdToConfirmationCountMap["21024c6f9aec5d227b33dedf380b3e05777364c8dcd010511ed16450b9164507"]
        )
    }

    @Test
    @Disabled
    fun `test scriptWatching`() = runBlocking {
        val scriptAddress =
            "addr_test1xpta3gjhejy2yuc6uddhdmm4xckzk4csg4y9g2ac9nd8awxcns7gfqfjmlcm0mp27f89jwahzs2xrw0vadw56z8rxdrqe8svxm"
        val client = buildClient()
        val monitorAddressUtxosRequest = monitorAddressRequest {
            address = scriptAddress
        }

        val flow = client.monitorAddress(monitorAddressUtxosRequest)
        flow.collect { monitorAddressUtxosResponse ->
            println("transaction: $monitorAddressUtxosResponse")
            println()
        }
    }

    @Test
    @Disabled
    fun `test monitorPaymentAddress`() = runBlocking {
        val client = buildClient()
        val response = client.monitorPaymentAddress(
            monitorPaymentAddressRequest {
                address = "addr_test1vrdqad0dwcg5stk9pzdknkrsurzkc8z9rqp9vyfrnrsgxkc4r8za2"
                lovelace = "6000000"
                timeoutMs = 600000
                nativeAssets.add(
                    nativeAsset {
                        policy = "769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff"
                        name = "744e45574d" // tNEWM
                        amount = "23"
                    }
                )
            }
        )
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("Payment Received")
    }

    @Test
    @Disabled
    fun `test monitorPaymentAddress ada-only`() = runBlocking {
        val client = buildClient()
        val response = client.monitorPaymentAddress(
            monitorPaymentAddressRequest {
                address = "addr_test1vr3enxw7xpvnq9yaur7l03907khnl4wac79c68yqy7906gq4e58wk"
                lovelace = "7288690"
                timeoutMs = 600000
            }
        )
        assertThat(response.success).isTrue()
        assertThat(response.message).isEqualTo("Payment Received")
    }

    @Test
    @Disabled
    fun `test monitorNativeAssets`() = runBlocking {
        val client = buildClient()
        val flow = client.monitorNativeAssets(
            monitorNativeAssetsRequest {
                startAfterId = 1763701
            }
        )
        flow.collect { monitorNativeAssetsResponse ->
            println("monitorNativeAssetsResponse: $monitorNativeAssetsResponse")
            println()
        }
    }

    @Test
    @Disabled
    fun `test calculateMinUtxoForOutput`() = runBlocking {
        val client = buildClient()
        val request = outputUtxo {
            address = Constants.DUMMY_STAKE_ADDRESS
            // lovelace = "0" // auto-calculated
            nativeAssets.add(
                nativeAsset {
                    policy = Constants.DUMMY_TOKEN_POLICY_ID
                    name = Constants.DUMMY_MAX_TOKEN_NAME
                    amount = "100000000"
                }
            )
        }
        val response = client.calculateMinUtxoForOutput(request)
        assertThat(response).isInstanceOf(OutputUtxo::class.java)
        assertThat(response.lovelace).isEqualTo("1288690")
    }

    @Test
    @Disabled
    fun `test snapshot`() = runBlocking {
        val client = buildClient()
        val request = snapshotNativeAssetsRequest {
            policy = "769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff"
            name = "744e45574d" // tNEWM
        }
        val response = client.snapshotNativeAssets(request)
        assertThat(response).isInstanceOf(SnapshotNativeAssetsResponse::class.java)
        val stakeAddressToAmountMap = response.snapshotEntriesList.associate { it.stakeAddress to it.amount }
        println("stakeAddressToAmountMap: $stakeAddressToAmountMap")
        assertThat(stakeAddressToAmountMap.size).isEqualTo(4)
        assertThat(stakeAddressToAmountMap["total_supply"]).isEqualTo(69L)
    }

    @Test
    @Disabled
    fun `test deriveWalletAddresses`() = runBlocking {
        val client = buildClient()
        val request = walletRequest {
            accountXpubKey =
                "xpub10yq2v72lq0h7lnhkw308uy23fjq384zufvyesh6mlklnpmv048xs8arze4nws0xfp8h87d7jdxwgm5dsr7l0qruedrtcdudjlnxls3sm0qlln"
        }
        val response = client.deriveWalletAddresses(request)
        println("response: $response")
    }

    @Test
    @Disabled
    fun `test queryWalletControlledLiveUtxos`() = runBlocking {
        val client = buildClient()
        val request = walletRequest {
            accountXpubKey =
                "xpub10yq2v72lq0h7lnhkw308uy23fjq384zufvyesh6mlklnpmv048xs8arze4nws0xfp8h87d7jdxwgm5dsr7l0qruedrtcdudjlnxls3sm0qlln"
        }
        val response = client.queryWalletControlledLiveUtxos(request)
        println("response: $response")
    }

    @Test
    @Disabled
    fun `test queryUtxoByNativeAsset`() = runBlocking {
        val client = buildClient()
        val request = queryByNativeAssetRequest {
            policy = "3d0d75aad1eb32f0ce78fb1ebc101b6b51de5d8f13c12daa88017624"
            name = "4f7261636c6546656564" // OracleFeed
        }
        val response = client.queryUtxoByNativeAsset(request)
        println("response: $response")
        assertThat(response.datumOrNull).isNotNull()
    }

    @Test
    @Disabled
    fun `test mutex`() = runBlocking {
        val client = buildClient()
        try {
            client.acquireMutex(
                acquireMutexRequest {
                    mutexName = "test"
                    this.lockExpiryMs = 10000
                    this.acquireWaitTimeoutMs = 10000
                }
            )

            val request = snapshotNativeAssetsRequest {
                policy = "769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff"
                name = "744e45574d" // tNEWM
            }
            val response = client.snapshotNativeAssets(request)
            assertThat(response).isInstanceOf(SnapshotNativeAssetsResponse::class.java)
            val stakeAddressToAmountMap = response.snapshotEntriesList.associate { it.stakeAddress to it.amount }
            assertThat(stakeAddressToAmountMap.size).isEqualTo(4)
        } finally {
            client.releaseMutex(releaseMutexRequest { mutexName = "test" })
        }
    }

    @Test
    @Disabled
    fun `test query music nfts`() = runBlocking {
        val client = buildClient()
        val request = queryByNativeAssetRequest {
            policy = "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617"
            name = "001bc280008557b67dfef2ddfdc102ed2b6c224bc266c44dc3401ff600e16501" // MusicNFT
        }
        val response = client.queryLedgerAssetMetadataListByNativeAsset(request)
        assertThat(response).isNotNull()
        assertThat(response.ledgerAssetMetadataCount).isEqualTo(6)
        response.ledgerAssetMetadataList.forEach {
            println(it)
        }
    }
}
