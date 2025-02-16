package io.newm.txbuildertest

import io.newm.chain.util.hexToByteArray
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KeyTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    @Disabled
    fun `test derive public from private extended key`() {
        val skBytes =
            "802a1b84dexxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx5027f258a50a1fb1f72f37387c4f9da60f5a14".hexToByteArray()
        val ed25519ParameterSpec: EdDSAParameterSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
        val skSpec = EdDSAPrivateKeySpec(ed25519ParameterSpec, skBytes)
        val sk = EdDSAPrivateKey(skSpec)

        val pkSpec = EdDSAPublicKeySpec(sk.a, sk.params)
        val pk = EdDSAPublicKey(pkSpec)

        pk.abyte.toHexString().also { println("publicKey: $it") }

//        println("publicKey: ${pk.encoded.toHexString()}")
    }
}
