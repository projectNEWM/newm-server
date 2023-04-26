package io.newm.server.features.cardano.model

import kotlinx.serialization.Serializable

@Serializable
data class EncryptionRequest(
    // The salt. Should be random hex. more than 8 bytes
    val s: String,
    // The spending password used for a password-based key generating function
    val password: String,
)
