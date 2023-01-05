package io.newm.server.grpc

import com.google.common.truth.Truth.assertThat
import io.grpc.ManagedChannelBuilder
import io.newm.chain.grpc.GreeterGrpcKt
import io.newm.chain.grpc.HelloReply
import io.newm.chain.grpc.HelloRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HelloWorldTests {

    @Test
    fun `test GreeterService`() = runBlocking {
        // plainText for localhost testing only. use SSL later.
        val channel = ManagedChannelBuilder.forAddress("localhost", 3737).usePlaintext().build()
        val greeterClient = GreeterGrpcKt.GreeterCoroutineStub(channel)
        val request = HelloRequest.newBuilder().setName("Nathan Huffhines").build()
        val response = greeterClient.sayHello(request)
        assertThat(response).isInstanceOf(HelloReply::class.java)
        assertThat(response.message).isEqualTo("Hello, Nathan Huffhines!")
    }
}