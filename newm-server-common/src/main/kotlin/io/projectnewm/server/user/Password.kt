package io.projectnewm.server.user

import io.projectnewm.server.ext.toHash
import io.projectnewm.server.ext.verify
import io.projectnewm.server.serialization.PasswordSerializer
import kotlinx.serialization.Serializable

@Serializable(with = PasswordSerializer::class)
data class Password(val value: String) {
    override fun toString(): String = if (value.isBlank()) value else "***"

    fun toHash(): String = value.toHash()

    fun verify(hash: String): Boolean = value.verify(hash)
}
