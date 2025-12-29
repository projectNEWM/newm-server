# 📚 NEWM Server Agent Documentation Index

> **For AI Agents:** Read this file first to understand available documentation and when to reference each resource.

This folder contains all the documentation needed for AI agents to effectively assist with development on the NEWM Server project. Use this index to quickly navigate to the relevant documentation for your current task.

---

## 🗺️ Quick Navigation

| Folder | Purpose | When to Read |
|--------|---------|--------------|
| [`/system`](./system/) | Architecture, schemas, API endpoints | **First**, for any architectural decisions or understanding system design |
| [`/task`](./task/) | PRDs and implementation plans history | When implementing new features similar to past work |
| [`/SOPs`](./SOPs/) | Standard Operating Procedures | When encountering known issues or following established patterns |
| [`/workflows`](./workflows/) | Step-by-step development workflows | When executing specific development tasks |

---

## 📁 Folder Details

### `/system` — Architecture & Schemas

**The source of truth for major architectural decisions.**

Read these files to understand:
- Overall system architecture and component relationships
- Database schemas and entity relationships
- API endpoints and contracts
- Integration patterns with external services

Files:
- `architecture.md` — System overview, module dependencies, data flow
- `database-schema.md` — Database entities, relationships, Exposed ORM patterns
- `api-endpoints.md` — REST endpoints, request/response formats

---

### `/task` — Implementation History

**Successful PRDs and implementation plans for reference.**

Before implementing a feature:
1. Check if a similar feature was implemented before
2. Use past plans as templates for consistency
3. Follow established patterns from successful implementations

This folder uses subdirectories per major feature/task.

---

### `/SOPs` — Standard Operating Procedures

**Learnings from resolved issues and best practices.**

When an issue is resolved or a complex integration succeeds:
1. Document the step-by-step solution
2. Include common pitfalls and how to avoid them
3. Reference related code or configuration

**To create a new SOP**, ask the agent:
> "Generate SOP for [task/integration name]"

---

### `/workflows` — Development Workflows

**Step-by-step guides for common development tasks.**

Available workflows:

| Workflow | Description | Trigger |
|----------|-------------|---------|
| [`build.md`](./workflows/build.md) | Build all modules | `/build` |
| [`test.md`](./workflows/test.md) | Run test suites | `/test` |
| [`backend.md`](./workflows/backend.md) | Backend Kotlin development | `/backend` |
| [`backend_dependencies.md`](./workflows/backend_dependencies.md) | Check and update dependencies | `/backend_dependencies` |
| [`database.md`](./workflows/database.md) | Database migrations | `/database` |
| [`kotlin_migration.md`](./workflows/kotlin_migration.md) | Java to Kotlin migration | `/kotlin_migration` |
| [`update-doc.md`](./workflows/update-doc.md) | Update documentation | `/update-doc` |

---

## 🏗️ Project Overview

**NEWM Server** is the backend for the NEWM music platform on Cardano. It provides REST APIs for mobile apps and the artist portal, enabling artists to distribute, tokenize, and earn from their music. The code is 100% Kotlin.

### Technology Stack

| Layer | Technology | Key Modules |
|-------|------------|-------------|
| **Backend** | Kotlin 2.x on Java 21, Ktor | `newm-server` |
| **ORM** | Exposed (Kotlin SQL framework) | Tables in `features/*/database/` |
| **DI** | Koin | Modules in `*KoinModule.kt` |
| **Database** | PostgreSQL | Flyway migrations |
| **Scheduling** | Quartz | `features/scheduler/` |
| **Blockchain** | Cardano (gRPC via Ogmios) | `newm-chain`, `newm-tx-builder` |
| **Cloud** | AWS (S3, SQS, KMS, Secrets Manager) | AWS SDK |

### Module Dependency Overview

```
newm-server (REST API)
├── newm-shared (Utilities, serialization)
├── newm-chain-grpc (Blockchain gRPC client)
├── newm-chain-util (Chain utilities)
└── newm-tx-builder (Transaction building)

newm-chain (Blockchain Indexer)
├── newm-chain-db (Persistence)
├── newm-chain-grpc (gRPC definitions)
└── Ogmios (External Cardano interface)
```

### Feature Domains

The `io.newm.server.features` package contains:
- `user` — User accounts, OAuth, verification
- `song` — Song metadata, audio processing
- `earnings` — Artist earnings management
- `distribution` — Music distribution to DSPs
- `minting` — NFT minting on Cardano
- `collaboration` — Artist collaborations
- `marketplace` — NFT marketplace
- `playlist` — Playlist management
- `cardano` — Cardano wallet operations
- `walletconnection` — Wallet linking

---

## ⚡ Quick Commands

### Build & Test
```bash
# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run specific module tests
./gradlew :newm-server:test

# Integration tests
./gradlew integTestGarage
./gradlew integTestStudio
```

### Code Quality
```bash
# Lint check
./gradlew ktlintCheck

# Auto-fix formatting
./gradlew ktlintFormat
```

---

## 🔐 Security Notes

- Never commit secrets or API keys
- Use AWS Secrets Manager for sensitive configuration
- JWT authentication via Ktor Auth plugin
- See `.env.sample` for required environment variables
