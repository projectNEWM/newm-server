package io.newm.server.ktx

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.lambda.AWSLambdaAsync
import com.amazonaws.services.lambda.model.InvokeRequest
import com.amazonaws.services.lambda.model.InvokeResult
import io.newm.shared.koin.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val awsLambda: AWSLambdaAsync by inject()

suspend fun InvokeRequest.await(): InvokeResult {
    return suspendCoroutine { continuation ->
        awsLambda.invokeAsync(
            this,
            object : AsyncHandler<InvokeRequest, InvokeResult> {
                override fun onSuccess(
                    request: InvokeRequest,
                    result: InvokeResult
                ) {
                    continuation.resume(result)
                }

                override fun onError(exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}
