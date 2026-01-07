package io.newm.server.config.model

import kotlinx.serialization.Serializable

/**
 * Entry for CIDR-based IP whitelist configuration.
 * Used to exempt specific IP ranges from reCAPTCHA verification.
 *
 * @property cidr The CIDR notation (e.g., "192.168.1.0/24" or "10.0.0.5/32")
 * @property desc Human-readable description for audit purposes (e.g., "office", "vpn")
 */
@Serializable
data class CidrWhitelistEntry(
    val cidr: String,
    val desc: String
)
