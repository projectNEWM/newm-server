package io.newm.chain.grpc

class GreeterService : GreeterGrpcKt.GreeterCoroutineImplBase() {
    override suspend fun sayHello(request: HelloRequest): HelloReply {
        return HelloReply.newBuilder().apply {
            message = "Hello, ${request.name}!"
        }.build()
    }
}