package io.newm.server.aws

import io.ktor.server.application.ApplicationEnvironment
import io.newm.shared.ktx.getConfigString
import org.koin.core.qualifier.named
import org.koin.dsl.module
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

val AWS_REGION = named("aws.region")

val awsKoinModule =
    module {
        single<S3Client> {
            S3Client
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<S3Presigner> {
            S3Presigner
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<S3AsyncClient> {
            S3AsyncClient
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<SqsAsyncClient> {
            SqsAsyncClient
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<KmsAsyncClient> {
            KmsAsyncClient
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<SecretsManagerAsyncClient> {
            SecretsManagerAsyncClient
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<LambdaAsyncClient> {
            LambdaAsyncClient
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<Ec2Client> {
            Ec2Client
                .builder()
                .region(get<Region>(AWS_REGION))
                .build()
        }

        single<Region>(AWS_REGION) { Region.of(get<ApplicationEnvironment>().getConfigString("aws.region")) }
    }
