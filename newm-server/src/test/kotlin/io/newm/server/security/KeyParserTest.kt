package io.newm.server.security

import com.google.common.truth.Truth.assertThat
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.BaseApplicationTests
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class KeyParserTest : BaseApplicationTests() {
    @Test
    fun testRSAPrivateKeyPcks8() =
        runBlocking {
            val environment: ApplicationEnvironment by inject()
            val pk = environment.getSecureConfigString("aws.cloudFront.audioStream.privateKey")
            assertThat(KeyParser.parse(pk)).isNotEmpty()
        }
}
