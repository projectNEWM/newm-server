package io.newm.shared.ktx

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll

fun Table.firstOrNull(where: () -> Op<Boolean>): ResultRow? = selectAll().where(where).limit(1).firstOrNull()

fun Table.exists(where: () -> Op<Boolean>): Boolean = firstOrNull(where) != null

fun <T : Comparable<T>> IdTable<T>.firstHavingIdOrNull(id: T): ResultRow? = this.firstOrNull { this@firstHavingIdOrNull.id eq id }

fun <T : Comparable<T>> IdTable<T>.existsHavingId(id: T): Boolean = exists { this@existsHavingId.id eq id }

fun <T : Comparable<T>> IdTable<T>.getId(where: () -> Op<Boolean>): T? = firstOrNull(where)?.get(id)?.value
