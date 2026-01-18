# ArDrive Turbo Kotlin SDK

## Overview

This PRD outlines the development of a new Kotlin module called `ardrive-turbo-kotlin` that provides a complete SDK for interacting with the ArDrive Turbo API. The SDK will replace the current AWS Lambda-based Bundlr/Irys upload system in `ArweaveRepositoryImpl.kt` with a native Kotlin implementation that offers:

- Full API coverage for ArDrive Turbo Upload and Payment services
- Built-in error handling and automatic retries
- Kotlin coroutines support
- Idiomatic Kotlin API design
- Type-safe request/response models

---

## Background

### Current System

The existing implementation in [ArweaveRepositoryImpl.kt](file:///home/westbam/Development/newm-server/newm-server/src/main/kotlin/io/newm/server/features/arweave/repo/ArweaveRepositoryImpl.kt) uses:

1. **arweave4s** - A Scala library for direct Arweave interactions (wallet management, balance checking)
2. **AWS Lambda** - External Lambda function that handles uploads via Bundlr/Irys network
3. **Arweave wallet (JWK format)** - For signing transactions and topping up credits

Key current capabilities:
- `getWalletAddress()` - Get the Arweave wallet address
- `getWalletARBalance()` - Check AR token balance
- `uploadSongAssets()` - Upload song assets (cover art, audio clips, lyrics, token agreements) via Lambda

### ArDrive Turbo

ArDrive Turbo is a high-performance upload and payment service for Arweave that offers:
- **Fast uploads** with instant confirmation
- **Bundled transactions** (ANS-104 data items)
- **Multi-part uploads** for large files
- **Flexible payment options** including AR tokens, fiat, and other cryptocurrencies
- **Credit sharing** between wallets

---

## Gap Analysis: Bundlr/Irys vs ArDrive Turbo

| Feature | Current (Bundlr/Irys via Lambda) | ArDrive Turbo |
|---------|----------------------------------|---------------|
| Upload mechanism | Lambda → Bundlr/Irys API | Direct API calls |
| AR token funding | ✅ Supported | ✅ Supported via `topUpWithTokens` |
| Multi-part uploads | Handled by Lambda | ✅ Native API support |
| Balance checking | Via arweave4s | ✅ Via `/balance` endpoint |
| Pricing calculation | Via arweave4s | ✅ Via `/price` endpoints |
| Transaction status | Via arweave4s | ✅ Via `/tx/:id/status` endpoint |
| Credit sharing | ❌ Not available | ✅ Full credit sharing support |
| File metadata/tags | ✅ Supported | ✅ Supported via data item opts |
| Retry logic | In Lambda | Built into SDK |
| Kotlin native | ❌ Scala interop | ✅ Native Kotlin |

### Payment Compatibility

> [!IMPORTANT]
> **AR Token Top-Up Confirmed**: ArDrive Turbo supports topping up credits using AR tokens via the `topUpWithTokens` method. This matches the current Bundlr/Irys funding mechanism using an Arweave wallet. The SDK will implement this to enable seamless migration.

Supported payment methods:
- **AR** (Arweave) - ✅ Same as current
- **ARIO** (AR.IO Network token)
- **ETH** (Ethereum)
- **SOL** (Solana)
- **POL/MATIC** (Polygon)
- **base-eth** (ETH on Base Network)
- **KYVE**
- **Fiat** via credit card (Stripe checkout)

---

## Requirements

### Functional Requirements

- [ ] Implement all ArDrive Turbo Upload Service API endpoints
- [ ] Implement all ArDrive Turbo Payment Service API endpoints
- [ ] Support Arweave JWK wallet authentication (matching current system)
- [ ] Provide idiomatic Kotlin coroutines suspend functions
- [ ] Include automatic retry logic with exponential backoff
- [ ] Support multi-part uploads for large files
- [ ] Enable AR token top-up for credits
- [ ] Provide type-safe request/response models

### Non-Functional Requirements

- [ ] High code coverage with unit tests
- [ ] Integration tests against Turbo API
- [ ] Comprehensive logging support
- [ ] Configurable timeout and retry policies
- [ ] Thread-safe for concurrent uploads

---

## Design Decisions

The following decisions have been made based on project requirements:

| Decision | Choice | Rationale |
|----------|--------|----------|
| **Wallet Format** | JWK only | Matches current system; wallet loaded via `environment.getSecureConfigString("arweave.walletJson")` |
| **Error Handling** | Sealed classes/interfaces | Idiomatic Kotlin, exhaustive when handling |
| **Logging** | kotlin-logging | Consistent with project standard |
| **Configuration** | Constructor injection | No DI framework in SDK; HOCON config read in `ArweaveRepositoryImpl`, values passed to SDK constructors |
| **Auto Top-Up** | Manual in SDK, auto in Repository | SDK provides `topUpWithTokens()`; `ArweaveRepositoryImpl` handles balance monitoring and auto top-up |
| **Cleanup Scope** | Immediate removal | Lambda function, Scala arweave4s code, and related dependencies removed as part of migration |

> [!IMPORTANT]
> The SDK module (`ardrive-turbo-kotlin`) must be **reusable outside this project**. It should:
> - Have no Koin/Spring DI dependencies
> - Accept all configuration via constructors
> - Be self-contained with minimal external dependencies

---

## Technical Design

### Module Structure

```
ardrive-turbo-kotlin/
├── build.gradle.kts
└── src/
    ├── main/kotlin/io/newm/turbo/
    │   ├── TurboClient.kt              # Main client interface
    │   ├── TurboClientImpl.kt          # Client implementation
    │   ├── TurboConfig.kt              # Configuration class (passed via constructor)
    │   ├── error/
    │   │   ├── TurboError.kt           # Sealed class hierarchy for errors
    │   │   └── TurboException.kt       # Exception wrappers
    │   ├── auth/
    │   │   ├── TurboSigner.kt          # Signing interface
    │   │   └── ArweaveSigner.kt        # Arweave JWK signer
    │   ├── upload/
    │   │   ├── UploadService.kt        # Upload operations
    │   │   ├── MultiPartUpload.kt      # Multi-part upload handling
    │   │   └── model/
    │   │       ├── UploadRequest.kt
    │   │       ├── UploadResponse.kt
    │   │       ├── DataItemOptions.kt
    │   │       ├── TransactionStatus.kt
    │   │       └── ServiceInfo.kt
    │   ├── payment/
    │   │   ├── PaymentService.kt       # Payment operations
    │   │   ├── TopUpService.kt         # Token top-up operations
    │   │   └── model/
    │   │       ├── Balance.kt
    │   │       ├── PriceQuote.kt
    │   │       ├── TopUpQuote.kt
    │   │       ├── Currency.kt
    │   │       ├── CreditApproval.kt
    │   │       └── PaymentInfo.kt
    │   ├── credit/
    │   │   ├── CreditSharingService.kt # Credit sharing operations
    │   │   └── model/
    │   │       └── CreditShareApproval.kt
    │   └── util/
    │       ├── RetryPolicy.kt          # Retry configuration
    │       ├── HttpClientFactory.kt    # Ktor client setup
    │       └── SignatureUtils.kt       # Request signing utilities
    └── test/kotlin/io/newm/turbo/
        ├── TurboClientTest.kt
        ├── UploadServiceTest.kt
        ├── PaymentServiceTest.kt
        └── integration/
            └── TurboIntegrationTest.kt
```

### Dependencies

```kotlin
// build.gradle.kts
dependencies {
    // Kotlin
    implementation(Dependencies.Kotlin.STDLIB_JDK8)
    implementation(Dependencies.KotlinLogging.ALL)
    
    // Coroutines
    implementation(Dependencies.Coroutines.CORE)
    
    // Ktor Client
    implementation(Dependencies.Ktor.CLIENT_CORE)
    implementation(Dependencies.Ktor.CLIENT_CIO)
    implementation(Dependencies.Ktor.CLIENT_CONTENT_NEGOTIATION)
    implementation(Dependencies.Ktor.SERIALIZATION_JSON)
    implementation(Dependencies.Ktor.CLIENT_LOGGING)
    
    // Serialization
    implementation(Dependencies.KotlinXSerialization.JSON)
    
    // Crypto (for signing)
    // Using existing or new crypto dependencies for JWK handling
    
    // Testing
    testImplementation(Dependencies.JUnit.BOM)
    testImplementation(Dependencies.Mockk.MOCKK)
    testImplementation(Dependencies.GoogleTruth.TRUTH)
    testImplementation(Dependencies.Coroutines.TEST)
    testImplementation(Dependencies.Ktor.CLIENT_MOCK)
}
```

---

## API Mapping

### Upload Service Endpoints

| Endpoint | Method | SDK Method |
|----------|--------|------------|
| `POST /v1/tx` | Upload signed data item | `uploadDataItem()` |
| `POST /v1/tx/{token}` | Upload for specific token | `uploadDataItem(token)` |
| `POST /v1/chunks/{token}` | Create multi-part upload | `createMultiPartUpload()` |
| `GET /v1/chunks/{token}/{uploadId}` | Get multi-part upload | `getMultiPartUpload()` |
| `POST /v1/chunks/{token}/{uploadId}` | Finalize multi-part upload | `finalizeMultiPartUpload()` |
| `POST /v1/chunks/{token}/{uploadId}/async` | Finalize async | `finalizeMultiPartUploadAsync()` |
| `GET /v1/chunks/{token}/{uploadId}/status` | Get upload status | `getMultiPartUploadStatus()` |
| `POST /v1/chunks/{token}/{uploadId}/{chunkOffset}` | Post chunk | `uploadChunk()` |
| `GET /v1/account/balance/{id}` | Get account balance | `getAccountBalance()` |
| `GET /v1/tx/{id}/status` | Get transaction status | `getTransactionStatus()` |
| `GET /v1/price/{token}/{byteCount}` | Get upload price | `getUploadPrice()` |
| `GET /v1/info` | Get service info | `getServiceInfo()` |

### Payment Service Endpoints

| Endpoint | Method | SDK Method |
|----------|--------|------------|
| `GET /v1/balance` | Get winc balance | `getBalance()` |
| `GET /v1/price/bytes/{byteCount}` | Get credits for bytes | `getPriceForBytes()` |
| `GET /v1/price/{type}/{amount}` | Get winc for payment | `getPriceForPayment()` |
| `GET /v1/top-up/{method}/{address}/{currency}/{amount}` | Get top-up quote | `getTopUpQuote()` |
| `POST /v1/account/balance/{token}` | Post pending payment | `submitPendingPayment()` |
| `GET /v1/currencies` | Get supported currencies | `getSupportedCurrencies()` |
| `GET /v1/countries` | Get supported countries | `getSupportedCountries()` |
| `GET /v1/rates` | Get conversion rates | `getConversionRates()` |
| `GET /v1/rates/{currency}` | Get AR rate for currency | `getARRate()` |
| `GET /v1/account/approvals` | Get credit approvals | `getCreditApprovals()` |
| `GET /v1/account/approvals/all` | Get all approvals | `getAllCreditApprovals()` |
| `GET /v1/redeem` | Redeem gift credits | `redeemCredits()` |
| `GET /v1/info` | Get payment service info | `getPaymentServiceInfo()` |

### Credit Sharing Operations

| Operation | SDK Method |
|-----------|------------|
| Share credits | `shareCredits()` |
| Revoke credits | `revokeCredits()` |
| List shares | `listCreditShares()` |
| Upload with paidBy | `uploadDataItem(paidBy = [...])` |

---

## Implementation Steps

### Subtask 1: Project Setup and Configuration
- [x] Create `ardrive-turbo-kotlin` module directory
- [x] Create `build.gradle.kts` with dependencies
- [x] Add module to `settings.gradle.kts`
- [x] Create base package structure `io.newm.ardrive.turbo`
- [x] Create `TurboConfig` class with endpoint URLs and timeouts

### Subtask 2: HTTP Client Infrastructure
- [x] Create `HttpClientFactory` for Ktor client setup
- [x] Implement logging interceptor
- [x] Implement retry interceptor with exponential backoff
- [x] Create `RetryPolicy` configuration class
- [x] Add timeout configuration support

### Subtask 3: Authentication and Signing
- [x] Create `TurboSigner` interface for request signing
- [x] Implement `ArweaveSigner` for JWK-based signing
- [x] Implement signature header generation (`x-signature`, `x-nonce`, `x-public-key`)
- [x] Add support for loading wallet from JSON string
- [x] Write unit tests for signing utilities

### Subtask 4: Upload Service - Basic Upload
- [x] Create upload request/response models
- [x] Implement `UploadService` interface
- [x] Implement `uploadDataItem()` for single file upload
- [x] Implement `getTransactionStatus()` for status polling
- [x] Add data item options (tags, target, anchor, paidBy)
- [x] Add data item signing support (pre-signed ANS-104 for now; full signing later)
- [x] Implement full ANS-104 data item signing and size calculation (arbundles parity)
- [x] Add signed data item stream factory helpers for upload flows
- [x] Write unit tests for upload service

### Subtask 5: Upload Service - Multi-Part Upload
- [x] Create multi-part upload models
- [x] Implement `createMultiPartUpload()`
- [x] Implement `uploadChunk()`
- [x] Implement `finalizeMultiPartUpload()` and async variant
- [x] Implement `getMultiPartUploadStatus()`
- [x] Add chunking logic with configurable chunk size
- [x] Write unit tests for multi-part upload

### Subtask 6: Upload Service - Account and Pricing
- [x] Implement `getAccountBalance()`
- [x] Implement `getUploadPrice()`
- [x] Implement `getServiceInfo()`
- [x] Write unit tests for account and pricing

### Subtask 7: Payment Service - Balance and Pricing
- [x] Create payment models (Balance, PriceQuote, Currency)
- [x] Implement `PaymentService` interface
- [x] Implement `getBalance()` with signed request
- [x] Implement `getPriceForBytes()`
- [x] Implement `getPriceForPayment()`
- [x] Write unit tests for balance and pricing

### Subtask 8: Payment Service - Currencies
- [x] Create currency models
- [x] Implement `getSupportedCurrencies()`
- [x] Implement `getSupportedCountries()`
- [x] Implement `getConversionRates()`
- [x] Implement `getARRate()`
- [x] Write unit tests for currency endpoints

### Subtask 9: Payment Service - Top-Up Operations
- [x] Create top-up models (TopUpQuote)
- [x] Implement `TopUpService` interface
- [x] Implement `getTopUpQuote()`
- [x] Implement `submitPendingPayment()`
- [x] Implement `topUpWithTokens()` for AR token funding
- [x] Write unit tests for top-up operations

### Subtask 10: Credit Sharing Service
- [x] Create credit sharing models (CreditShareApproval)
- [x] Implement `CreditSharingService` interface
- [x] Implement `shareCredits()`
- [x] Implement `revokeCredits()`
- [x] Implement `listCreditShares()`
- [x] Implement `getCreditApprovals()`
- [x] Add `paidBy` support to upload methods
- [x] Write unit tests for credit sharing

### Subtask 11: Unified TurboClient Interface
- [x] Create `TurboClient` interface aggregating all services
- [x] Implement `TurboClientImpl` with constructor-based dependency injection (no DI framework)
- [x] Add factory methods for client creation
- [x] Implement authenticated vs unauthenticated client modes
- [x] Write unit tests for client

### Subtask 12: High-Level Upload Helpers
- [x] Implement `uploadFile()` with automatic chunking decision
- [x] Implement `uploadFolder()` with manifest generation
- [x] Add upload progress callbacks
- [x] Add upload event handling (onProgress, onError, onSuccess)
- [x] Write unit tests for helpers

### Subtask 13: Integration + Cleanup (ArweaveRepositoryImpl)
- [x] Update existing `ArweaveRepositoryImpl` to use Turbo SDK (no new repository class)
- [x] Load config from HOCON via `environment.getSecureConfigString("arweave.walletJson")`
- [x] Inject config values into TurboClient via constructors
- [x] Migrate `uploadSongAssets()` to use Turbo SDK
- [x] Download remote file URLs to temp storage (or stream) before upload, mirroring Lambda behavior
- [x] Apply `Content-Type` tag on each upload (single file per request) and return per-file result list
- [x] Preserve per-file error handling: continue uploads, collect errors, and fail the call if any errors occurred
- [x] Add optional upload test mode to bypass real network uploads for integration tests (as Lambda `test` flag does)
- [x] Support `checkAndFund`-style behavior: if enabled, compare credit balance and top up when below 0.2 AR equivalent
- [x] Ensure temporary files are cleaned up after upload (Lambda deletes from `/tmp` to stay under 512MB)
- [x] Migrate `getWalletARBalance()` to use Turbo balance (or keep for monitoring)
- [x] Migrate `getWalletAddress()` from wallet
- [x] Implement balance monitoring and auto top-up logic in repository
- [x] Update Koin module for dependency injection
- [x] Maintain backward compatibility with existing `ar://` URLs
- [x] Remove AWS Lambda invocation code from `ArweaveRepositoryImpl.kt`
- [x] Remove Scala arweave4s library dependency from `build.gradle.kts`
- [x] Remove arweave4s imports and related Scala interop code
- [x] Remove `WeaveRequest`, `WeaveResponse`, `WeaveProps`, `WeaveFile` models
- [x] Remove Lambda-related configuration from HOCON
- [x] Update `Dependencies.kt` to remove arweave4s

### Subtask 14: Integration Tests
- [x] Create integration test suite
- [x] Test upload flow end-to-end (manual-only, disabled by default)
- [x] Test balance and pricing flows (manual-only, disabled by default)
- [x] Test top-up flow (manual-only, disabled by default)
- [ ] Test credit sharing flow (won't do)
- [x] Document test prerequisites

### Subtask 15: Documentation
- [x] Create README.md for module
- [x] Add KDoc comments to public APIs
- [x] Create usage examples
- [x] Update project documentation

---

## Testing Strategy

### Unit Tests
- Mock HTTP responses for all API endpoints
- Test retry logic with simulated failures
- Test signing correctness
- Test data model serialization/deserialization

### Integration Tests
- Test against ArDrive Turbo testnet/mainnet
- Verify upload → status → retrieval flow
- Test balance checking with real wallet
- Test pricing calculations

### Manual Verification
- End-to-end song asset upload
- Verify uploaded content accessible via `ar://` URLs
- Compare with current Lambda-based uploads

---

## Rollout Plan

1. **Phase 1: SDK Development** (Subtasks 1-12)
   - Develop SDK module independently
   - Full test coverage

2. **Phase 2: Integration + Cleanup** (Subtasks 13-14)
   - Update `ArweaveRepositoryImpl` to use Turbo SDK
   - Remove Lambda/arweave4s dependencies alongside migration
   - Validate uploads match current behavior

3. **Phase 3: Documentation** (Subtask 15)
   - Publish module documentation and usage examples

---

## Files to Remove (Cleanup)

The following files/dependencies will be removed as part of this migration:

### Dependencies (`buildSrc/src/main/kotlin/Dependencies.kt`)
- `co.upvest:arweave4s-core_2.12` - Scala Arweave library

### Model Files (`newm-server/.../arweave/model/`)
- `WeaveRequest.kt`
- `WeaveResponse.kt`
- `WeaveResponseItem.kt`
- `WeaveProps.kt`
- `WeaveFile.kt`

### Lambda Configuration
- `arweave.lambdaFunctionName` config property
- AWS Lambda SDK imports in `ArweaveRepositoryImpl.kt`

### Scala Interop Code
- All `co.upvest.arweave4s.*` imports
- Scala `Future` handling code
- `FunctionK`, `Monad` implementations

---

**Status:** ✅ Completed  
**Date:** 2026-01-19  
**Author:** Agent (Antigravity)  
**Latest Update:** Subtasks 13-15 complete; manual-only integration tests added; README/KDoc/examples published; credit sharing integration test intentionally skipped.
