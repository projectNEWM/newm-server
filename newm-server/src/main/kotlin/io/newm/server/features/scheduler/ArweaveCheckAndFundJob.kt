package io.newm.server.features.scheduler

import com.amazonaws.services.lambda.model.InvokeRequest
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException

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
                val newmWeaveRequest = WeaveRequest(
                    json.encodeToString(
                        WeaveProps(
                            arweaveWalletJson = environment.getSecureConfigString("arweave.walletJson"),
                            files = emptyList(),
                            checkAndFund = true
                        )
                    )
                )

                val invokeRequest = InvokeRequest()
                    .withFunctionName(environment.getConfigString("arweave.lambdaFunctionName"))
                    .withPayload(json.encodeToString(newmWeaveRequest))

                val invokeResult = invokeRequest.await()
                val weaveResponse: WeaveResponse = json.decodeFromString(invokeResult.payload.array().decodeToString())
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
