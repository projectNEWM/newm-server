package io.newm.chain.grpc

import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyServerBuilder
import io.ktor.server.config.ApplicationConfig
import io.newm.chain.cardano.randomHex
import io.newm.ktor.server.grpc.GRPCApplicationEngine
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.altindag.ssl.SSLFactory
import nl.altindag.ssl.netty.util.NettySslUtils
import nl.altindag.ssl.pem.util.PemUtils
import nl.altindag.ssl.util.KeyManagerUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import javax.net.ssl.X509ExtendedKeyManager
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

object GrpcConfig {
    // Cannot use DI here as GRPC is initialized before it is available.
    private val log: Logger by lazy { LoggerFactory.getLogger("GrpcConfig") }

    val init: GRPCApplicationEngine.Configuration.(appConfig: ApplicationConfig) -> Unit = { appConfig ->
        this.serverConfigurer = {
            try {
                // Enable keepAlive every 30 seconds
                keepAliveTime(30L, TimeUnit.SECONDS)
                // Allow sending a ping even if there are no calls
                permitKeepAliveWithoutCalls(true)

                // This is aggressive, but we should be fine since there will only be one or two instances of newm-server
                // communicating with us. 10 seconds is the most aggressive ping client can have.
                permitKeepAliveTime(10L, TimeUnit.SECONDS)
                // 20 seconds to respond to a keepAlive ping
                keepAliveTimeout(20L, TimeUnit.SECONDS)

                maxConnectionAge(1L, TimeUnit.HOURS)
                maxConnectionAgeGrace(5L, TimeUnit.MINUTES)

                // Enable JWT authorization
                intercept(JwtAuthorizationServerInterceptor(appConfig.config("jwt")))

                val grpcConfig = appConfig.config("grpc")

                val certificateChainPath =
                    Paths.get(
                        grpcConfig.propertyOrNull("sslCertChainPath")?.getString() ?: "/nonexistent_${randomHex(16)}.pem"
                    )
                val privateKeyPath =
                    Paths.get(
                        grpcConfig.propertyOrNull("sslPrivateKeyPath")?.getString() ?: "/nonexistent_${randomHex(16)}.pem"
                    )

                if (certificateChainPath.exists() && privateKeyPath.exists()) {
                    (this as? NettyServerBuilder)?.let { nettyServerBuilder ->
                        log.warn("gRPC Secured with TLS")
                        val keyManager: X509ExtendedKeyManager =
                            PemUtils.loadIdentityMaterial(certificateChainPath, privateKeyPath)
                        val sslFactory =
                            SSLFactory.builder()
                                .withIdentityMaterial(keyManager)
                                .withSwappableIdentityMaterial()
                                .build()
                        val sslContext = GrpcSslContexts.configure(NettySslUtils.forServer(sslFactory)).build()
                        nettyServerBuilder.sslContext(sslContext)

                        @OptIn(DelicateCoroutinesApi::class)
                        GlobalScope.launch {
                            var privateKeyLastModified =
                                withContext(Dispatchers.IO) {
                                    Files.readAttributes(privateKeyPath, BasicFileAttributes::class.java).lastModifiedTime()
                                }
                            var certChainLastModified =
                                withContext(Dispatchers.IO) {
                                    Files.readAttributes(certificateChainPath, BasicFileAttributes::class.java)
                                        .lastModifiedTime()
                                }
                            while (true) {
                                try {
                                    delay(60000L)
                                    val checkPrivateKeyLastModified =
                                        withContext(Dispatchers.IO) {
                                            Files.readAttributes(privateKeyPath, BasicFileAttributes::class.java)
                                                .lastModifiedTime()
                                        }
                                    val checkCertChainLastModified =
                                        withContext(Dispatchers.IO) {
                                            Files.readAttributes(certificateChainPath, BasicFileAttributes::class.java)
                                                .lastModifiedTime()
                                        }
                                    if (privateKeyLastModified != checkPrivateKeyLastModified || certChainLastModified != checkCertChainLastModified) {
                                        // data has changed. reload it
                                        val newKeyManager: X509ExtendedKeyManager =
                                            PemUtils.loadIdentityMaterial(certificateChainPath, privateKeyPath)
                                        KeyManagerUtils.swapKeyManager(sslFactory.keyManager.get(), newKeyManager)

                                        privateKeyLastModified = checkPrivateKeyLastModified
                                        certChainLastModified = checkCertChainLastModified
                                        log.warn("RELOADED SSL CERTIFICATES!!!")
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Throwable) {
                                    Sentry.captureException(e)
                                    log.error("Error with SSL Certificate Reload!", e)
                                }
                            }
                        }
                    } ?: log.error("gRPC TLS Failed: ServerBuilder is not a NettyServerBuilder!")
                } else {
                    log.warn("gRPC Unsecured with plaintext")
                    if (!certificateChainPath.exists()) {
                        log.warn("File not found: ${certificateChainPath.absolutePathString()}")
                    }
                    if (!privateKeyPath.exists()) {
                        log.warn("File not found: ${privateKeyPath.absolutePathString()}")
                    }
                }
            } catch (e: Throwable) {
                log.error("Error configuring GRPC!", e)
            }
        }
    }
}
