package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OutletStatusCode(val code: Int) {
    @SerialName("1021")
    DRAFT(1021),

    @SerialName("1022")
    READY_TO_DISTRIBUTE(1022),

    @SerialName("1208")
    READY_TO_SUBMIT(1208),

    @SerialName("1201")
    DISTRIBUTE_INITIATED(1201),

    @SerialName("1202")
    DISTRIBUTED(1202),

    @SerialName("1203")
    TAKEDOWN_INITIATED(1203),

    @SerialName("1205")
    UPDATE_INITIATED(1205),

    @SerialName("1206")
    UPDATED(1206),

    @SerialName("1207")
    REVIEW_PENDING(1207),

    @SerialName("1034")
    DISAPPROVED(1034),
}
