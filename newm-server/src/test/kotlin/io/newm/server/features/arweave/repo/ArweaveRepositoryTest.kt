package io.newm.server.features.arweave.repo

import com.amazonaws.services.lambda.model.InvokeRequest
import com.google.common.truth.Truth.assertThat
import io.newm.server.features.arweave.model.WeaveFile
import io.newm.server.features.arweave.model.WeaveProps
import io.newm.server.features.arweave.model.WeaveRequest
import io.newm.server.features.arweave.model.WeaveResponse
import io.newm.server.features.arweave.model.WeaveResponseItem
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.await
import io.newm.shared.serialization.BigDecimalSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ArweaveRepositoryTest {

    @Test
    fun `test webp regex replacement`() {
        val urlString = "https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.png"
        val newUrl =
            urlString.asValidUrl().replace(Regex("\\.(png|jpg|jpeg|bmp|gif|tiff)\$", RegexOption.IGNORE_CASE), ".webp")
        assertThat(newUrl).isEqualTo("https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.webp")
    }

    @Test
    @Disabled
    fun `test arweave upload lambda`() = runBlocking {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            serializersModule = SerializersModule {
                contextual(BigDecimal::class, BigDecimalSerializer)
            }
        }
        val newmWeaveRequest = WeaveRequest(
            json.encodeToString(
                WeaveProps(
                    arweaveWalletJson = "",
                    files = listOf(
                        WeaveFile(
                            url = "https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.webp",
                            contentType = "image/png"
                        )
                    )
                )
            )
        )
        val payload = json.encodeToString(newmWeaveRequest)
        println("payload: $payload")
        val invokeRequest = InvokeRequest()
            .withFunctionName("<arn here>")
            .withPayload(payload)

        val invokeResult = invokeRequest.await()

        val responseString = invokeResult.payload.array().decodeToString()
        println("responseString: $responseString")
        val weaveResponse: WeaveResponse = json.decodeFromString(responseString)
        val weaveResponsItems: List<WeaveResponseItem> = json.decodeFromString(weaveResponse.body)
        assertThat(weaveResponsItems).isNotEmpty()
        println("weaveResponses: $weaveResponsItems")
    }
}
