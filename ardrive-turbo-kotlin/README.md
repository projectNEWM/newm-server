# ArDrive Turbo Kotlin SDK

Kotlin SDK for ArDrive Turbo upload and payment APIs. This module powers NEWM Server's Arweave uploads and can also be used standalone.

## Features
- Authenticated Turbo uploads (single + chunked)
- Payment endpoints (balance, pricing, top-ups)
- Credit sharing operations
- ANS-104 signing and upload helpers
- Coroutine-friendly API

## Install

Add the module dependency in your Gradle build:

```kotlin
dependencies {
    implementation(project(":ardrive-turbo-kotlin"))
}
```

## Quick Start

```kotlin
import io.newm.ardrive.turbo.TurboClientFactory
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.TurboLogSeverity
import kotlin.time.Duration.Companion.seconds

val walletJson = "" // NEVER commit real wallet JSON to source control.

val config = TurboConfig(
    uploadBaseUrl = "https://upload.ardrive.io",
    paymentBaseUrl = "https://payment.ardrive.io",
    requestTimeout = 60.seconds,
    connectTimeout = 10.seconds,
    socketTimeout = 60.seconds,
    retryPolicy = io.newm.ardrive.turbo.RetryPolicy(maxAttempts = 3),
    logSeverity = TurboLogSeverity.INFO,
)

val client = TurboClientFactory.createAuthenticated(
    walletJson = walletJson,
    config = config,
)
```

### Upload a file

```kotlin
val response = client.uploadFile(
    data = "hello turbo".toByteArray(),
    contentType = "text/plain",
)
println(response.id)
```

### Check balance and pricing

```kotlin
val balance = client.getPaymentBalance()
val price = client.paymentService.getPriceForBytes(1024)
```

### Top up with AR

```kotlin
val response = client.paymentService.topUpWithTokens(
    token = io.newm.ardrive.turbo.model.TokenType.ARWEAVE,
    tokenAmount = "0.1",
)
println(response.status)
```

## Configuration

`TurboConfig` supports custom endpoints, timeouts, retry policy, and logging controls. Use constructor injection so the SDK can be reused outside this project.

## Testing

- Unit tests: `./gradlew :ardrive-turbo-kotlin:test`
- Manual integration tests live under `newm-server/src/test/kotlin/io/newm/server/features/arweave/integration/` and are disabled by default.

## Security

Never commit wallet JSON or other secrets to source control. Provide them locally or via a secure secrets manager.
