package io.newm.server.features.cardano

import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.NewmChainGrpcKt.NewmChainCoroutineStub
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.cardano.repo.CardanoRepositoryImpl
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.getConfigBoolean
import io.newm.shared.ktx.getConfigInt
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module

val cardanoKoinModule = module {
    single<CardanoRepository> {
        CardanoRepositoryImpl(
            get(),
            get(),
            runBlocking { get<ApplicationEnvironment>().getSecureConfigString("aws.kms.keyId") },
            get(),
        )
    }
    single<NewmChainCoroutineStub> {
        val environment = get<ApplicationEnvironment>()
        val host = environment.getConfigString("newmChain.host")
        val port = environment.getConfigInt("newmChain.port")
        val jwt = runBlocking { environment.getSecureConfigString("newmChain.jwt") }
        val secure = environment.getConfigBoolean("newmChain.secure")
        val channel = ManagedChannelBuilder.forAddress(host, port).apply {
            if (secure) {
                useTransportSecurity()
            } else {
                usePlaintext()
            }
        }.build()
        NewmChainCoroutineStub(channel).withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(
                Metadata().apply {
                    put(
                        Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER),
                        "Bearer $jwt"
                    )
                }
            )
        )
    }
}
