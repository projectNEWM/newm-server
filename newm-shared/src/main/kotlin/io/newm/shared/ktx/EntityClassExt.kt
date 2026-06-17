package io.newm.shared.ktx

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.exists(op: () -> Op<Boolean>): Boolean = table.exists(op)

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.existsHavingId(id: ID): Boolean = table.existsHavingId(id)
