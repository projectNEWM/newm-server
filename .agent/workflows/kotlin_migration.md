---
description: Safe Java to Kotlin migration with test-first approach
---

# Kotlin Migration Workflow

Guide for migrating Java code to Kotlin safely.

> **Note:** This codebase is already 100% Kotlin. This workflow is for reference if any Java code is added.

## Overview

NEWM Server is written entirely in Kotlin. If Java files are introduced (e.g., from dependencies), use this workflow to migrate them.

---

## Migration Process

### 1. Identify Java Code

```bash
find . -name "*.java" -path "*/src/main/*"
```

### 2. Convert with IntelliJ

1. Right-click Java file → "Convert Java File to Kotlin File"
2. Review automatic conversion
3. Apply Kotlin idioms manually

### 3. Apply Kotlin Idioms

**Before (Java-style):**
```kotlin
fun getUser(id: UUID): User? {
    val user = userRepository.findById(id)
    if (user != null) {
        return user
    }
    return null
}
```

**After (Kotlin-style):**
```kotlin
fun getUser(id: UUID): User? = userRepository.findById(id)
```

### 4. Run Tests

// turbo
```bash
./gradlew test
```

---

## Common Conversions

| Java | Kotlin |
|------|--------|
| `Optional<T>` | `T?` |
| `Stream.map()` | `.map {}` |
| `StringBuilder` | String templates |
| Getters/Setters | Properties |
| `static` | `companion object` |

---

## Verify

// turbo
```bash
./gradlew ktlintCheck
./gradlew test
```
