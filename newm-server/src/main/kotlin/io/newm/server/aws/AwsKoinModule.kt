package io.projectnewm.server.aws

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.ktor.server.application.ApplicationEnvironment
import io.projectnewm.server.ext.getConfigString
import org.koin.dsl.module

val awsKoinModule = module {

    single<AmazonS3> {
        AmazonS3ClientBuilder.standard()
            .withRegion(get<Regions>())
            .withCredentials(get())
            .build()
    }

    single<AWSCredentialsProvider> { CredentialsProvider(get()) }

    single { Regions.fromName(get<ApplicationEnvironment>().getConfigString("aws.region")) }
}
