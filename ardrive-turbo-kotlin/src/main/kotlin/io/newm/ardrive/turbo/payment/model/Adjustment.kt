package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Adjustment(
    val name: String,
    val description: String,
    val operatorMagnitude: Double,
    val operator: AdjustmentOperator,
    val adjustmentAmount: String,
)

@Serializable
enum class AdjustmentOperator {
    @SerialName("multiply")
    MULTIPLY,

    @SerialName("add")
    ADD,
}
