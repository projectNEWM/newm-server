# Chain Sync Initial-Load Performance Improvements

## Overview
Improve full initial sync performance for newm-chain/newm-chain-db without changing external behavior or weakening transactional guarantees. Focus on reducing write amplification, eliminating N+1 query patterns, and shortening transaction duration while keeping the system runnable and correct after every incremental change.

## Requirements
- [x] Initial sync throughput improves measurably without regressions in correctness.
- [x] No changes to external APIs or observable ledger semantics.
- [x] Transactional guarantees remain intact (no partial or inconsistent state).
- [x] Each incremental change is safe, isolated, and testable.
- [x] Rollback handling remains correct and reliable.
- [x] Sync logic remains stable and recoverable after crashes/restarts.

## Technical Design

### Components Affected
- Module: newm-chain (BlockDaemon)
  - Guard rollback logic to run only on actual rollbacks.
  - Tune block batching behavior for initial sync.
  - Optional config-gated fast sync for non-critical metadata/logs.
- Module: newm-chain-db (ChainRepositoryImpl)
  - Batch insert chain blocks.
  - Compute etaV in-memory for batches to reduce DB reads.
- Module: newm-chain-db (LedgerRepositoryImpl)
  - Replace N+1 per-UTXO/asset queries with batch operations.
  - Batch spend updates.
  - Reduce repeated metadata/log reads within a block.

### Database Changes
No schema changes required. Existing indexes already cover key lookups:
- chain: unique indexes on slot_number and block_number.
- ledger_utxos: indexes on block_spent, block_created, (tx_id, tx_ix), ledger_id, transaction_spent.
- ledger_assets: (policy, name).
- ledger_utxo_assets: ledger_utxo_id, ledger_asset_id.
- logs: address_tx_log(address, tx_id), block_number indexes; native_asset_log(block_number).
- stake: stake_delegations and stake_registrations indexes.

### API Changes
None.

## Implementation Steps
1. Make rollbacks explicit and rare
   - Only perform rollback deletes when a RollBackward actually occurs. A RollBackward is always sent when the connection to Kogmios is made, so normal forward syncs should not trigger rollback logic.
   - Keep rollback semantics intact; avoid delete/rollback on normal forward batches.

2. Batch chain inserts and etaV computation
   - Implement batch insert for chain blocks in ChainRepositoryImpl.insertAll.
   - Precompute etaV sequentially in memory per batch using the last known etaV.

3. Batch ledger UTXO creation
   - Preload ledger ids for all addresses in a block.
   - Batch insert missing ledger rows.
   - Batch insert ledger_utxos.
   - Resolve asset ids in a single query and batch insert missing assets.
   - Batch insert ledger_utxo_assets rows.

4. Batch spend updates
   - Replace per-UTXO updates with a batch update keyed by (tx_id, tx_ix).

5. Optimize metadata/log reads within a block
   - Cache asset and metadata lookups per block to avoid repeated DB reads.

6. Stabilize batch sizing for initial sync
   - Cap blockBufferSize and use deterministic batch sizes for catch-up.

7. Add performance instrumentation
   - Time per repository method and per batch.
   - Capture query counts where possible.

## Testing Strategy
- Replay a representative block range and verify:
  - Chain table continuity and etaV correctness.
  - Ledger UTXO counts and balances vs. baseline.
  - Stake registrations/delegations match baseline.
  - Native asset metadata/log outputs unchanged (or match when fast sync is off).
- Integration test rollback scenario to ensure rollback correctness.
- Measure sync throughput before and after each change.
- Verify idempotency and crash recovery across mid-sync restarts.

## Rollout Plan
- Implement changes incrementally in separate PRs or commits.
- Validate each step with a small sync range and a rollback test.
- Only enable optional fast sync mode after validation and with a backfill plan.

## Open Questions
- What is the acceptable tradeoff between throughput and metadata/log latency during initial sync?

---

Status: ✅ Completed
Date: 2026-01-30
Author: OpenCode

## Progress
- [x] Task-001: Rollback only on RollBackward (gate rollback deletes to rollback events and clear forward buffer on rollback).
- [x] Task-002: Cap batch size during catch-up (cap block buffer growth for non-tip batches).
- [x] Task-003: Batch insert chain blocks (batch chain inserts and compute etaV in-memory per batch).
- [x] Task-004: Batch ledger lookup/insert (preload ledger ids by address and batch insert missing ledger rows).
- [x] Task-005: Batch insert ledger_utxos (insert created UTXOs in batch and return ids for asset join).
- [x] Task-006: Batch resolve and insert ledger_assets (resolve asset ids in one query and batch insert missing assets).
- [x] Task-007: Batch insert ledger_utxo_assets (insert asset-to-utxo rows in a batch).
- [x] Task-008: Batch spend updates (batch update spent UTXOs by block).
- [x] Task-009: Per-block metadata lookup cache (cache ledger asset/metadata lookups within a block to reduce repeated reads).
- [~] Step-007: Performance instrumentation (deferred).
