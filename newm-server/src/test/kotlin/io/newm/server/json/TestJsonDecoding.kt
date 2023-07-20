package io.newm.server.json

import com.google.common.truth.Truth.assertThat
import io.newm.server.features.collaboration.model.Collaboration
import io.newm.shared.serialization.BigDecimalSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TestJsonDecoding {

    @Test
    fun `test BigDecimal json`() {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            serializersModule = SerializersModule {
                contextual(BigDecimal::class, BigDecimalSerializer)
            }
        }

        val jsonString =
            """{"songId":"7141858a-29bb-4cdf-8e8e-4896f24c133d","email":"tscandalios@newm.io","role":"Artist","royaltyRate":100,"credited":true,"featured":false}"""

        val result: Collaboration = json.decodeFromString(jsonString)

        assertThat(result.royaltyRate).isEqualTo(BigDecimal(100))
    }
}
