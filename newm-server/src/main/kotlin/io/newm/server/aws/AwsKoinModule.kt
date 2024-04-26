package io.newm.server.aws

import com.amazonaws.regions.Region
import com.amazonaws.regions.RegionUtils
import com.amazonaws.regions.Regions
import com.amazonaws.services.kms.AWSKMSAsync
import com.amazonaws.services.kms.AWSKMSAsyncClientBuilder
import com.amazonaws.services.lambda.AWSLambdaAsync
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.secretsmanager.AWSSecretsManagerAsync
import com.amazonaws.services.secretsmanager.AWSSecretsManagerAsyncClientBuilder
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import io.ktor.server.application.ApplicationEnvironment
import io.newm.shared.ktx.getConfigString
import org.koin.dsl.module

val awsKoinModule =
    module {

        single<AmazonS3> {
            AmazonS3ClientBuilder.standard()
                .withRegion(get<Regions>())
                .build()
        }

        single<AmazonSQSAsync> {
            AmazonSQSAsyncClientBuilder.standard()
                .withRegion(get<Regions>())
                .build()
        }

        single<AWSKMSAsync> {
            AWSKMSAsyncClientBuilder.standard()
                .withRegion(get<Regions>())
                .build()
        }

        single<AWSSecretsManagerAsync> {
            AWSSecretsManagerAsyncClientBuilder.standard()
                .withRegion(get<Regions>())
                .build()
        }

        single<AWSLambdaAsync> {
            AWSLambdaAsyncClientBuilder.standard()
                .withRegion(get<Regions>())
                .build()
        }

        single<Region> { RegionUtils.getRegion(get<ApplicationEnvironment>().getConfigString("aws.region")) }
    }
