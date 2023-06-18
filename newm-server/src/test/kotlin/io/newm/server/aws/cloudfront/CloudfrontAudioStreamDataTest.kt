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
    fun testPolicyResourcePathUpdated() = runBlocking {
        val environment: ApplicationEnvironment by inject()
        val pk = environment.getSecureConfigString("aws.cloudFront.audioStream.privateKey")

        val keyPairId = "TEST_KEYPAIR_ID"
        val streamData = cloudfrontAudioStreamData {
            this.url = "https://newm.io/path/filename.m3u8"
            this.keyPairId = keyPairId
            this.privateKey = pk
        }

        assertThat(streamData.cookies).containsKey("CloudFront-Key-Pair-Id")
        assertThat(streamData.cookies["CloudFront-Key-Pair-Id"]).isEqualTo(keyPairId)
        assertThat(streamData.cookies).containsKey("CloudFront-Policy")
        val policy = streamData.cookies["CloudFront-Policy"]
        // policy is a slightly customized base64 encoded string, so just checking that it exists for now
        assertThat(policy).isNotNull()
    }
}
