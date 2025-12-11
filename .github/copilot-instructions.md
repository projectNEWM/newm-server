# GitHub Copilot Code Review Instructions

## Review Philosophy
- Only comment when you have HIGH CONFIDENCE (>80%) that an issue exists
- Be concise: one sentence per comment when possible
- Focus on actionable feedback, not observations
- When reviewing text, only comment on clarity issues if the text is genuinely confusing or could lead to errors. "Could be clearer" is not the same as "is confusing" - stay silent unless HIGH confidence it will cause problems

## Priority Areas (Review These)

### Security & Safety
- Command injection risks (shell commands, user input)
- Path traversal vulnerabilities
- Credential exposure or hardcoded secrets
- Missing input validation on external data
- Improper error handling that could leak sensitive info
- SQL injection vulnerabilities (use parameterized queries)

### Correctness Issues
- Logic errors that could cause exceptions or incorrect behavior
- Race conditions in concurrent code (Kotlin coroutines)
- Resource leaks (database connections, file handles)
- Off-by-one errors or boundary conditions
- Improper null handling (use null-safe operators in Kotlin)
- Optional types that don't need to be optional
- Booleans that should default to false but are set as optional
- Error messages that don't add useful information
- Overly defensive code that adds unnecessary checks
- Unnecessary comments that just restate what the code already shows (remove them)
- Exposed ORM transactions not properly scoped

### Architecture & Patterns
- Code that violates existing patterns in the codebase
- Missing error handling in Kotlin (should use Result/try-catch appropriately)
- Improper use of Koin dependency injection
- Exposed ORM anti-patterns (N+1 queries, missing transactions)
- gRPC service implementation issues



## Project-Specific Context

- This is a **Kotlin/Ktor** backend (100% Kotlin, no frontend in this repo)
- Backend modules: Multi-module Gradle project with Ktor
  - `newm-server` - Main REST API server (Ktor/CIO)
  - `newm-chain` - Cardano blockchain monitor/indexer (gRPC server)
  - `newm-chain-db` - Database layer for chain module
  - `newm-chain-grpc` - gRPC definitions and client
  - `newm-chain-util` - Cardano utility functions
  - `newm-tx-builder` - Cardano transaction builder
  - `newm-shared` - Shared utilities across modules
  - `newm-objectpool` - Object pooling utilities
  - `ktor-grpc` - Custom Ktor gRPC integration
- Build tool: Gradle (Kotlin DSL) with configuration cache enabled
- Database: Exposed ORM for persistence
- Cardano blockchain integration throughout multiple modules
- Dependency injection: Koin
- Deploys to AWS Elastic Beanstalk via CodePipeline

## CI Pipeline Context

**Important**: You review PRs immediately, before CI completes. Do not flag issues that CI will catch.

### What Our CI Checks

**Kotlin/Backend checks** (`.github/workflows/test.yml`):
- `./gradlew test -PisGithubActions` - All Kotlin tests across submodules
- Uses JDK 21 (Zulu distribution)

**Ktlint checks** (`.github/workflows/ktlint.yml`):
- Downloads and verifies ktlint binary (SHA256 + PGP signature)
- Runs `ktlint '**/*.kt*' '!**/generated/**'` for linting
- Uses ktlint v1.8.0

**Deployment** :
- Auto-deploys master branch to AWS Elastic Beanstalk through AWS Code Pipeline

**Setup steps CI performs:**
- Caches Gradle dependencies with configuration cache
- Runs with `-PisGithubActions` flag


## Skip These (Low Value)

Do not comment on:
- **Style/formatting** - CI handles this (ktlint)
- **Linting warnings** - CI handles this (ktlint)
- **Test failures** - CI handles this (full test suite via Gradle)
- **Missing dependencies** - CI handles this (Gradle sync will fail)
- **Minor naming suggestions** - unless truly confusing
- **Suggestions to add comments** - for self-documenting code
- **Refactoring suggestions** - unless there's a clear bug or maintainability issue
- **Multiple issues in one comment** - choose the single most critical issue
- **Logging suggestions** - unless for errors or security events (the codebase needs less logging, not more)
- **Pedantic accuracy in text** - unless it would cause actual confusion or errors. No one likes a reply guy

## Response Format

When you identify an issue:
1. **State the problem** (1 sentence)
2. **Why it matters** (1 sentence, only if not obvious)
3. **Suggested fix** (code snippet or specific action)

Example:
```
This could throw IndexOutOfBoundsException if the list is empty. Consider using `.getOrNull(0)` or add a size check.
```

## When to Stay Silent

If you're uncertain whether something is an issue, don't comment. False positives create noise and reduce trust in the review process.