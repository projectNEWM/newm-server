package io.newm.txbuildertest

import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import io.newm.chain.grpc.signingKey
import io.newm.txbuilder.ktx.sign
import io.newm.txbuilder.ktx.verify
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Do not want to commit real keys to source control")
class SigningKeyTest {
    @Test
    fun `test sign verify`() {
        val signingKey = signingKey {
            this.vkey = ByteString.fromHex("5b857...e4fa67c5f")
            this.skey =
                ByteString.fromHex("1846e...06f24d")
        }
        val transactionId = "cae6793079d9dc328c65cfcec715843dd01a1b70493af6df07401146e1650669".hexToByteArray()

        val signature = signingKey.sign(transactionId)
        println("signature: ${signature.toHexString()}")

        assertThat(signingKey.verify(transactionId, signature)).isTrue()
    }

    @Test
    fun `test sign verify with chain code`() {
        val signingKey = signingKey {
            this.vkey = ByteString.fromHex("fc632...de3")
            this.skey =
                ByteString.fromHex("08f33e2792...b2c8b49f")
        }
        val transactionId = "cae6793079d9dc328c65cfcec715843dd01a1b70493af6df07401146e1650669".hexToByteArray()

        val signature = signingKey.sign(transactionId)
        println("signature: ${signature.toHexString()}")

        assertThat(signingKey.verify(transactionId, signature)).isTrue()
    }
}
