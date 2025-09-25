package io.newm.server.features.cardano

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Metadata
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.cardano.repo.CardanoRepositoryImpl
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.getConfigBoolean
import io.newm.shared.ktx.getConfigInt
import io.newm.shared.ktx.getConfigString
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module

private val log by lazy { KotlinLogging.logger {} }

val cardanoKoinModule =
    module {
        single<CardanoRepository> {
            CardanoRepositoryImpl(
                get(),
                get(),
                runBlocking { get<ApplicationEnvironment>().getSecureConfigString("aws.kms.keyId") },
                get(),
                get(),
                get(),
            )
        }
        single<NewmChainCoroutineStub> {
            val environment = get<ApplicationEnvironment>()
            val host = environment.getConfigString("newmChain.host")
            val port = environment.getConfigInt("newmChain.port")
            val jwt = runBlocking { environment.getSecureConfigString("newmChain.jwt") }
            val secure = environment.getConfigBoolean("newmChain.secure")

            log.info { "Configuring gRPC channel for NewmChain:" }
            log.info { "  Host: $host" }
            log.info { "  Port: $port" }
            log.info { "  Secure: $secure" }
            log.info { "  Keep-alive time: 30 seconds" }
            log.info { "  Keep-alive timeout: 20 seconds" }
            log.info { "  Keep-alive without calls: true" }
            log.info { "  Transport: ${if (secure) "TLS" else "Plaintext"}" }

            val channel =
                NettyChannelBuilder
                    .forTarget("dns:///$host:$port") // force DNS resolver
                    .keepAliveTime(30L, TimeUnit.SECONDS)
                    .keepAliveTimeout(20L, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .apply {
                        if (secure) {
                            useTransportSecurity()
                        } else {
                            usePlaintext()
                        }
                    }.build()

            log.info { "gRPC channel created successfully for $host:$port" }
            NewmChainCoroutineStub(channel)
                .withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(
                        Metadata().apply {
                            put(
                                Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                                "Bearer $jwt"
                            )
                        }
                    )
                ).withWaitForReady()
        }
    }
