package io.newm.server.grpc

import com.google.common.truth.Truth.assertThat
import io.grpc.ManagedChannelBuilder
import io.newm.chain.grpc.NewmChainGrpcKt
import io.newm.chain.grpc.QueryUtxosRequest
import io.newm.chain.grpc.QueryUtxosResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class GrpcTests {

    @Test
    @Disabled
    fun `test queryUtxos`() = runBlocking {
        // plainText for localhost testing only. use SSL later.
        val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
        val client = NewmChainGrpcKt.NewmChainCoroutineStub(channel)
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
}
