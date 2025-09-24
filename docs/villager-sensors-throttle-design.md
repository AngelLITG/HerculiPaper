# Villager Sensor Scans Throttling (Design, Phase 0)

Goals
- Reduce CPU churn from frequent villager sensor scans in dense areas.
- Preserve gameplay semantics with a conservative, default-off gate.
- Provide tunable, per-tick minimum interval to coalesce repeated scans.

Scope (Phase 0)
- Research/design document and config gates only.
- Identify primary sensor hotspots (POI brain sensors, gossip, job site/home bed checks).

Invariants
- When disabled, behavior is vanilla-identical.
- Throttling decisions happen on the main thread.
- No starvation: ensure a maximum deferral window.

Candidate Hooks
- Brain sensor tick methods (e.g., `net.minecraft.world.entity.ai.sensing.*`).
- Villager job site and bed acquisition logic.

Policy (Phase 1 proposal)
- `villagers.sensors.throttle.minIntervalTicks` per-villager minimum interval between scans.
- Optional jitter (`villagers.sensors.throttle.jitterTicks`) to avoid synchronized bursts.
- Optional per-sensor allowlist/denylist (future phase) for fine tuning.

Config (default-off)
- villagers.sensors.throttle.enabled: false
- villagers.sensors.throttle.minIntervalTicks: 10
- villagers.sensors.throttle.jitterTicks: 2
- villagers.sensors.debug: false

Metrics (Phase 1)
- totalSensorScans
- deferredDueToThrottle
- coalescedScans

Failure Modes
- Over-throttling could slow villager job/home updates. Mitigate with conservative defaults and caps.

Testing
- Villager-heavy trading halls; bed/job reallocation storms; raid behavior.

Phases
- Phase 0: Design doc + config scaffolding.
- Phase 1: Implement per-villager throttle with metrics.
- Phase 2: Per-sensor policy and tuning.
