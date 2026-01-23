package io.newm.chain.grpc

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.model.NativeAsset as ModelNativeAsset
import io.newm.chain.grpc.NativeAsset as GrpcNativeAsset
import io.newm.chain.model.toNativeAssetMap as toModelNativeAssetMap
import io.newm.txbuilder.ktx.toNativeAssetMap as toGrpcNativeAssetMap

class MonitorPaymentAddressTest {
    @Test
    fun `test native asset map comparison type mismatch`() {
        val policy = "policy"
        val name = "name"
        val amount = BigInteger.valueOf(100)

        val modelAsset = ModelNativeAsset(policy, name, amount)
        val grpcAsset = nativeAsset {
            this.policy = policy
            this.name = name
            this.amount = amount.toString()
        }

        val modelMap: Map<String, List<ModelNativeAsset>> = listOf(modelAsset).toModelNativeAssetMap()

        // Fix: convert gRPC assets to model assets
        val grpcMapAsModel: Map<String, List<ModelNativeAsset>> = listOf(grpcAsset)
            .map {
                ModelNativeAsset(
                    policy = it.policy,
                    name = it.name,
                    amount = it.amount.toBigInteger()
                )
            }.toModelNativeAssetMap()

        assertThat(modelMap == grpcMapAsModel).isTrue()
    }

    @Test
    fun `test native asset map comparison sorting mismatch fixed`() {
        val policy = "policy"
        val name1 = "name1"
        val name2 = "name2"
        val amount = BigInteger.valueOf(100)

        val modelAsset1 = ModelNativeAsset(policy, name1, amount)
        val modelAsset2 = ModelNativeAsset(policy, name2, amount)

        // Inverting order
        val modelMap = listOf(modelAsset2, modelAsset1).toModelNativeAssetMap()

        val modelMapExpected = listOf(modelAsset1, modelAsset2).toModelNativeAssetMap()

        assertThat(modelMap["policy"]).containsExactly(modelAsset1, modelAsset2).inOrder()
        assertThat(modelMap == modelMapExpected).isTrue()
    }
}
