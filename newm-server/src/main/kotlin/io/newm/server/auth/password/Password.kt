package io.newm.server.auth.password

import io.newm.server.ext.toHash
import io.newm.server.ext.verify
import io.newm.server.serialization.PasswordSerializer
import kotlinx.serialization.Serializable

@Serializable(with = PasswordSerializer::class)
data class Password(val value: String) {
    override fun toString(): String = if (value.isBlank()) value else "***"

    fun toHash(): String = value.toHash()

    fun verify(hash: String): Boolean = value.verify(hash)
}
