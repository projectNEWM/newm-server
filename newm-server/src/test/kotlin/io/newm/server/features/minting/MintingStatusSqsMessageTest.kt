package io.newm.server.features.minting

import com.google.common.truth.Truth.assertThat
import io.newm.server.features.song.model.MintingStatus
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import java.util.*

class MintingStatusSqsMessageTest {
    @Test
    fun `test MintingStatusSqsMessage`() {
        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = true
                serializersModule =
                    SerializersModule {
                        contextual(UUID::class, UUIDSerializer)
                    }
            }
        val songId = UUID.randomUUID()
        val message =
            MintingStatusSqsMessage(
                songId = songId,
                mintingStatus = MintingStatus.ReadyToDistribute,
            )

        val messageJson = json.encodeToString(message)
        val expectedJson = """{"songId":"$songId","mintingStatus":"ReadyToDistribute"}"""
        assertThat(messageJson).isEqualTo(expectedJson)

        val deserializedMessage: MintingStatusSqsMessage = json.decodeFromString(messageJson)
        assertThat(message).isEqualTo(deserializedMessage)
    }
}
