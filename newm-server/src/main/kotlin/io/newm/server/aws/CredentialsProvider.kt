package io.newm.server.aws

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import io.ktor.server.application.ApplicationEnvironment
import io.newm.shared.ktx.getConfigString

internal class CredentialsProvider(
    private val environment: ApplicationEnvironment
) : AWSCredentialsProvider {
    override fun getCredentials(): AWSCredentials = object : AWSCredentials {
        override fun getAWSAccessKeyId() = environment.getConfigString("aws.accessKeyId")
        override fun getAWSSecretKey() = environment.getConfigString("aws.secretKey")
    }

    override fun refresh() {
    }
}
