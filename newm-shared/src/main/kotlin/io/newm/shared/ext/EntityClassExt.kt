package io.newm.shared.ext

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.exists(
    op: SqlExpressionBuilder.() -> Op<Boolean>
): Boolean = table.exists(op)

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.existsHavingId(id: ID): Boolean = table.existsHavingId(id)
