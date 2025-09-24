# Threaded Chunk Change Broadcasting — Design (Draft)

## Goals
- Define ordering guarantees for block/entity change broadcasts under multi-threading.
- Introduce per-recipient queueing where needed to preserve view-consistency.
- Gate the feature fully via config, default-off, safe to ship.
- Expose basic metrics in `/herculi queues`.

## Scope (Phase 1)
- Limit to low-risk broadcast classes (block updates, block entity updates, multi-block changes) emitted from region threads.
- Keep high-risk paths (entity add/remove, chunk (un)load) on main until proven safe.

## Invariants
- All Bukkit events remain dispatched on the main thread.
- For any single player, broadcasts must be observed in causal order per-chunk (or stronger, per-section) where applicable.
- Cross-chunk ordering is best-effort; per-recipient queue serialization per-source-chunk ensures stability for most gameplay/UI cases.

## Architecture
- Region thread emits change packets into a per-recipient, per-source-chunk queue (bounded).
- A lightweight `NetworkFence`/"fence token" is attached to a batch; the main network send thread drains serialized order per recipient.
- Backpressure: if recipient queue is full, drop to a safe fallback (coalesce or degrade to main-thread send) based on config.

## Data Structures
- `RecipientQueueKey = (playerUUID, worldKey, chunkX, chunkZ)`
- `RecipientQueue` holds small ring buffers of packet batches with sequence numbers.
- `RecipientQueueManager` tracks queues, bounded by total memory and per-player caps.

## Ordering
- Per `(player, chunk)` queue preserves order of broadcasts originating from that chunk.
- For multi-chunk effects, we allow independent queues; UI tearing is acceptable in Phase 1.

## Metrics (in `/herculi queues`)
- activeQueues
- enqueuedBatches
- dequeuedBatches
- droppedBatches (by cause: overflow, timeout)
- inFlightPerRecipient

## Config (herculi.yml)
```yaml
broadcasting:
  threaded:
    enabled: false
    perRecipientQueues: true
    maxQueuesTotal: 20000
    maxQueuesPerPlayer: 512
    maxBatchesPerQueue: 64
    dropPolicy: coalesce   # coalesce|fallback_to_main|drop
    drainIntervalMs: 2     # pacing for drains
```

## Failure Modes and Fallbacks
- Overflow: obey `dropPolicy`.
- Long stall: if a recipient stops draining, after `stallTimeoutMs` switch that recipient to main-thread fallback for affected chunks.

## Phase Plan
- Phase 0 (scaffold):
  - Define interfaces, queues, metrics plumbing (no behavior change).
- Phase 1 (gated):
  - Route block updates (single/multi), block entity data through queues when enabled.
- Phase 2 (opt-in):
  - Consider entity metadata/equipment deltas per-player queueing with strict ordering.

## Testing
- Synthetic: flood block updates within a chunk; verify order per recipient.
- Live world: profile with `/herculi queues`; ensure no excessive drops or stalls.

## Open Questions
- Cross-chunk atomicity needs? Likely no for Phase 1.
- How to coalesce smartly (e.g., last-wins within section).

## Acceptance (matches TODO 63)
- Defined ordering guarantees documented.
- Per-recipient queueing where needed.
- Gated via config.
- Metrics visible in `/herculi queues`.
