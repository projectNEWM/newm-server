package io.newm.chain.grpc

import org.koin.dsl.module

val grpcKoinModule =
    module {
        single { TxSubmitClientPool(5) }
    }
