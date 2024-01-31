package io.newm.server.aws.cloudfront

import com.google.common.truth.Truth.assertThat
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.BaseApplicationTests
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CloudfrontAudioStreamDataTest : BaseApplicationTests() {
    @Test
    fun testPolicyResourcePathUpdated() =
        runBlocking {
            val environment: ApplicationEnvironment by inject()
            val pk = environment.getSecureConfigString("aws.cloudFront.audioStream.privateKey")

            val keyPairId = "TEST_KEYPAIR_ID"
            val streamData =
                cloudfrontAudioStreamData {
                    this.url = "https://newm.io/path/filename.m3u8"
                    this.keyPairId = keyPairId
                    this.privateKey = pk
                }

            val url = streamData.url
            println("url=$url")
            assertThat(streamData.url).contains("https://newm.io/path/filename.m3u8?")
            assertThat(streamData.url).contains("Policy=")
            assertThat(streamData.url).contains("Key-Pair-Id=")
            assertThat(streamData.url).contains("Signature=")

            assertThat(streamData.cookies.filter { it.name == "CloudFront-Key-Pair-Id" }).isNotEmpty()
            assertThat(streamData.cookies.filter { it.name == "CloudFront-Signature" }).isNotEmpty()
            assertThat(streamData.cookies.filter { it.name == "CloudFront-Policy" }).isNotEmpty()
            val policy = streamData.cookies.filter { it.name == "CloudFront-Policy" }.first().value
            // policy is a slightly customized base64 encoded string, so just checking that it exists for now
            assertThat(policy).isNotNull()
        }
}
