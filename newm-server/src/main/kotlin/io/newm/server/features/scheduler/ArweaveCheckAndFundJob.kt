package io.newm.server.features.scheduler

import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.arweave.model.WeaveProps
import io.newm.server.features.arweave.model.WeaveRequest
import io.newm.server.features.arweave.model.WeaveResponse
import io.newm.server.ktx.await
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.koin.inject
import io.newm.shared.ktx.error
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.info
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.model.InvokeRequest

@DisallowConcurrentExecution
class ArweaveCheckAndFundJob : Job {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val environment: ApplicationEnvironment by inject()
    private val json: Json by inject()

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            try {
                log.info {
                    "ArweaveCheckAndFundJob executed at ${
                        LocalDateTime.ofInstant(
                            context.fireTime.toInstant(),
                            ZoneOffset.UTC
                        )
                    }"
                }
                val newmWeaveRequest =
                    WeaveRequest(
                        json.encodeToString(
                            WeaveProps(
                                arweaveWalletJson = environment.getSecureConfigString("arweave.walletJson"),
                                files = emptyList(),
                                checkAndFund = true
                            )
                        )
                    )

                val invokeRequest = InvokeRequest
                    .builder()
                    .functionName(environment.getConfigString("arweave.lambdaFunctionName"))
                    .payload(SdkBytes.fromUtf8String(json.encodeToString(newmWeaveRequest)))
                    .build()
                val invokeResult = invokeRequest.await()
                val weaveResponse: WeaveResponse = json.decodeFromString(invokeResult.payload().asUtf8String())
                if (weaveResponse.statusCode != 200) {
                    log.error { "Error invoking Arweave lambda: $weaveResponse" }
                }
            } catch (e: CancellationException) {
                log.info("ArweaveCheckAndFundJob cancelled.")
                throw e
            } catch (e: Throwable) {
                log.error("Error invoking Arweave lambda!", e)
            }
        }
    }
}
