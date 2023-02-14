package io.newm.chain.database.repository

import com.github.benmanes.caffeine.cache.Caffeine
import io.newm.chain.database.entity.User
import io.newm.chain.database.table.UsersTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration

class UsersRepositoryImpl : UsersRepository {

    private val usersByIdCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<Long, User?> { userId ->
            transaction {
                UsersTable.select { UsersTable.id eq userId }.firstOrNull()?.let { row ->
                    User(
                        id = row[UsersTable.id].value,
                        name = row[UsersTable.name],
                    )
                }
            }
        }

    override fun get(userId: Long): User? = usersByIdCache[userId]

    override fun getByName(name: String): User? {
        return transaction {
            UsersTable.select { UsersTable.name eq name }.firstOrNull()?.let { row ->
                User(
                    id = row[UsersTable.id].value,
                    name = row[UsersTable.name],
                )
            }
        }
    }

    override fun insert(user: User): Long {
        return transaction {
            UsersTable.insertAndGetId { row ->
                row[name] = user.name
            }.value.also { usersByIdCache.invalidate(it) }
        }
    }

    override fun update(user: User): Int {
        return transaction {
            UsersTable.update({ UsersTable.id eq user.id!! }) { row ->
                row[name] = user.name
            }.also { usersByIdCache.invalidate(user.id) }
        }
    }
}
