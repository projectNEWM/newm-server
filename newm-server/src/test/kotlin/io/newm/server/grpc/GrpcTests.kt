package io.newm.server.grpc

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.newm.chain.grpc.MonitorAddressRequest
import io.newm.chain.grpc.NewmChainGrpcKt
import io.newm.chain.grpc.QueryUtxosRequest
import io.newm.chain.grpc.QueryUtxosResponse
import io.newm.chain.grpc.SubmitTransactionRequest
import io.newm.chain.grpc.SubmitTransactionResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class GrpcTests {

    @Test
    @Disabled
    fun `test queryUtxos`() = runBlocking {
        // plainText for localhost testing only. use SSL later.
        val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
        val client =
            NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    Metadata().apply {
                        put(
                            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                            "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>"
                        )
                    }
                )
            )
        val request = QueryUtxosRequest.newBuilder()
            .setAddress("addr_test1wzrkjfmne72vg4ppqk2xmt470tltjyqwldappq2c5tuhf8qflnumr")
            .build()
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
    fun `test submitTx`() = runBlocking {
        // plainText for localhost testing only. use SSL later.
        val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
        val client =
            NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    Metadata().apply {
                        put(
                            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                            "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>"
                        )
                    }
                )
            )
        val request = SubmitTransactionRequest.newBuilder()
            .setCbor(ByteString.fromHex("84a400858258205549f543da473f1053596d00091e6973ea949077ca639f155a143894bdd25a9a008258209927e5de056c92e771aeafbe50af39d9bd9e429e863662aa8a5f90be8520c19900825820c5b42d5f94b847d857626a5d8134607539b918f42d6b234b9e5a37f340fc14ce00825820cc53d6ed905708f5a6db78e20d37070b91ab558d77a4d91dc6d3865246a7bb2500825820d5c9ab6f807c69fc6713f19ef2c49cf7b1b7c80d028a7d56bb86a4f18b9d07e4010181a200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b011b0000008c3a140b9c021a0002cd21031a013eecb7a100818258204499320a77997987955eadba91721d5be54ca36536c5448009e822ba3f882d69584031b7356b14c7d6510f07582bc17987cf09fbe47744f71a9ad8d3c04ce04956a315c45f162e5fb5824dac38f78093ac8993f92bce1e19abe77833a859a4b70104f5f6"))
            .build()
        val response = client.submitTransaction(request)
        assertThat(response).isInstanceOf(SubmitTransactionResponse::class.java)
        assertThat(response.result).isEqualTo("MsgAcceptTx")
        assertThat(response.txId).isEqualTo("70a3b6e119004981d275ec637ccc4b2e6afacdf70fede259cd00e34fb75ef433")
    }

    @Test
    @Disabled
    fun `test queryLiveUtxos`() = runBlocking {
        // plainText for localhost testing only. use SSL later.
        val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
        val client =
            NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    Metadata().apply {
                        put(
                            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                            "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>"
                        )
                    }
                )
            )
        val queryUtxosRequest = QueryUtxosRequest.newBuilder()
            .setAddress("addr_test1vrdqad0dwcg5stk9pzdknkrsurzkc8z9rqp9vyfrnrsgxkc4r8za2")
            .build()
        var queryUtxosResponse = client.queryUtxos(queryUtxosRequest)
        assertThat(queryUtxosResponse).isInstanceOf(QueryUtxosResponse::class.java)

        var queryLiveUtxosResponse = client.queryLiveUtxos(queryUtxosRequest)
        assertThat(queryLiveUtxosResponse).isEqualTo(queryUtxosResponse)

        // submit a tx to change the live utxos
        val request = SubmitTransactionRequest.newBuilder()
            .setCbor(ByteString.fromHex("84a400868258202a9ed5c889796567d9c0d6607634eb80fc5ba149ddc19481767d1820aa58665c008258207f8f8d0ccbe5abbeefb916561076608e8c3d50a3d41381792b0f005f9983eeff018258208c1169b97131b9fc5f9db8b6841a96bf6f116b3561900f2f306a053f3dd0011d008258208d4549456c34ead1d1c156bc2a5058d86470ae24d2c3868c99202fefc158b30800825820a2df6b85b0fa6b1cff992da297d6df58d4f0870630ee8ab7b014086e4ad5cc3600825820f9028bd3398e3f931790bca5c8a36237b00811c193ebe8a906fd9870d85a0c8f000182a200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b011b0000009c4490304ca200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b01821a00106026a1581c48664e8d76f2b15606677bd117a3eac9929c378ac547ed295518dfd5a14f74426967546f6b656e4e616d65303202021a0002f4a9031a014c1ce5a100818258204499320a77997987955eadba91721d5be54ca36536c5448009e822ba3f882d695840bb5489659b0e81ac1c9c13e0a4f7338dc86668b3a7962d5748faee6dbf32aee34e2dbc7ed830c2eb3ee6701130897cefe7578c8c2d891eb67f0b2473a547950df5f6"))
            .build()
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
    fun `test scriptWatching`() = runBlocking {
        val scriptAddress =
            "addr_test1xpta3gjhejy2yuc6uddhdmm4xckzk4csg4y9g2ac9nd8awxcns7gfqfjmlcm0mp27f89jwahzs2xrw0vadw56z8rxdrqe8svxm"

        // plainText for localhost testing only. use SSL later.
        val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
        val client =
            NewmChainGrpcKt.NewmChainCoroutineStub(channel).withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(
                    Metadata().apply {
                        put(
                            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                            "Bearer <JWT_TOKEN_HERE_DO_NOT_COMMIT>"
                        )
                    }
                )
            )
        val monitorAddressUtxosRequest = MonitorAddressRequest.newBuilder()
            .setAddress(scriptAddress)
            .build()

        val flow = client.monitorAddress(monitorAddressUtxosRequest)
        flow.collect { monitorAddressUtxosResponse ->
            println("transaction: $monitorAddressUtxosResponse")
            println()
        }
    }
}
