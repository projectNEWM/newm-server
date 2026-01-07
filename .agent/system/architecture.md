# 🏗️ NEWM Server System Architecture

> **Source of truth for system-wide architectural decisions.**

This document provides the definitive overview of the NEWM Server system architecture, module relationships, and core design patterns.

---

## System Overview

NEWM Server is the backend for the NEWM music platform on Cardano. It enables artists to distribute, tokenize, and earn from their music through NFTs and streaming royalties.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            NEWM PLATFORM                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐     REST API       ┌─────────────────────────────────────┐  │
│  │ Mobile Apps │◄──────────────────►│          newm-server                │  │
│  │   Portal    │                    │           (Ktor)                    │  │
│  └─────────────┘                    └───────────┬───────────────────────┬─┘  │
│                                                 │                       │    │
│  ┌─────────────┐     REST API                   │                       │    │
│  │ newm-admin  │◄───────────────────────────────┤                       │    │
│  │ (Rust/GPUI) │                                │                       │    │
│  └─────────────┘                    ┌───────────┼───────────────────────┤    │
│                                     │           │                       │    │
│                                     ▼           ▼                       ▼    │
│               ┌──────────────────┐   ┌─────────────────┐   ┌───────────────┐ │
│               │   PostgreSQL     │   │   newm-chain    │   │   AWS SDK     │ │
│               │   (Exposed ORM)  │   │    (gRPC)       │   │ (S3,SQS,KMS)  │ │
│               └──────────────────┘   └────────┬────────┘   └───────────────┘ │
│                                               │                              │
│                                               ▼                              │
│                                      ┌─────────────────┐                     │
│                                      │ Cardano (Ogmios)│                     │
│                                      └─────────────────┘                     │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Module Reference

### Backend Modules

| Module | Package | Responsibility |
|--------|---------|----------------|
| `newm-server` | `io.newm.server` | REST API, business logic, Ktor routes |
| `newm-shared` | `io.newm.shared` | Shared utilities, serialization, auth |
| `newm-chain` | `io.newm.chain` | Cardano gRPC server (Ogmios interface) |
| `newm-chain-db` | `io.newm.chain.database` | Blockchain data persistence |
| `newm-chain-grpc` | `io.newm.chain.grpc` | gRPC protocol definitions |
| `newm-tx-builder` | `io.newm.txbuilder` | Cardano transaction building |
| `newm-chain-util` | `io.newm.chain.util` | Chain utilities |
| `newm-objectpool` | `io.newm.objectpool` | Object pooling utilities |

### Desktop Applications

| Module | Language | Framework | Responsibility |
|--------|----------|-----------|----------------|
| `newm-admin` | Rust | GPUI + gpui-component | Admin desktop app for earnings/refunds management |

**newm-admin Details:**
- **UI Framework**: GPUI (GPU-accelerated, from Zed editor)
- **HTTP Client**: reqwest with async-compat bridge for GPUI async compatibility
- **Auth**: JWT-based, validates `admin: true` claim
- **Environments**: Garage (dev) / Studio (prod)
- **Platforms**: Linux, macOS, Windows

See [/admin workflow](../workflows/admin.md) for development commands.

### Feature Domains (in newm-server)

| Feature | Path | Responsibility |
|---------|------|----------------|
| `user` | `features/user/` | User accounts, OAuth, verification |
| `song` | `features/song/` | Song metadata, audio processing |
| `earnings` | `features/earnings/` | Artist earnings management |
| `distribution` | `features/distribution/` | Music distribution to DSPs |
| `minting` | `features/minting/` | NFT minting on Cardano |
| `collaboration` | `features/collaboration/` | Artist collaborations |
| `marketplace` | `features/marketplace/` | NFT marketplace |
| `playlist` | `features/playlist/` | Playlist management |
| `cardano` | `features/cardano/` | Cardano wallet operations |
| `walletconnection` | `features/walletconnection/` | Wallet linking |

---

## Core Design Patterns

### Ktor Routes

```kotlin
// Location: io.newm.server.features/{feature}/{Feature}Routes.kt

fun Route.songRoutes() {
    authenticate(AUTH_JWT) {
        route("/v1/songs") {
            get {
                val songs = songRepository.getAll(filters, offset, limit)
                call.respond(songs)
            }
            post {
                val request = call.receive<CreateSongRequest>()
                val songId = songRepository.add(request)
                call.respond(HttpStatusCode.Created, songId)
            }
        }
    }
}
```

### Koin Dependency Injection

```kotlin
// Location: io.newm.server.features/{feature}/{Feature}KoinModule.kt

val songKoinModule = module {
    single<SongRepository> { SongRepositoryImpl(get()) }
}

// Installation in Application.kt
install(Koin) {
    modules(songKoinModule, userKoinModule, /* ... */)
}
```

### Repository Pattern

```kotlin
// Interface
interface SongRepository {
    suspend fun add(song: Song): SongId
    suspend fun get(songId: SongId): Song
    suspend fun getAll(filters: SongFilters, offset: Int, limit: Int): List<Song>
}

// Implementation with Exposed ORM
class SongRepositoryImpl : SongRepository {
    override suspend fun get(songId: SongId): Song = transaction {
        SongEntity.findById(songId)?.toModel() ?: throw NotFoundException()
    }
}
```

### Exposed ORM Pattern

```kotlin
// Table definition
object SongTable : UUIDTable(name = "songs") {
    val title = text("title")
    val ownerId = reference("owner_id", UserTable)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

// Entity class
class SongEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SongEntity>(SongTable)
    var title by SongTable.title
    var owner by UserEntity referencedOn SongTable.ownerId
}
```

---

## Integration Points

### Cardano (newm-chain)
- **Interface**: gRPC via Ogmios
- **Purpose**: Query UTXOs, submit transactions, monitor chain
- **Port**: 3737 (default)

### AWS Services
- **S3**: Audio file storage
- **SQS**: Async job processing
- **KMS**: Key management
- **Secrets Manager**: Configuration secrets

### External APIs
- **Arweave**: Permanent audio storage
- **Cloudinary**: Image processing
- **NFTCDN**: NFT metadata serving
- **Idenfy**: KYC verification
- **PayPal**: Payment processing
- **DripDropz**: Token distribution

---

## Environment Configuration

### Environments
- `garage` — Development/testing
- `studio` — Production

### Required Configuration
See `.env.sample` for environment variables. Secrets are stored in AWS Secrets Manager.

---

## Build & Deploy Overview

See [`/workflows/build.md`](../workflows/build.md) for detailed build instructions.

### Quick Reference

```bash
# Full build
./gradlew build

# Fat JAR for deployment
./gradlew shadowJar

# Run locally
./gradlew :newm-server:run
```
