package io.newm.server.features.arweave.repo

import com.google.common.truth.Truth.assertThat
import io.newm.server.features.arweave.model.WeaveFile
import io.newm.server.features.arweave.model.WeaveProps
import io.newm.server.features.arweave.model.WeaveRequest
import io.newm.server.features.arweave.model.WeaveResponse
import io.newm.server.features.arweave.model.WeaveResponseItem
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.await
import io.newm.shared.serialization.BigDecimalSerializer
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.model.InvokeRequest

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
    fun `test arweave upload lambda`() =
        runBlocking {
            val json =
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    isLenient = true
                    serializersModule =
                        SerializersModule {
                            contextual(BigDecimal::class, BigDecimalSerializer)
                        }
                }
            val newmWeaveRequest =
                WeaveRequest(
                    json.encodeToString(
                        WeaveProps(
                            arweaveWalletJson = "",
                            files =
                                listOf(
                                    WeaveFile(
                                        url = "https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.webp",
                                        contentType = "image/png"
                                    )
                                ),
                            checkAndFund = false,
                        )
                    )
                )
            val payload = json.encodeToString(newmWeaveRequest)
            println("payload: $payload")
            val invokeRequest =
                InvokeRequest
                    .builder()
                    .functionName("<arn here>")
                    .payload(SdkBytes.fromUtf8String(payload))
                    .build()

            val invokeResult = invokeRequest.await()

            val responseString = invokeResult.payload().asUtf8String()
            println("responseString: $responseString")
            val weaveResponse: WeaveResponse = json.decodeFromString(responseString)
            val weaveResponsItems: List<WeaveResponseItem> = json.decodeFromString(weaveResponse.body)
            assertThat(weaveResponsItems).isNotEmpty()
            println("weaveResponses: $weaveResponsItems")
        }
}
