package io.projectnewm.server.ext

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import java.util.*

fun Table.firstOrNull(where: SqlExpressionBuilder.() -> Op<Boolean>): ResultRow? =
    select(where).limit(1).firstOrNull()

fun Table.exists(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean = !select(where).empty()

fun IdTable<UUID>.firstHavingIdOrNull(id: UUID): ResultRow? = this.firstOrNull { this@firstHavingIdOrNull.id eq id }

fun IdTable<UUID>.existsHavingId(id: UUID): Boolean = exists { this@existsHavingId.id eq id }

fun IdTable<UUID>.getId(where: SqlExpressionBuilder.() -> Op<Boolean>): UUID? = firstOrNull(where)?.get(id)?.value
