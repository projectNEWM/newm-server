---
description: Database migrations and persistence layer workflow
---

# Database Workflow

Guide for database operations, migrations, and persistence layer development.

## Overview

The NEWM Server uses:
- **Exposed ORM** — Kotlin SQL framework
- **PostgreSQL** — Database
- **Flyway** — Database migrations

---

## Quick Setup

### Local Development

Docker Compose is available in `newm-server/`:

// turbo
```bash
cd newm-server
docker-compose up -d
```

This starts PostgreSQL on port 5432.

---

## Schema Development

### Creating a New Table

1. Create Table object:

```kotlin
// io.newm.server.features/{feature}/database/{Feature}Table.kt

object MyFeatureTable : UUIDTable(name = "my_features") {
    val name = text("name")
    val userId = reference("user_id", UserTable)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
```

2. Create Entity class:

```kotlin
// io.newm.server.features/{feature}/database/{Feature}Entity.kt

class MyFeatureEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<MyFeatureEntity>(MyFeatureTable)
    
    var name by MyFeatureTable.name
    var user by UserEntity referencedOn MyFeatureTable.userId
    var createdAt by MyFeatureTable.createdAt
    
    fun toModel() = MyFeature(
        id = id.value,
        name = name,
        userId = user.id.value
    )
}
```

3. Register table in database initialization (if needed).

---

## Migrations

### Location
```
newm-server/src/main/resources/db/migration/
```

### Creating a Migration

```sql
-- V{next_version}__description.sql

CREATE TABLE my_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_my_features_user_id ON my_features(user_id);
```

### Naming Convention
```
V1__initial_schema.sql
V2__add_songs_table.sql
V3__add_earnings_indexes.sql
```

---

## Queries

### Basic Operations

```kotlin
// Find by ID
val entity = MyFeatureEntity.findById(id)

// Find with filter
val entities = MyFeatureEntity.find { MyFeatureTable.name eq "test" }

// Create
val newEntity = MyFeatureEntity.new {
    name = "New Feature"
    user = UserEntity[userId]
}

// Update
entity.name = "Updated Name"

// Delete
entity.delete()
```

### Transactions

```kotlin
suspend fun createFeature(request: CreateRequest): UUID = transaction {
    MyFeatureEntity.new {
        name = request.name
        user = UserEntity[request.userId]
    }.id.value
}
```

---

## Troubleshooting

### Migration Failed

Check Flyway history:
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
```

### Reset Local Database

```bash
cd newm-server
docker-compose down -v
docker-compose up -d
```
