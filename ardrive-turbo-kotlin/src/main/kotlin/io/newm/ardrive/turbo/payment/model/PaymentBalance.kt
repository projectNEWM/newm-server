package io.newm.ardrive.turbo.payment.model

import io.newm.ardrive.turbo.upload.model.CreditShareApproval
import kotlinx.serialization.Serializable

@Serializable
data class PaymentBalance(
    val controlledWinc: String,
    val winc: String,
    val effectiveBalance: String,
    val receivedApprovals: List<CreditShareApproval> = emptyList(),
    val givenApprovals: List<CreditShareApproval> = emptyList(),
)
