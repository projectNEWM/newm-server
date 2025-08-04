package io.newm.server.features.song.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PaymentType {
    /**
     * The first currency we are supporting. We accept USD value worth of ADA. DEFAULT
     */
    @SerialName("ADA")
    ADA,

    /**
     * We accept USD value worth of NEWM, initially with a 20% discount.
     */
    @SerialName("NEWM")
    NEWM,

    /**
     * We accept USD via PayPal.
     */
    @SerialName("PAYPAL")
    PAYPAL,

    // TODO: Finish support for additional payment types

//    /**
//     * We accept USD value worth of USDM as 1:1 equivalent.
//     */
//    @SerialName("USDM")
//    USDM,

//    /**
//     * We accept USD value using Stripe via Credit Card.
//     */
//    @SerialName("STRIPE_CCD")
//    STRIPE_CCD,
}
