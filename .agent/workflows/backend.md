---
description: Backend Kotlin development for API, database, and architecture
---

# Backend Development Workflow

This workflow guides development on the Kotlin backend modules including API, database, and architecture components.

## Overview

The backend uses:
- **Kotlin 2.x** — Primary language (running on Java 21)
- **Ktor** — Web framework
- **Exposed** — Kotlin SQL framework (ORM)
- **Koin** — Dependency injection
- **PostgreSQL** — Database
- **Gradle** — Build system (Kotlin DSL)

---

## Module Structure

| Module | Package | Purpose |
|--------|---------|---------|
| `newm-server` | `io.newm.server` | REST API, Ktor routes, business logic |
| `newm-shared` | `io.newm.shared` | Shared utilities, serialization |
| `newm-chain` | `io.newm.chain` | Cardano gRPC interface |
| `newm-tx-builder` | `io.newm.txbuilder` | Transaction building |

---

## Quick Commands

### Build

// turbo
```bash
./gradlew build
```

### Test

// turbo
```bash
./gradlew test
```

### Single Module Test

```bash
./gradlew :newm-server:test
./gradlew :newm-chain:test
```

### Lint (ktlint)

// turbo
```bash
./gradlew ktlintCheck
./gradlew ktlintFormat  # Auto-fix
```

---

## Architecture Patterns

### Layer Architecture

```
┌─────────────────────────────────────────┐
│             Routes Layer                │
│      (Ktor routes, endpoints)           │
├─────────────────────────────────────────┤
│          Repository Layer               │
│    (Business logic, data access)        │
├─────────────────────────────────────────┤
│            Table/Entity Layer           │
│     (Exposed Tables and Entities)       │
└─────────────────────────────────────────┘
```

### Ktor Routes Pattern

```kotlin
// Location: io.newm.server.features/{feature}/{Feature}Routes.kt

fun Route.songRoutes() {
    authenticate(AUTH_JWT) {
        route("/v1/songs") {
            get {
                val songs = get<SongRepository>().getAll(filters, offset, limit)
                call.respond(songs)
            }
            
            post {
                val request = call.receive<CreateSongRequest>()
                val songId = get<SongRepository>().add(request)
                call.respond(HttpStatusCode.Created, SongIdBody(songId))
            }
            
            route("/{id}") {
                get {
                    val songId = call.parameters["id"]?.toUUID()
                    val song = get<SongRepository>().get(songId)
                    call.respond(song)
                }
            }
        }
    }
}
```

### Koin Module Pattern

```kotlin
// Location: io.newm.server.features/{feature}/{Feature}KoinModule.kt

val songKoinModule = module {
    single<SongRepository> { SongRepositoryImpl() }
}
```

### Repository Pattern

```kotlin
// Interface
interface SongRepository {
    suspend fun add(song: Song): SongId
    suspend fun get(songId: SongId): Song
    suspend fun getAll(filters: SongFilters, offset: Int, limit: Int): List<Song>
    suspend fun update(songId: SongId, song: Song)
    suspend fun delete(songId: SongId)
}

// Implementation
class SongRepositoryImpl : SongRepository {
    override suspend fun get(songId: SongId): Song = transaction {
        SongEntity.findById(songId)?.toModel()
            ?: throw HttpUnprocessableEntityException("Song not found")
    }
}
```

---

## Database Development

### Creating Tables (Exposed)

```kotlin
// Location: io.newm.server.features/{feature}/database/{Feature}Table.kt

object SongTable : UUIDTable(name = "songs") {
    val title = text("title")
    val ownerId = reference("owner_id", UserTable)
    val genres = text("genres")  // JSON serialized
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
```

### Creating Entities

```kotlin
// Location: io.newm.server.features/{feature}/database/{Feature}Entity.kt

class SongEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SongEntity>(SongTable)
    
    var title by SongTable.title
    var owner by UserEntity referencedOn SongTable.ownerId
    var genres by SongTable.genres
    var createdAt by SongTable.createdAt
    
    fun toModel() = Song(
        id = id.value,
        title = title,
        ownerId = owner.id.value,
        genres = Json.decodeFromString(genres)
    )
}
```

### Database Migrations

See [/database workflow](./database.md) for Flyway migration details.

---

## API Development

### Request/Response Models

```kotlin
// Location: io.newm.server.features/{feature}/model/

@Serializable
data class Song(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val title: String,
    val genres: List<String> = emptyList()
)

@Serializable
data class CreateSongRequest(
    val title: String,
    val genres: List<String> = emptyList()
)
```

---

## Testing

### Unit Tests

```kotlin
class SongRepositoryTest {
    private val songRepository = mockk<SongRepository>()
    
    @Test
    fun `get returns song when exists`() = runTest {
        val songId = UUID.randomUUID()
        val song = Song(id = songId, title = "Test Song")
        
        coEvery { songRepository.get(songId) } returns song
        
        val result = songRepository.get(songId)
        
        assertThat(result.title).isEqualTo("Test Song")
    }
}
```

### Integration Tests

```bash
./gradlew integTestGarage   # Test against Garage
./gradlew integTestStudio   # Test against Studio
```

---

## Common Patterns

### Null Safety

```kotlin
// Use Kotlin null-safe operators
fun findSong(id: UUID): Song? {
    return songRepository.get(id)
}

// Elvis operator for defaults
val title = song?.title ?: "Untitled"
```

### Transactions

```kotlin
// Use transaction{} for database operations
suspend fun createSong(song: Song): UUID = transaction {
    SongEntity.new {
        title = song.title
        owner = UserEntity[song.ownerId]
    }.id.value
}
```

---

## Troubleshooting

### Build Failures

**Problem:** Gradle build fails  
**Solution:** 
```bash
./gradlew clean build --refresh-dependencies
```

### Lazy Loading Issues

**Problem:** Entity relationships not loaded  
**Solution:** Access relationships within `transaction {}` block:
```kotlin
transaction {
    val song = SongEntity.findById(id)
    song?.owner?.email  // Access inside transaction
}
```
