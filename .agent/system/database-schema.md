# 📊 Database Schema Reference

> **Source of truth for database entities and relationships.**

This document describes the database schema for the NEWM Server platform. The persistence layer uses **Exposed ORM** with **PostgreSQL**.

---

## Entity Overview

The database is organized into feature domains:

| Domain | Tables | Description |
|--------|--------|-------------|
| **Users** | `users` | User accounts and profiles |
| **Songs** | `songs`, `song_receipts` | Music metadata and blockchain receipts |
| **Collaborations** | `collaborations` | Artist collaboration agreements |
| **Earnings** | `earnings`, `earnings_claims` | Artist royalty earnings |
| **Distribution** | `distribution_*` | Music distribution to DSPs |
| **Minting** | `pending_songs` | NFT minting queue |
| **Wallet** | `wallet_connections` | Cardano wallet links |
| **Marketplace** | `marketplace_*` | NFT marketplace data |

---

## Exposed ORM Patterns

### Table Definition

```kotlin
// Location: io.newm.server.features/{feature}/database/{Feature}Table.kt

object UserTable : UUIDTable(name = "users") {
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val email = text("email")
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val walletAddress = text("wallet_address").nullable()
    val verificationStatus = enumeration("verification_status", UserVerificationStatus::class)
}
```

### Entity Class

```kotlin
// Location: io.newm.server.features/{feature}/database/{Feature}Entity.kt

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)
    
    var createdAt by UserTable.createdAt
    var email by UserTable.email
    var firstName by UserTable.firstName
    var lastName by UserTable.lastName
    var walletAddress by UserTable.walletAddress
    var verificationStatus by UserTable.verificationStatus
    
    fun toModel() = User(
        id = id.value,
        email = email,
        firstName = firstName,
        // ...
    )
}
```

---

## Key Entities

### Users

```kotlin
object UserTable : UUIDTable(name = "users") {
    val email: Column<String>
    val firstName: Column<String?>
    val lastName: Column<String?>
    val walletAddress: Column<String?>
    val verificationStatus: Column<UserVerificationStatus>
    val oauthType: Column<OAuthType?>
    val oauthId: Column<String?>
    val createdAt: Column<LocalDateTime>
}
```

### Songs

```kotlin
object SongTable : UUIDTable(name = "songs") {
    val ownerId: Column<EntityID<UUID>>  // FK to users
    val title: Column<String>
    val genres: Column<List<String>>     // JSON array
    val mintingStatus: Column<MintingStatus>
    val arweaveClipUrl: Column<String?>
    val createdAt: Column<LocalDateTime>
}
```

---

## Database Conventions

### Naming
- **Tables**: `snake_case`, plural (e.g., `users`, `wallet_connections`)
- **Columns**: `snake_case` (e.g., `created_at`, `wallet_address`)
- **Foreign Keys**: `{table_singular}_id` (e.g., `user_id`, `song_id`)

### Common Columns
- `id` — UUID primary key (auto-generated)
- `created_at` — Creation timestamp

### Transactions

```kotlin
// Use transaction{} block for database operations
suspend fun getUser(userId: UUID): User = transaction {
    UserEntity.findById(userId)?.toModel() ?: throw NotFoundException()
}

// Suspending transaction for coroutine context
suspend fun addUser(user: User): UUID = newSuspendedTransaction {
    UserEntity.new {
        email = user.email
        firstName = user.firstName
    }.id.value
}
```

---

## Repository Pattern

```kotlin
// Interface
interface UserRepository {
    suspend fun add(user: User, clientPlatform: ClientPlatform?): UserId
    suspend fun get(userId: UserId): User
    suspend fun findByEmail(email: String): User
    suspend fun update(userId: UserId, user: User)
    suspend fun delete(userId: UserId)
}

// Implementation
class UserRepositoryImpl : UserRepository {
    override suspend fun get(userId: UserId): User = transaction {
        UserEntity.findById(userId)?.toModel() 
            ?: throw HttpUnprocessableEntityException("User not found")
    }
}
```

---

## Migrations

Flyway is used for database migrations.

### Migration Location
```
newm-server/src/main/resources/db/migration/
```

### Naming Convention
```
V{version}__{description}.sql
Example: V1__create_users_table.sql
```

---

## Relationships

```
┌───────────────┐       ┌───────────────┐
│    users      │       │    songs      │
├───────────────┤       ├───────────────┤
│ id (UUID)     │◄──────│ owner_id      │
│ email         │       │ title         │
│ wallet_addr   │       │ minting_status│
└───────────────┘       └───────┬───────┘
        │                       │
        │                       │
        ▼                       ▼
┌───────────────────┐   ┌───────────────────┐
│collaborations     │   │   earnings        │
├───────────────────┤   ├───────────────────┤
│ song_id           │   │ song_id           │
│ user_id           │   │ amount            │
│ royalty_rate      │   │ claimed_at        │
└───────────────────┘   └───────────────────┘
```
