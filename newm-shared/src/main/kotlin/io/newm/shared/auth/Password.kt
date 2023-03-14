package io.newm.shared.auth

import io.newm.shared.ext.toHash
import io.newm.shared.ext.verify
import io.newm.shared.serialization.PasswordSerializer
import kotlinx.serialization.Serializable

@Serializable(with = PasswordSerializer::class)
data class Password(val value: String) {
    override fun toString(): String = if (value.isBlank()) value else "***"

    fun toHash(): String = value.toHash()

    fun verify(hash: String): Boolean = value.verify(hash)
}
