package io.newm.server.ktx

import io.newm.shared.koin.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import software.amazon.awssdk.services.lambda.LambdaAsyncClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse

private val awsLambda: LambdaAsyncClient by inject()

suspend fun InvokeRequest.await(): InvokeResponse =
    suspendCoroutine { continuation ->
        awsLambda.invoke(this).whenComplete { invokeResponse, throwable ->
            throwable?.let { continuation.resumeWithException(it) } ?: continuation.resume(invokeResponse)
        }
    }
