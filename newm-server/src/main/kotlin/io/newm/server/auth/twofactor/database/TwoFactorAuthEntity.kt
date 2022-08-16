package io.newm.server.auth.twofactor.database

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.time.LocalDateTime

class TwoFactorAuthEntity(id: EntityID<Long>) : Entity<Long>(id) {

    var email by TwoFactorAuthTable.email
    var codeHash by TwoFactorAuthTable.codeHash
    var expiresAt by TwoFactorAuthTable.expiresAt

    companion object : EntityClass<Long, TwoFactorAuthEntity>(TwoFactorAuthTable) {

        fun getByEmail(email: String): TwoFactorAuthEntity? = find {
            TwoFactorAuthTable.email.lowerCase() eq email.lowercase()
        }.firstOrNull()

        fun deleteByEmail(email: String) = TwoFactorAuthTable.deleteWhere {
            TwoFactorAuthTable.email.lowerCase() eq email.lowercase()
        }

        fun deleteAllExpired() = TwoFactorAuthTable.deleteWhere {
            TwoFactorAuthTable.expiresAt lessEq LocalDateTime.now()
        }
    }
}
