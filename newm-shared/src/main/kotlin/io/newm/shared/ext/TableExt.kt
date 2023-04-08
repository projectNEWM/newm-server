package io.newm.shared.ext

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select

fun Table.firstOrNull(where: SqlExpressionBuilder.() -> Op<Boolean>): ResultRow? =
    select(where).limit(1).firstOrNull()

fun Table.exists(where: SqlExpressionBuilder.() -> Op<Boolean>): Boolean = !select(where).empty()

fun <T : Comparable<T>> IdTable<T>.firstHavingIdOrNull(id: T): ResultRow? =
    this.firstOrNull { this@firstHavingIdOrNull.id eq id }

fun <T : Comparable<T>> IdTable<T>.existsHavingId(id: T): Boolean = exists { this@existsHavingId.id eq id }

fun <T : Comparable<T>> IdTable<T>.getId(where: SqlExpressionBuilder.() -> Op<Boolean>): T? =
    firstOrNull(where)?.get(id)?.value
