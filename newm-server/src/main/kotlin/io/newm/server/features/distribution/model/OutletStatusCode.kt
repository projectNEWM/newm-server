package io.newm.server.features.distribution.model

import kotlinx.serialization.Serializable

@Serializable
enum class OutletStatusCode(val code: Int) {
    DRAFT(1021),
    READY_TO_DISTRIBUTE(1022),
    READY_TO_SUBMIT(1208),
    DISTRIBUTE_INITIATED(1201),
    DISTRIBUTED(1202),
    TAKEDOWN_INITIATED(1203),
    UPDATE_INITIATED(1205),
    UPDATED(1206),
    REVIEW_PENDING(1207),
    DISAPPROVED(1034),
}
