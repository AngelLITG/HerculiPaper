# Multithread Chunk Ticking Plan (Design)

Goals
- Improve throughput of chunk ticking by parallelizing non-critical portions under strict safety guards.
- Preserve gameplay semantics with default-off gates and easy rollback.
- Provide clear invariants, ordering rules, and thread-ownership guarantees.

Invariants
- All Bukkit events remain dispatched on the main thread.
- World/chunk data is only mutated by its owning thread according to region/thread ownership rules.
- Cross-chunk interactions must use safe handoff/scheduling primitives.
- Determinism: no observable ordering regressions for gameplay-critical effects.

Scope (Phase 1 candidates)
- Target low-risk tick tasks first (e.g., environmental updates, passive entity behaviors) while keeping redstone and complex AI on main.
- Consider per-region executors (already present) to own groups of chunks.
- Maintain a main-thread barrier for network emission and plugin callbacks.

Thread Ownership
- Define region keys as `(dimension, regionX, regionZ)` where region size is `1 << threading.region_shift` chunks.
- A region executor owns all chunk-tick work within its key for the duration of a tick slice.
- Cross-region work must schedule via region or global executors.

Ordering & Fencing
- Within a region, per-entity and per-chunk tasks are ordered deterministically.
- Inter-region effects use message passing and are applied on the owner region the following tick slice.
- Network sends funnel to main via fenced queues (default-off), preserving per-recipient ordering.

Config (default-off)
- ticking.parallel.enabled: false
- ticking.parallel.maxRegionsInFlight: 1
- ticking.parallel.debug: false
- ticking.parallel.categories:
  - entities.passive: true
  - environment.randomTicks: true
  - tileEntities.safeSet: true
  - redstone: false (remain on main)

Metrics
- regionsInFlight, tasksEnqueued, tasksExecuted, overBudgetTicks.
- Per-category executed and deferred.

Rollout Plan
- Phase 0: This design doc + config scaffolding.
- Phase 1: Route a small, safe category (e.g., environment random ticks) via region executors behind gate.
- Phase 2: Expand to additional categories, add fairness budgets, and richer metrics.

Testing
- Synthetic stress: many chunks loaded with passive entities and random ticks.
- Plugin-heavy servers: verify event ordering and no deadlocks; TPS stability under load.
