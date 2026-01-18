package io.newm.ardrive.turbo.upload.util

import com.google.common.truth.Truth.assertThat
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.DataItemTag
import java.util.Base64
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class DataItemSignerTest {
    @Test
    fun `estimates size matches signed output`() =
        runBlocking {
            val signer = ArweaveSigner(TestJwkFactory.create())
            val dataItemSigner = DataItemSigner(signer)
            val options = DataItemOptions(
                tags = listOf(DataItemTag("Content-Type", "text/plain")),
                target = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32) { 1 }),
            )

            val payload = "hello".toByteArray()
            val signed = dataItemSigner.sign(payload, options)
            val estimate = dataItemSigner.estimateSize(payload.size, options)

            assertThat(signed.size).isEqualTo(estimate)
            assertThat(signed.bytes.size).isEqualTo(estimate)
        }
}
