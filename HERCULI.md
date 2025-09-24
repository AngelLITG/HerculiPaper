# Herculi Configuration Notes

This project auto-creates a minimal `config/herculi.yml` on first run. Unknown keys are ignored. Comments are included inline.

## Implemented features (high level)
- Region scheduler foundations and guards (default-off gates). See `todo-tasks.txt` items 1–4.
- Block/unload instrumentation (default-off; see keys below).
- World-gen micro-optimizations (allocation reductions in decoration loop).
- Thread-safety improvements:
  - Navigating mobs set concurrency in `ServerLevel`.
  - Safer entity maps iteration in `net.minecraft.world.level.entity.EntityLookup`.
  - NearbyPlayers verification and usage.
- Command marshalling scaffolding for per-target execution (gated).

See `todo-tasks.txt` for the complete parity plan and status.

## Random Tick Precompute (Task 18)
- Key: `threading.random_precompute.enabled`
- Default: `false`
- Scope: Server-level random tick path.
- Behavior when enabled:
  - Schedules a tiny, read-only off-thread precompute before `ServerLevel.optimiseRandomTick(...)`.
  - At the beginning of `optimiseRandomTick(...)` a poll occurs to complete the end-to-end path (currently discarding the result).
- Performance rationale:
  - Presently used for instrumentation and plumbing validation only; it does not reduce main-thread work yet. Keep disabled in production until subsequent phases leverage it to skip real hot-path work.
- Safety:
  - Entire feature is guarded; when disabled there is zero behavioral change and negligible overhead.

## Unload Metrics (Task 20)
- Key: `instrumentation.unload.metrics_enabled`
- Default: `false`
- Scope: `ServerLevel.unload(LevelChunk)` timing
- Behavior when enabled:
  - Records per-unload duration (nanoseconds), counts, average (microseconds), and max (microseconds).
  - Exposed via `/herculi probe`: fields `unloads.count`, `unloads.avg_us`, `unloads.max_us`.
  - Reset via `/herculi probe reset`.
- Notes:
  - This is lightweight and only active when enabled. Disabled state has effectively zero overhead.

## Block Entity Metrics (Task 24)
- Key: `instrumentation.block_entities.metrics_enabled`
- Default: `false`
- Scope: `LevelChunk.BoundTickingBlockEntity.tick()` timing
- Behavior when enabled:
  - Records per-block-entity tick durations (ns), count, average (µs), and max (µs).
  - Exposed via `/herculi probe`: fields `be.count`, `be.avg_us`, `be.max_us`.
  - Reset via `/herculi probe reset`.
- Notes:
  - Implemented as a minimal finally-block around the ticker call with a config check. Disabled state has negligible overhead.
## Configuration keys index (current)
- threading.random_precompute.enabled
- instrumentation.unload.metrics_enabled
- instrumentation.block_entities.metrics_enabled
- commands.marshalPerTarget
- networking.fencing.enabled
- networking.fencing.debug
- broadcasting.threaded.enabled
- broadcasting.threaded.maxBatchesPerQueue
- broadcasting.threaded.perTickDrainBudget
- tracking.max_trackers_per_entity
- tracking.max_trackers_heavy
- tracking.heavy_types

## Upcoming items (parity with ShreddedPaper)
- Threaded chunk changes broadcasting (0028) — gated, budgeted drains, ordering guarantees. Coordinates with `0067-BroadcastPacketEvent` scaffolding.
- Portal pipeline (0030) — staged, region-affine portal handling; Bukkit events on main thread.
- Ender Dragon fight concurrency adjustments (0031).
- Misc threadsafety sweep (0024) and additional optimization toggles (0062, 0066).

## Reloading
Use `/herculi reload` to reload the configuration file at runtime. Some subsystems reinitialize on reload.
