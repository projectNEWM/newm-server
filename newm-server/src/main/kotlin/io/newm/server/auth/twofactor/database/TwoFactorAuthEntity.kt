package io.newm.server.auth.twofactor.database

import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.lowerCase
import java.time.LocalDateTime

class TwoFactorAuthEntity(
    id: EntityID<Long>
) : Entity<Long>(id) {
    var email: String by TwoFactorAuthTable.email
    var codeHash: String by TwoFactorAuthTable.codeHash
    var expiresAt: LocalDateTime by TwoFactorAuthTable.expiresAt

    companion object : EntityClass<Long, TwoFactorAuthEntity>(TwoFactorAuthTable) {
        fun getByEmail(email: String): TwoFactorAuthEntity? =
            find {
                TwoFactorAuthTable.email.lowerCase() eq email.lowercase()
            }.firstOrNull()

        fun deleteByEmail(email: String) =
            TwoFactorAuthTable.deleteWhere {
                this.email.lowerCase() eq email.lowercase()
            }

        fun deleteAllExpired() =
            TwoFactorAuthTable.deleteWhere {
                expiresAt lessEq LocalDateTime.now()
            }
    }
}
