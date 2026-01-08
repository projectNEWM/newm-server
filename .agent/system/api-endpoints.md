# 🔌 API Endpoints Reference

> **Source of truth for REST API contracts.**

This document describes the REST API endpoints exposed by the `newm-server` module.

---

## API Overview

| Base URL | Environment |
|----------|-------------|
| `http://localhost:3737` | Local Development |
| `https://garage.newm.io` | Garage (Dev) |
| `https://studio.newm.io` | Studio (Production) |

### Documentation
- **OpenAPI/Swagger**: [https://garage.newm.io/openapi/](https://garage.newm.io/openapi/)

### Authentication
Protected endpoints require a valid JWT token:
```
Authorization: Bearer <jwt_token>
```

---

## Endpoint Categories

| Category | Base Path | Description |
|----------|-----------|-------------|
| **Users** | `/v1/users` | User profile management |
| **Songs** | `/v1/songs` | Song metadata and management |
| **Earnings** | `/v1/earnings` | Artist earnings |
| **Earnings (Admin)** | `/v1/earnings/admin` | Admin earnings management |
| **Distribution** | `/v1/distribution` | DSP distribution |
| **Cardano** | `/v1/cardano` | Wallet and blockchain operations |
| **Collaborations** | `/v1/collaborations` | Artist collaborations |
| **Playlists** | `/v1/playlists` | Playlist management |
| **Marketplace** | `/v1/marketplace` | NFT marketplace |

---

## Key Endpoints

### Users

#### GET `/v1/users/me`
Get current user profile.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "id": "uuid",
  "email": "artist@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "walletAddress": "addr1...",
  "verificationStatus": "Verified"
}
```

#### PATCH `/v1/users/me`
Update user profile.

---

### Songs

#### GET `/v1/songs`
List songs with pagination and filters.

**Query Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `offset` | int | Pagination offset |
| `limit` | int | Items per page |
| `ownerIds` | string[] | Filter by owner |
| `genres` | string[] | Filter by genre |

#### POST `/v1/songs`
Create a new song.

**Request:**
```json
{
  "title": "My Song",
  "genres": ["Pop", "Electronic"],
  "description": "Song description"
}
```

#### GET `/v1/songs/{id}`
Get song details.

#### PUT `/v1/songs/{id}`
Update song metadata.

#### DELETE `/v1/songs/{id}`
Delete a song.

---

### Earnings

#### GET `/v1/earnings/{walletAddress}`
Get earnings for a wallet address (requires recaptcha verification).

#### GET `/v1/earnings/song/{songId}`
Get earnings by song ID with optional date range filters.

**Query Parameters:**
| Param | Type | Description |
|-------|------|-------------|
| `startDate` | datetime | Filter earnings from this date |
| `endDate` | datetime | Filter earnings until this date |

#### POST `/v1/earnings`
Create a claim order for all unclaimed earnings on a wallet.

### Earnings (Admin)

> **Auth:** Requires `AUTH_JWT_ADMIN` (admin-only endpoints)

#### GET `/v1/earnings/admin`
Fetch all earnings records.

#### POST `/v1/earnings/admin`
Batch create earnings records.

**Request:** `List<Earning>`

#### DELETE `/v1/earnings/admin`
Delete earnings by IDs.

**Request:**
```json
["uuid1", "uuid2", "uuid3"]
```

**Response:** `204 No Content`

#### GET `/v1/earnings/admin/{songIdOrIsrc}`
Get earnings by song ID or ISRC.

**Path Parameters:**
- `songIdOrIsrc` — UUID or ISRC format (e.g., `IE-LOI-23-01693`)

#### POST `/v1/earnings/admin/{songIdOrIsrc}`
Create royalty splits for a song.

**Request:**
```json
{
  "usdAmount": 10500000
}
```
> Amount is in 6-decimal format (10.50 USD = 10500000)

---

### Cardano

#### GET `/v1/cardano/wallet`
Get wallet information.

#### POST `/v1/cardano/wallet/connect`
Connect a Cardano wallet.

---

## Authentication Flow

1. **OAuth Login** — `/v1/auth/{provider}` (Google, Apple, etc.)
2. **Email Login** — `/v1/auth/login` with email/password
3. **Receive JWT** — Token returned on successful auth
4. **Use Token** — Include in `Authorization` header

---

## Response Patterns

### Success
```json
{
  "id": "uuid",
  "property": "value"
}
```

### Collection with Pagination
```json
{
  "items": [...],
  "offset": 0,
  "limit": 20,
  "total": 100
}
```

### Errors
```json
{
  "code": 400,
  "message": "Validation error",
  "details": {}
}
```

---

## Notes

- All IDs are UUIDs
- Timestamps are ISO 8601 format
- See OpenAPI docs for complete schema definitions
