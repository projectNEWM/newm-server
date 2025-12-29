# Claude Code Instructions

> **For Claude Code.** This file points to the centralized documentation in `.agent/`.

---

## Documentation Location

All agent documentation is centralized in the [`.agent/`](./.agent/) directory.

**Start here:** [`.agent/readme.md`](./.agent/readme.md)

---

## Quick Reference

### System Documentation
- [Architecture](./.agent/system/architecture.md) — System overview, modules, patterns
- [Database Schema](./.agent/system/database-schema.md) — Entities, relationships
- [API Endpoints](./.agent/system/api-endpoints.md) — REST API contracts

### Workflows
- [/build](./.agent/workflows/build.md) — Build all modules
- [/test](./.agent/workflows/test.md) — Run tests
- [/backend](./.agent/workflows/backend.md) — Backend Kotlin development
- [/backend_dependencies](./.agent/workflows/backend_dependencies.md) — Dependency updates
- [/database](./.agent/workflows/database.md) — Database operations
- [/kotlin_migration](./.agent/workflows/kotlin_migration.md) — Java to Kotlin migration
- [/update-doc](./.agent/workflows/update-doc.md) — Update documentation

### Learning Resources
- [Task History](./.agent/task/) — Past implementation plans
- [SOPs](./.agent/SOPs/) — Standard operating procedures

---

## Common Commands

### Build
```bash
./gradlew build                    # Build all modules
./gradlew shadowJar                # Build fat JAR
./gradlew :newm-server:build       # Build specific module
```

### Test
```bash
./gradlew test                     # Run all tests
./gradlew :newm-server:test        # Test specific module
./gradlew integTestGarage          # Integration tests (Garage)
./gradlew integTestStudio          # Integration tests (Studio)
```

### Lint
```bash
./gradlew ktlintCheck              # Check code style
./gradlew ktlintFormat             # Auto-fix formatting
```

---

## Project Structure

```
newm-server/
├── .agent/                 # 📚 Agent documentation (READ THIS)
├── newm-server/            # Kotlin REST API (Ktor)
├── newm-chain/             # Cardano gRPC interface (Ogmios)
├── newm-chain-db/          # Blockchain data persistence
├── newm-tx-builder/        # Transaction building
├── newm-shared/            # Shared utilities
└── newm-chain-util/        # Chain utilities
```

---

## Creating Documentation

When completing features or resolving issues:

1. **Implementation plans** → Save to `.agent/task/{domain}/`
2. **Resolved issues** → Create SOP in `.agent/SOPs/{category}/`
3. **New workflows** → Add to `.agent/workflows/`

Run `/update-doc` workflow for guidance.
