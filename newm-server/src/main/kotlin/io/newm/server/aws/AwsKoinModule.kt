package io.newm.server.aws

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
import io.ktor.server.application.*
import io.newm.shared.ktx.getConfigString
import org.koin.core.qualifier.named
import org.koin.dsl.module

val AWS_REGION = named("aws.region")

val awsKoinModule =
    module {
        single<AmazonS3> {
            AmazonS3ClientBuilder.standard()
                .withRegion(get<String>(AWS_REGION))
                .build()
        }

        single<AmazonSQSAsync> {
            AmazonSQSAsyncClientBuilder.standard()
                .withRegion(get<String>(AWS_REGION))
                .build()
        }

        single<AWSKMSAsync> {
            AWSKMSAsyncClientBuilder.standard()
                .withRegion(get<String>(AWS_REGION))
                .build()
        }

        single<AWSSecretsManagerAsync> {
            AWSSecretsManagerAsyncClientBuilder.standard()
                .withRegion(get<String>(AWS_REGION))
                .build()
        }

        single<AWSLambdaAsync> {
            AWSLambdaAsyncClientBuilder.standard()
                .withRegion(get<String>(AWS_REGION))
                .build()
        }

        single<String>(AWS_REGION) { get<ApplicationEnvironment>().getConfigString("aws.region") }
    }
