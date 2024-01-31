package io.newm.server.ktx

import com.google.protobuf.kotlin.toByteString
import io.newm.chain.grpc.signingKey
import io.newm.server.features.cardano.model.Key
import io.newm.txbuilder.ktx.sign

fun Key.sign(transactionId: ByteArray): ByteArray =
    signingKey {
        vkey = this@sign.vkey.toByteString()
        skey = this@sign.skey.toByteString()
    }.sign(transactionId)
