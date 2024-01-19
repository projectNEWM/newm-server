package io.newm.txbuildertest

import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.toHexString
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.withMinUtxo
import org.junit.jupiter.api.Test

class MinUtxoTest {
    @Test
    fun `test minutxo ada-only enterprise`() {
        val outputUtxo = outputUtxo {
            address = "addr_test1vr29962dpn7cxmt02xqej94n6hppt3vm8tp7ws53ytfgq7shl88ss"
        }.also {
            println(
                "cbor: ${
                    it.toBuilder().setLovelace("5000000").build().toCborObject().toCborByteArray().toHexString()
                }"
            )
            // a200581d60d452e94d0cfd836d6f51819916b3d5c215c59b3ac3e7429122d2807a011a004c4b40
        }
        println(outputUtxo.withMinUtxo(4310L))
        // 857690
    }

    @Test
    fun `test minutxo ada-only staking`() {
        val outputUtxo = outputUtxo {
            address =
                "addr_test1qpwrm04e7fhalvw28ct274c2cfxqg5h3m3d8s0mnsn2xdtp633cwhfu0k3795qt78zcyy495a929k3seen3256m3gwes6t2fxt"
        }.also {
            println(
                "cbor: ${
                    it.toBuilder().setLovelace("5000000").build().toCborObject().toCborByteArray().toHexString()
                }"
            )
            // a2005839005c3dbeb9f26fdfb1ca3e16af570ac24c0452f1dc5a783f7384d466ac3a8c70eba78fb47c5a017e38b04254b4e9545b4619cce2aa6b7143b3011a004c4b40
        }
        println(outputUtxo.withMinUtxo(4310L))
        // 857690
    }

    @Test
    fun `test minutxo with token staking`() {
        val outputUtxo = outputUtxo {
            address =
                "addr_test1qpwrm04e7fhalvw28ct274c2cfxqg5h3m3d8s0mnsn2xdtp633cwhfu0k3795qt78zcyy495a929k3seen3256m3gwes6t2fxt"
            nativeAssets.add(
                nativeAsset {
                    policy = "bce40727b0979ab901007a4969c3177a6cdab005427f4e977776a69b"
                    name = "624e45574d"
                    amount = "543000"
                }
            )
        }.also {
            println(
                "cbor: ${
                    it.toBuilder().setLovelace("5000000").build().toCborObject().toCborByteArray().toHexString()
                }"
            )
            // a2005839005c3dbeb9f26fdfb1ca3e16af570ac24c0452f1dc5a783f7384d466ac3a8c70eba78fb47c5a017e38b04254b4e9545b4619cce2aa6b7143b301821a004c4b40a1581cbce40727b0979ab901007a4969c3177a6cdab005427f4e977776a69ba145624e45574d1a00084918
        }
        println(outputUtxo.withMinUtxo(4310L))
        // 1168010
    }
}
