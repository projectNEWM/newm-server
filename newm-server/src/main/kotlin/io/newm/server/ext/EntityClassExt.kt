package io.projectnewm.server.ext

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import java.util.UUID

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.existsHavingId(
    op: SqlExpressionBuilder.() -> Op<Boolean>
): Boolean = table.exists(op)

fun <T : UUIDEntity> UUIDEntityClass<T>.existsHavingId(id: UUID): Boolean = table.existsHavingId(id)
