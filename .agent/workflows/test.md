---
description: Run test suites for backend modules
---

# Test Workflow

Quick commands for running tests across NEWM Server modules.

## Quick Commands

### Run All Tests

// turbo
```bash
./gradlew test
```

### Run Specific Module Tests

```bash
./gradlew :newm-server:test
./gradlew :newm-chain:test
./gradlew :newm-tx-builder:test
./gradlew :newm-shared:test
```

---

## Integration Tests

### Garage Environment (Dev)

```bash
./gradlew integTestGarage
```

### Studio Environment (Production)

```bash
./gradlew integTestStudio
```

### Required Environment Variables

```bash
export NEWM_EMAIL="test@example.com"
export NEWM_PASSWORD="password"
export NEWM_BASEURL="https://garage.newm.io"
```

---

## Test with Logging

```bash
./gradlew test --info
```

---

## Run Single Test Class

```bash
./gradlew :newm-server:test --tests "io.newm.server.features.user.UserRepositoryTest"
```

---

## Troubleshooting

### Tests Failing with Database Errors

Ensure PostgreSQL TestContainers can start:
```bash
docker ps  # Check Docker is running
```

### Flaky Tests

Force re-run:
```bash
./gradlew test --rerun-tasks
```
