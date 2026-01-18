package io.newm.ardrive.turbo.upload.util

import com.google.common.truth.Truth.assertThat
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateCrtKey
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class SignedDataItemStreamFactoryTest {
    @Test
    fun `fromSignedBytes exposes stream and size`() {
        val data = "signed".toByteArray()
        val factory = SignedDataItemStreamFactory.fromSignedBytes(data)

        assertThat(factory.dataItemSizeFactory()).isEqualTo(data.size)
        assertThat(factory.dataItemStreamFactory().readBytes()).isEqualTo(data)
    }

    @Test
    fun `fromBytes signs data and exposes size`() =
        runBlocking {
            val signer = DataItemSigner(ArweaveSigner(TestJwkFactory.create()))
            val factory = SignedDataItemStreamFactory.fromBytes(
                data = "payload".toByteArray(),
                signer = signer,
                options = DataItemOptions(),
            )

            assertThat(factory.dataItemSizeFactory()).isGreaterThan(0)
            assertThat(factory.dataItemStreamFactory().readBytes().size)
                .isEqualTo(factory.dataItemSizeFactory())
        }
}
