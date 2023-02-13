package io.newm.chain.database.repository

import io.newm.chain.database.entity.User

interface UsersRepository {
    fun get(userId: Long): User?
    fun getByName(name: String): User?
    fun insert(user: User): Long
    fun update(user: User): Int
}
