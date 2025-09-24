# Research: Pathfinding Recalculation Scheduling

Goal: Evaluate moving pathfinding recalculation triggers to the entity region scheduler, behind a separate gate (default-off), without changing gameplay semantics.

Questions to answer
- Which triggers cause recalculation (goal tick, navigation invalidate, collisions, target changes)?
- What ordering constraints exist vs tick phases (brain, goals, movement, collisions)?
- Which codepaths may sync-touch global state or Bukkit events (must be fenced to main)?

Initial hypotheses
- Recalculation can often be deferred to the same-tick region queue, preserving ordering for that entity.
- Cross-region dependencies (target across boundary) require careful read-only access; writes must stay in-region.

Metrics to gather
- Count recalculation invocations per-entity type per-minute.
- Time spent in recalculation and queue wait time when region-scheduled.
- Failure/rollback counters (if any guardrails trip).

Design sketch (phase 0)
- Gate: ai.pathfinding.region_schedule (default: false)
- If enabled, enqueue recalculation on the entity region scheduler.
- Fencing: if codepath raises a Bukkit event or touches Craft layer, marshal that portion to main.

Validation plan
- A/B on test server with crowded mobs and moving targets.
- Compare path optimality and arrival time distributions vs baseline.
- Watchdog/AsyncCatcher logs must stay clean.
