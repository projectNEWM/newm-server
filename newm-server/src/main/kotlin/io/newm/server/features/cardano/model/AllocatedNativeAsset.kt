package io.newm.server.features.cardano.model

import io.newm.chain.grpc.NativeAsset
import java.util.UUID

data class AllocatedNativeAsset(
    val asset: NativeAsset,
    val allocations: Map<UUID, Long>
)
