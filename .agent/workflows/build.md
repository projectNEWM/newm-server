---
description: Build all project modules (backend)
---

# Build Workflow

Quick commands and procedures for building all NEWM Server modules.

## Quick Commands

### Full Build

// turbo
```bash
./gradlew build
```

### Fat JAR (for deployment)

// turbo
```bash
./gradlew shadowJar
```

### Clean Build

// turbo
```bash
./gradlew clean build
```

---

## Build Specific Modules

```bash
# Main server
./gradlew :newm-server:build

# Blockchain chain sync
./gradlew :newm-chain:build

# Transaction builder
./gradlew :newm-tx-builder:build
```

---

## Lint Check

// turbo
```bash
./gradlew ktlintCheck
```

// turbo
```bash
./gradlew ktlintFormat  # Auto-fix
```

---

## Dependency Updates Check

```bash
./gradlew dependencyUpdates
```

---

## Troubleshooting

### Build Cache Issues

```bash
./gradlew clean build --refresh-dependencies
```

### Memory Issues

```bash
./gradlew build -Dorg.gradle.jvmargs="-Xmx4g"
```