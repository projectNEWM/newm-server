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
        val outputUtxo =
            outputUtxo {
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
        val outputUtxo =
            outputUtxo {
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
        val outputUtxo =
            outputUtxo {
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

    @Test
    fun `test hosky poo drop`() {
        // 420000029ad9527271b1b1e3c27ee065c18df70a4a4cfc3093a41a44.41584f|150000000,
        // 51a5e236c4de3af2b8020442e2a26f454fda3b04cb621c1294a0ef34.424f4f4b|4000000,
        // b6a7467ea1deb012808ef4e87b5ff371e85f7142d7b356a40d9b42a0.436f726e75636f70696173205b76696120436861696e506f72742e696f5d|2000000,
        // af2e27f580f7f08e93190a81f72462f153026d06450924726645891b.44524950|420000000,
        // a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235.484f534b59|69696969,
        // 95a427e384527065f2f8946f5e86320d0117839a5e98ea2c0b55fb00.48554e54|1000000,
        // 5d16cc1a177b5d9ba9cfa9793b07e60f1fb70fea1f8aef064415d114.494147|1000000,
        // 681b5d0383ac3b457e1bcc453223c90ccef26b234328f45fa10fd276.4a5047|2000000,
        // 682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63.4e45574d|15000000,
        // 5dac8536653edc12f6f5e1045d8164b9f59998d3bdc300fc92843489.4e4d4b52|25000000,
        // 1d7f33bd23d85e1a25d87d86fac4f199c3197a2f7afeb662a0f34e1e.776f726c646d6f62696c65746f6b656e|1000000
        val outputUtxo =
            outputUtxo {
                address = "addr_test1qpwrm04e7fhalvw28ct274c2cfxqg5h3m3d8s0mnsn2xdtp633cwhfu0k3795qt78zcyy495a929k3seen3256m3gwes6t2fxt"
                nativeAssets.add(
                    nativeAsset {
                        policy = "420000029ad9527271b1b1e3c27ee065c18df70a4a4cfc3093a41a44"
                        name = "41584f"
                        amount = "150000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "51a5e236c4de3af2b8020442e2a26f454fda3b04cb621c1294a0ef34"
                        name = "424f4f4b"
                        amount = "4000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "b6a7467ea1deb012808ef4e87b5ff371e85f7142d7b356a40d9b42a0"
                        name = "436f726e75636f70696173205b76696120436861696e506f72742e696f5d"
                        amount = "2000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "af2e27f580f7f08e93190a81f72462f153026d06450924726645891b"
                        name = "44524950"
                        amount = "420000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235"
                        name = "484f534b59"
                        amount = "69696969"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "95a427e384527065f2f8946f5e86320d0117839a5e98ea2c0b55fb00"
                        name = "48554e54"
                        amount = "1000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "5d16cc1a177b5d9ba9cfa9793b07e60f1fb70fea1f8aef064415d114"
                        name = "494147"
                        amount = "1000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "681b5d0383ac3b457e1bcc453223c90ccef26b234328f45fa10fd276"
                        name = "4a5047"
                        amount = "2000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63"
                        name = "4e45574d"
                        amount = "15000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "5dac8536653edc12f6f5e1045d8164b9f59998d3bdc300fc92843489"
                        name = "4e4d4b52"
                        amount = "25000000"
                    }
                )
                nativeAssets.add(
                    nativeAsset {
                        policy = "1d7f33bd23d85e1a25d87d86fac4f199c3197a2f7afeb662a0f34e1e"
                        name = "776f726c646d6f62696c65746f6b656e"
                        amount = "1000000"
                    }
                )
            }
        println(outputUtxo.withMinUtxo(4310L))
    }
}
