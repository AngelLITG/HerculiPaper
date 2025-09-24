# Candidate hook points for Entity Region Scheduler handoffs

Purpose: enumerate low-risk, non-gameplay-critical call sites that can be executed on a region scheduler thread and fenced to main thread when they interact with Bukkit API or global shared state.

Scope: default-off unless explicitly enabled via `threading.handoff.*` gates. Guardrails remain ON and enforce Bukkit event dispatch on main.

- Particles (low risk)
  - API: `World.spawnParticle(...)`, `Player.spawnParticle(...)`
  - Behavior: purely client visual. No world state changes.
  - Risks: plugins observing particle events (rare), excessive volume.
  - Mitigation: emit via region scheduler; if Bukkit event listeners exist, schedule event on main.

- Sounds (low risk)
  - API: `World.playSound(Location, ...)`, `World.playSound(Entity, ...)`
  - Behavior: client auditory. No world state changes.
  - Risks: ordering vs other packets; seed-based sound variance.
  - Mitigation: keep per-region ordering; use debug logs to confirm handoff.

- Tracker packet emissions (low/medium)
  - API: entity add/remove/move packets sending to tracked players.
  - Behavior: network-only; must preserve intra-entity ordering per region.
  - Risks: cross-region ordering and concurrency to same player connection.
  - Mitigation: fence to per-player network queue; avoid reordering across regions.

- Chunk tracker flush windows (medium)
  - API: `ChunkMap.TrackedEntity` periodic updates.
  - Risks: hidden dependencies in plugin callbacks; back-pressure.
  - Mitigation: small batched windows; preserve order; default-off.

- Pathfinding recalculation (medium)
  - API: brain/goal triggers causing recalculation.
  - Risks: correctness vs vanilla tick phasing; CPU cost bursts.
  - Mitigation: separate gate; coalesce; schedule per-entity region queue.

- Villager sensor scan throttling (medium)
  - API: villager brain sensors.
  - Risks: gameplay subtlety (trade restock timings, gossip, POI selection).
  - Mitigation: separate gate; rate-limit scans; measure impact before enabling.

- Event constraints (critical)
  - Always dispatch Bukkit events on main (e.g., `PlayerTeleportEvent`, `EntityTeleportEvent`, interact, place/break, etc.).
  - Use `MainThread.runOnMainAndWait(...)`/`supplyOnMainAndWait(...)` as needed.

- Config gates
  - `threading.handoff.enabled` (master switch)
  - `threading.handoff.particles` (default: false)
  - `threading.handoff.sounds` (default: false)
  - `threading.handoff.tracker_packets` (default: false)
  - `threading.handoff.debug` (default: false)

- Diagnostics
  - Add debug lines when a handoff occurs (throttled), including region, entity id, and action (particle/sound/tracker).
  - Consider counters per region for emissions to spot hotspots.
