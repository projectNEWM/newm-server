# 🤖 NEWM Server AI Agent Instructions

> **For all AI coding assistants.** This file provides a starting point for understanding the NEWM Server codebase.

---

## Quick Start

**Read the full documentation:** [`.agent/readme.md`](./.agent/readme.md)

The `.agent/` directory contains comprehensive documentation organized for AI agents:

| Folder | Purpose | When to Read |
|--------|---------|--------------|
| `.agent/system/` | Architecture, schemas, APIs | Understanding system design |
| `.agent/task/` | Past PRDs and implementation plans | Before implementing new features |
| `.agent/SOPs/` | Standard operating procedures | When encountering known issues |
| `.agent/workflows/` | Step-by-step guides | When executing specific tasks |

---

## Available Workflows

Use these slash commands to access workflows:

| Command | Description |
|---------|-------------|
| `/build` | Build all project modules |
| `/test` | Run test suites |
| `/backend` | Backend Kotlin development |
| `/backend_dependencies` | Check and update dependencies |
| `/database` | Database migrations |
| `/kotlin_migration` | Java to Kotlin migration |
| `/update-doc` | Update this documentation |

---

## Project Overview

**NEWM Server** is the backend for the NEWM music platform on Cardano. It provides REST APIs for mobile apps and the artist portal, enabling artists to distribute, tokenize, and earn from their music.

### Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Kotlin 2.x on Java 21, Ktor |
| ORM | Exposed (Kotlin SQL framework) |
| DI | Koin |
| Database | PostgreSQL |
| Scheduling | Quartz |
| Blockchain | Cardano (via newm-chain gRPC) |
| Cloud | AWS (S3, SQS, KMS, Secrets Manager) |

### Key Modules

| Module | Responsibility |
|--------|----------------|
| `newm-server` | REST API, business logic, features |
| `newm-chain` | Cardano blockchain gRPC interface (Ogmios) |
| `newm-chain-db` | Blockchain data persistence |
| `newm-tx-builder` | Cardano transaction building |
| `newm-shared` | Shared utilities, serialization |

---

## Important Patterns

### Backend (Kotlin/Ktor)
- Feature-based packages in `io.newm.server.features`
- Repository interfaces with suspend functions
- Exposed `Table` objects + `Entity` classes for DB
- Ktor routing for REST endpoints
- Koin modules for dependency injection

### Database (Exposed ORM)
```kotlin
// Table definition
object UserTable : UUIDTable(name = "users") {
    val email = text("email")
    val firstName = text("first_name").nullable()
}

// Entity class
class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)
    var email by UserTable.email
}
```

### Ktor Routes
```kotlin
fun Route.songRoutes() {
    authenticate {
        route("/v1/songs") {
            get { /* list songs */ }
            post { /* create song */ }
        }
    }
}
```

---

## Before You Start

1. Read [`.agent/readme.md`](./.agent/readme.md) for full documentation index
2. Check [`.agent/system/architecture.md`](./.agent/system/architecture.md) for system overview
3. Review relevant workflow in [`.agent/workflows/`](./.agent/workflows/)

---

## Security Notes

- Never commit secrets or API keys
- Use AWS Secrets Manager for sensitive config
- JWT authentication via Ktor Auth plugin
