# Pathfinding Recalculation Scheduling (Design, Phase 0)

Goals
- Reduce pathological CPU spikes from frequent path recomputations.
- Maintain gameplay parity: no functional behavior change when disabled.
- Provide conservative default-off gate with clear guardrails and rollbacks.

Scope (Phase 0)
- Research and design only; add config gates and debug counters.
- Identify low-risk hooks for deferring or coalescing recalculations.

Invariants
- Bukkit/vanilla semantics preserved when the feature is disabled.
- All scheduling decisions happen on the main thread.
- No starvation: entity paths eventually recompute within a bounded time.

Candidate Hooks
- Path recompute triggers in navigation controllers (e.g., `net.minecraft.world.entity.ai.navigation.*`).
- Villager/raid AI that causes repeated recalcs during disturbances.

Policy (Phase 1 proposal)
- Per-entity minimum recompute interval (ticks), e.g. `pathfinding.recalc.minIntervalTicks`.
- Optional jitter window to avoid herd synchronization.
- Optional coalescing: if multiple triggers occur within the interval, recompute once at the end.

Config (default-off)
- pathfinding.recalc.enabled: false
- pathfinding.recalc.minIntervalTicks: 5
- pathfinding.recalc.jitterTicks: 2
- pathfinding.recalc.debug: false

Metrics (Phase 1)
- totalRecalcRequests
- deferredDueToMinInterval
- coalescedRecalcs

Failure Modes
- Over-deferral may cause sluggish path updates; mitigate by strict max deferral per-entity.
- High-churn mobs (villagers, raids) need per-AI exceptions; default allowlist.

Testing
- Micro-bench with 50-100 mobs moving between waypoints.
- Raid/villager scenarios: verify movement remains responsive under load.

Phases
- Phase 0: This design doc + config scaffolding (default-off).
- Phase 1: Implement min-interval + coalescing with metrics.
- Phase 2: Tune policies per-entity-class and expose runtime toggles.
