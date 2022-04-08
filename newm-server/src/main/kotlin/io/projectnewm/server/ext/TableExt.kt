package io.projectnewm.server.ext

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import java.util.UUID

fun Table.firstOrNull(where: SqlExpressionBuilder.() -> Op<Boolean>): ResultRow? =
    select(where).limit(1).firstOrNull()

fun Table.exists(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean = !select(where).empty()

fun IdTable<UUID>.firstOrNull(id: UUID): ResultRow? = firstOrNull { this@firstOrNull.id eq id }

fun IdTable<UUID>.exists(id: UUID): Boolean = exists { this@exists.id eq id }

fun IdTable<UUID>.getId(where: SqlExpressionBuilder.() -> Op<Boolean>): UUID? = firstOrNull(where)?.get(id)?.value
