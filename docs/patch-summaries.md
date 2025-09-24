# Patch and Planning Summaries

This document aggregates the non-task planning content that was previously in `todo-tasks.txt`.

---

10) 0032-End-Gateways.patch
- Intent
  - Make End Gateway teleports and portal generation region-thread-safe and partially async.
- Key entry points
  - `TheEndGatewayBlockEntity`: convert `findOrCreateValidTeleportPos` to async future; schedule cross-level region tasks via `ShreddedPaperRegionScheduler` and `RegionPos`.
- 1.21.8 anchors (candidates)
  - Block entity tick codepaths and spawn position search functions.
- Risks/Notes
  - Event/threading: ensure any Bukkit events triggered in this flow are executed on main thread.
 - Anchor status: Found — `TheEndGatewayBlockEntity`, `EndGatewayBlock`, and `EndGatewayFeature` present.

11) 0033-Block-events.patch
- Intent
  - Move block event queues into regions; run per-region after chunk ticking to maintain thread safety.
- Key entry points
  - `LevelChunkRegion`: adds `blockEvents` queue and helpers; marks region non-empty when events pending.
  - `LevelChunkRegionMap`: add block events by region; iterate regions in a bounding box.
  - `ShreddedPaperChunkTicker`: calls `level.runBlockEvents(region)` within region tick.
  - `ServerLevel`: replaces global lists with thread-local reschedule list; introduce per-region processing entry.
- 1.21.8 anchors (candidates)
  - `ServerLevel#runBlockEvents` and block event scheduling paths.
- Risks/Notes
  - Maintain vanilla semantics (reschedule/cancellation) and avoid cross-thread access to level structures.
 - Anchor status: Partial — `ServerLevel` present; new region event queues will be added during port.

13) 0051-Bukkit-API-thread-checks.patch
- Intent
  - Strengthen Bukkit-side thread checks to region ownership instead of only main-thread checks.
- Key entry points
  - `CraftChunk#getHandle(ChunkStatus)`, `CraftWorld#getChunkAt`, and `CraftBlock` getters/setters call `TickThread.ensureTickThread(...)` with region assertions.
- 1.21.8 anchors (candidates)
  - CraftBukkit class methods exist but may have minor signature or package changes; align with Purpur 1.21.8 tree.
- Risks/Notes
  - This is developer-facing: may surface more IllegalStateExceptions; document in API migration notes.
 - Anchor status: Not in NMS — verify in API/CraftBukkit module when available.

14) 0063-Optimization-Parallelization.patch
- Intent
  - Add optional parallelization for processing track queues and flushing network queues; add global lock on region locker.
- Key entry points
  - `ShreddedPaperChunkTicker`: partitions tracked entities and player connections across worker threads; gates via config.
  - `ShreddedPaperRegionLocker`: introduces `StampedLock globalLock()` for coordinated parallel phases.
  - `Connection#flushQueue()` made public and optionally skipped during per-tick if parallel flush is enabled.
- 1.21.8 anchors (candidates)
  - Netty `Connection` flush logic; region locker location; chunk tick orchestration.
- Risks/Notes
  - Must avoid deadlocks when global write lock is taken; ensure fairness and bounded partition sizes. Validate network thread safety under parallel flush.
 - Anchor status: Partial — `net.minecraft.network.Connection` present; flush points to verify; region locker to be introduced by port.

15) 0007-Thread-safe-random.patch — Make `LegacyRandomSource` CAS-based and thread-safe; accept sequence differences. Anchor: `LegacyRandomSource`. Risk: reproducibility.
 - Anchor status: Found — `LegacyRandomSource` present; also saw `ThreadSafeLegacyRandomSource` in NMS.

16) 0008-Thread-safe-NeigbhorUpdater.patch — New `PerThreadNeighborUpdater`; `Level` uses it to wrap `CollectingNeighborUpdater`. Anchor: `Level` ctor. Risk: parity.
 - Anchor status: Needs Verification — `Level` present; neighbor updater wiring to check.

17) 0011-Player.patch — Regionize players: per-region sets, region-thread player/network ticking; spawn selection via `ShreddedPaper.ensureSync`. Anchors: `ServerLevel` player list, `ServerPlayer` spawn, `RegionizedPlayerChunkLoader`. Risk: plugin threading assumptions.
 - Anchor status: Partial — `ServerLevel` and `ServerPlayer` present; `RegionizedPlayerChunkLoader` is new in port.

18) 0012-Level-ticks.patch — `LevelTicksRegionProxy` partitions block/fluid ticks; tick per region in `ShreddedPaperChunkTicker`. Anchors: `ServerLevel` fields/ctor, `LevelAccessor` schedule. Risk: scheduling semantics.
 - Anchor status: Partial — `ServerLevel` and `LevelAccessor` present; proxy types will be added.

19) 0013-Custom-spawners.patch — Run custom spawners on correct region/entity thread via schedulers; minor safety fixes. Anchors: `VillageSiege`, `CatSpawner`, `WanderingTraderSpawner`, `PatrolSpawner`, `PhantomSpawner`. Risk: behavior differences minimal.
 - Anchor status: Found — All listed spawner classes present in NMS.

20) 0014-Chunk-unloading.patch — Move unload queue to `LevelChunkRegion.unloadQueue`; process via `ChunkHolderManager.processUnloads(region)` in region tick. Anchors: `NewChunkHolder#setInUnloadQueue`, `ChunkMap#processUnloads`. Risk: ensure fairness; remove old global queue paths.
 - Anchor status: Partial — `ChunkMap` present; `NewChunkHolder` and specific method names need verification/introducing.

21) 0015-mpmap-debug-command.patch — Add `/mpmap` debug map (chunk status view).
 - Anchor status: Needs Verification — Command scaffolding not yet checked.
22) 0016-EntityLookup-accessibleEntities.patch — Sync `EntityLookup.accessibleEntities`; null-guards in debug.
 - Anchor status: Found — `EntityLookup` present (including `ca.spottedleaf` variant and NMS base).
23) 0017-World-gen.patch — Region-lock during respawn position search.
 - Anchor status: Needs Verification — Respawn/pos search anchors to locate in 1.21.8.
24) 0018-Block-entities.patch — Regionize block-entity ticking; remove global list usages.
 - Anchor status: Partial — `ServerLevel` and block entity types present; regional queues will be added.
25) 0019-Multithreaded-WeakSeqLock.patch — Exclusive writer via CAS/yield in `WeakSeqLock`.
 - Anchor status: Missing — `WeakSeqLock` not found; to be added in port.
26) 0022-Thread-safe-navigatingMobs.patch — Track navigating mobs per region; collect across locked neighbors.
27) 0023-entityMap-thread-safety.patch — New `Int2ObjectMapWrapper`; defensive sync for entity maps.
28) 0024-Misc-threadsafety.patch — Broad thread-safety sweep (locks, thread-locals, save on chunk thread).
29) 0025-NearbyPlayers.patch — Lock/guard nearby-players maps and iteration.
30) 0026-Run-unsupported-plugins-in-sync.patch — `SynchronousPluginExecution` to serialize non-Folia plugins by dep order.
31) 0027-Thread-safe-AreaMap.patch — Add `SimpleStampedLock` to `AreaMap`; reduce unnecessary recalcs.
32) 0029-ThreadLocals-for-block-state-capturing.patch — Convert capture flags/collections to ThreadLocal.

33) 0034-Add-various-API-for-Folia-plugin-compatibility.patch — Provide Folia-compat API shims: stub `io.papermc.paper.threadedregions.RegionizedServer`, fire `RegionizedServerInitEvent` on startup, and add `CraftServer#isGlobalTickThread()` distinguishing global vs region tick threads.
 - Anchor status: Partial — `org.bukkit.craftbukkit.CraftServer` present under `paper-server/`; `RegionizedServer` is new API introduced by port.
34) 0035-allow-unsupported-plugins-to-modify-chunks-via-global-scheduler.patch — Permit main/global scheduler to bypass region lock checks for non-Folia plugins when enabled. Anchors: `TickThread.canBypassTickThreadCheck()`, `ShreddedPaperChunkTicker.tickingChunks`, `ShreddedPaperRegionLocker.hasLock/hasWriteLock` relaxed under bypass.
 - Anchor status: Partial — `ca.spottedleaf.moonrise.common.util.TickThread` present in `paper-server`; other types introduced by port.
35) 0036-Entity-retirement-debug-log.patch — Track and log retirement reason in `EntityScheduler`; error if a retired scheduler is ticked.
 - Anchor status: Needs Verification — `EntityScheduler` not yet located; likely added by port.
36) 0037-Raids.patch — Make raid logic region-thread-safe: use `ShreddedPaper.ensureSync` for rewards, switch `Raids.raidMap` to concurrent map, and run tick/removal on owning region.
 - Anchor status: Found — `net.minecraft.world.entity.raid.Raids` present in NMS.
37) 0038-Thread-safe-alternate-redstone-handler.patch — Change `ServerLevel.wireHandler` to `ThreadLocal<WireHandler>` to avoid cross-thread Alternate Current access.
 - Anchor status: Partial — `ServerLevel` present; `wireHandler` field/method signatures to verify.
38) 0039-Thread-safe-redstoneUpdateInfos.patch — Move `Level.redstoneUpdateInfos` into `LevelChunkRegion.redstoneUpdateInfos` and reference by region in `RedstoneTorchBlock`.
 - Anchor status: Partial — `RedstoneTorchBlock` present; region field to be introduced.
39) 0040-Thread-safe-chunksBeingWorkedOn.patch — Use `ConcurrentHashMap<Long,Integer>` for light engine `chunksBeingWorkedOn` and atomically add/remove region tickets.
 - Anchor status: Needs Verification — Light engine internals to locate in 1.21.8.
40) 0041-kill-command.patch — Run `/kill` on the correct region thread via `ShreddedPaper.ensureSync(entity, Entity::kill)`; add `ShreddedPaper.ensureSync(Entity, Consumer<Entity>)` helper.
 - Anchor status: Found — `Entity#kill` present; helper will be added by port.

41) 0042-Purpur-teleportIfOutsideBorder-use-teleportAsync.patch — Switch Purpur world-border auto-teleport to `teleportAsync` for `ServerPlayer`. Anchor: `LivingEntity` world-border damage path. Note: aligns with event-thread constraints.
 - Anchor status: Found — `LivingEntity` present; world-border damage path exists.
42) 0043-Ensure-worlds-are-loaded-on-the-global-scheduler.patch — Enforce world loading on global scheduler: guard `MinecraftServer#prepareLevels` and `CraftServer#createWorld` with tick-thread checks; disallow region-thread or async.
 - Anchor status: Found — `MinecraftServer#prepareLevels` and `CraftServer#createWorld` present.
43) 0044-Plugin-not-folia-supported-warning.patch — Log an informational warning when synchronous plugin execution is initialized, including dependency list.
 - Anchor status: Needs Verification — Logging site depends on sync-execution impl in API layer.

45) 0046-Optimization-entity-activation-check-frequency.patch — Add config-driven reduction of entity activation checks; stagger by entity id and tick.
 - Anchor status: Found — `io.papermc.paper.entity.activation.ActivationRange` present.
46) 0047-handlingTick.patch — Convert `ServerLevel.handlingTick` to `ThreadLocal<Boolean>` and set within region tick boundaries in `ShreddedPaperChunkTicker`.
 - Anchor status: Needs Verification — `ServerLevel.handlingTick` field usage to confirm in 1.21.8.

47) 0049-PotentialCalculator.patch — Avoid CME by iterating `charges` via index and documenting add-only behavior; missing charges under contention are acceptable.
 - Anchor status: Needs Verification — `PotentialCalculator` location to confirm.
48) 0050-Remove-firework-rocket-if-entity-teleported-away.patch — If the attached entity is no longer on this connection’s tick thread for its current position, discard the `FireworkRocketEntity` to prevent cross-thread motion.
 - Anchor status: Found — `FireworkRocketEntity` present.

51) 0054-Send-ping-packet-later.patch — Move `keepConnectionAlive()` to just before flush during player tick; make it public; stop earlier invocation in `ServerGamePacketListenerImpl`.
 - Anchor status: Found — `ServerGamePacketListenerImpl#keepConnectionAlive` present.

52) 0055-Moving-into-another-region.patch — Enforce entity movement stays within correct region threads; verify both source and destination with `TickThread.ensureTickThread(entity)` and `ensureTickThread(level, destPos)`. Cap knockback/velocity to one region (`RegionPos.MAX_DISTANCE_SQR`) and warn on excessive velocity; also cap `EyeOfEnder` horizontal distance.
 - Anchor status: Partial — `Entity` present; `TickThread` present in `paper-server`; region-check helpers added by port.
53) 0056-Don't-allow-actions-outside-of-our-region.patch — Prevent block breaking outside the player’s region by checking `TickThread.isTickThreadFor(level, pos)` before accessing block state during delayed/destroy phases in `ServerPlayerGameMode`.
 - Anchor status: Partial — `ServerPlayerGameMode` present; `TickThread` in `paper-server`.
54) 0057-Fix-test-failures-and-add-null-safety-checks.patch — Add null-safety in `SynchronousPluginExecution` (config may be null) and `PaperEventManager.callEvent` (server null guard). Add missing Bukkit `EntityRemoveEvent.Cause` for `Entity#discard()` in TTL path and for `FireworkRocketEntity` discard.
 - Anchor status: Partial — NMS discard sites present; `PaperEventManager`/sync execution live outside NMS.
55) 0058-Fix-EntityRemoveEventTest-by-adding-missing-Bukkit-remove-cause.patch — Add Bukkit remove cause to `Projectile` discard when exceeding load budgets to satisfy tests.
 - Anchor status: Found — `Projectile` discard paths exist in NMS.

57) 0060-Optimization-vanish-api.patch — Add config toggle to bypass Bukkit vanish checks; `CraftPlayer#canSee(...)` early-returns true if disabled.
 - Anchor status: Not in NMS — `CraftPlayer` lives in CraftBukkit layer.
58) 0061-Optimization-Use-lazyExecute-if-we-aren't-flushing.patch — Use Netty `lazyExecute` for packet tasks when not flushing; gated by config; reduces scheduling overhead.
 - Anchor status: Partial — `Connection` present; verify `lazyExecute` path in this version.
60) 0064-Optimization-maximum-trackers-per-entity.patch — Cap tracked viewers per entity; order by distance with bypass permission support; throttle full tracker updates by config frequency; adds `ServerPlayer#hasMaximumTrackerBypassPermission`.
 - Anchor status: Partial — `ServerPlayer` present; watcher limits and permissions to be added by port.
61) 0065-Optimization-Cache-chunk-packets.patch — Cache `ClientboundLevelChunkWithLightPacket` per chunk (weak/soft ref) and expire by time; clear cache on chunk change broadcast.
 - Anchor status: Partial — Packet class present; caching layer to be introduced by port.

63) 0067-BroadcastPacketEvent.patch — Introduce experimental `BroadcastPacketEvent` and fire when broadcasting packets (entity tracker, block break progress, PlayerList.broadcast); minor optimization to reuse packet and iterate `level.players`.
 - Anchor status: Partial — Hook sites present (entity tracker, block break, broadcast); event is new API.
64) 0068-Optimization-Write-player-saves-async.patch — Optionally write player data asynchronously to `Util.ioPool()` when not on shutdown thread; wraps IO in runnable; preserves error logging.
 - Anchor status: Needs Verification — Player save IO path to inspect in 1.21.8.

---

## I. ShreddedPaper API Patch Summaries

1) 0001-Pufferfish-API.patch — Sentry context + SIMD utilities (non-stable, not for plugin use)
 - Intent
   - Add Sentry logging context helpers and SIMD-based map palette utilities; expose some loader/profiler helpers.
 - Key entry points
   - `build.gradle.kts`: add `io.sentry:sentry:5.4.0`, enable incubator vector module.
   - `gg.pufferfish.pufferfish.sentry.SentryContext` (thread context for plugin/event logging).
   - `gg.pufferfish.pufferfish.simd.*` and `org.bukkit.map.MapPalette` vectorized path; `MapPalette.colors` public.
   - `SimplePluginManager`, `JavaPluginLoader`: wrap errors with Sentry context; disable plugin on enable error.
   - `PluginClassLoader`: `_airplane_hasClass`, closed guard, throw on null after close.
 - Notes
   - Not intended as public plugin API; port if build allows incubator vector, else gate by flag.

2) 0002-Add-isFoliaSupported.patch — Plugin YAML `folia-supported` support
 - Intent
   - Add `PluginDescriptionFile.isFoliaSupported()` and parse/serialize `folia-supported` key.
 - Anchors
   - `org.bukkit.plugin.PluginDescriptionFile` fields, `loadMap`, and `saveMap` serialization.

3) 0003-Add-various-API-for-Folia-plugin-compatibility.patch — Region threading APIs
 - Intent
   - Add `RegionizedServerInitEvent` and expose `Bukkit.isGlobalTickThread()` / `Server.isGlobalTickThread()`.
 - Anchors
   - `io.papermc.paper.threadedregions.RegionizedServerInitEvent` (API).
   - `org.bukkit.Bukkit` and `org.bukkit.Server` new methods.
 - Note
   - Server-side firing implemented in server patch 0034.

4) 0004-ShreddedPaper-branding.patch — Branding in `/version`
 - Intent

5) 0005-Fix-missing-Nullable-annotations-in-SentryContext.patch — Nullability fixes
 - Intent
  - Add `@Nullable` annotations to `SentryContext.setEventContext(...)` and `SentryContext.State` accessors to satisfy tests.

This is the master planning document for HerculiPaper. It captures the porting strategy from legacy sources, risk and validation plans, and phased task trees. It will be iteratively refined as we scan the ShreddedPaper and MultiPaper patches and anchor them to Purpur 1.21.8.

References:
- X Folder (MultiPaper): `d:/Tareas no abrir/Minecraft Server/MultiPaper/`
- Y Folder (ShreddedPaper): `d:/Tareas no abrir/Minecraft Server/ShreddedPaper/`
- Patch folders: `.../patches/` in each repo

## A. Overview

- Goals
  - Finish ShreddedPaper-style multithreading on Purpur 1.21.8.
  - Extend public plugin APIs with safe async and thread-affinity surfaces.
  - Prepare for MultiPaper multi-server (cluster) integration after stability gates.

- Versions & repos
  - Target base: Purpur 1.21.8 (confirm in build files; TBD during anchor scan).
  - Legacy sources to learn from: ShreddedPaper 1.20.6, MultiPaper 1.20.1/1.20.6 era.
  - Patch sources live in `ShreddedPaper/patches/` and `MultiPaper/patches/`.

- Porting strategy (1 → 2 → 3)
  1) Implement ShreddedPaper multithreading foundations (region locking, executors, scheduler boundaries, async chunk pipeline, networking handoffs, instrumentation).
  2) Expose safe async/affinity APIs and compat shims for common Bukkit/Paper patterns; document threading guarantees.
  3) Catalog MultiPaper cross-server pieces and bring up minimal cluster/state-sync after Phase 1/2 stability.

## B. Readiness & Risks

- Toolchain & targets
  - JDK: 21+ (following ShreddedPaper guidance). Build system: Gradle (paperweight). CI TBD.
  - Confirm Purpur constants and NMS package changes in 1.21.8 (TBD during anchor scan).

- 1.20.6 → 1.21.8 notable deltas to check
  - Chunk system APIs, region scheduler, entity ticking pipelines, networking stack (`ServerGamePacketListener*`), light engine, structure generation hooks.
  - Paper async APIs and region scheduler stability/changes.

- Risk register and mitigations
  - Deadlocks between region locks and main-thread calls.
    - Mitigate via strict lock acquisition order, watchdogs, deadlock detector, timeouts, and main-thread marshalling for Bukkit events.
  - Memory growth/leaks from async pipelines and caches.
    - Mitigate via bounded queues, soft/weak references where appropriate, and metrics/alerts.
  - Tick starvation and unfair scheduling across regions/worlds.
    - Mitigate via pacing budgets, fair queueing, back-pressure, and visibility in dashboards.
  - Packet reordering/duplication during async handoffs.
    - Mitigate with encode/decode fences, per-session sequencing, and defensive network staging.
  - Plugin API breakage and unsafe cross-thread access.
    - Provide compat shims, clear guarantees, feature flags, and deprecations with migration notes.
  - Portal/teleport threading and event timing.
    - Note: Bukkit events like `PlayerTeleportEvent` must be fired synchronously on main thread. Our async portal routing must schedule back to main thread for events even if teleport execution occurs on a region thread.

- Validation & test hooks
  - Microbenchmarks, regression/soak (TPS, variance, stutter), watchdog logs, flamegraphs.
  - Region lock contention counters, scheduler queue depths, packet pipeline timings.

## C. Phase 1 — ShreddedPaper Multithread (implement first)

Ordered task tree (dependencies indicated). Each item includes rationale, 1.21.8 touchpoints (TBD anchors), acceptance, and branch suggestion.

1) Scheduler/Executor Architecture
   - Rationale: Provide region/actor style execution with strict boundaries; mirror ShreddedPaper concepts on 1.21.8.
   - Touchpoints (anchor mapping TBD): `RegionScheduler` integration, global scheduler, world tick loop, `ServerLevel` tick segmentation, `MinecraftServer` tick pacing.
   - Acceptance: Multiple worker threads process disjoint regions without cross-thread violations; metrics exposed; feature flag to disable.
   - Branch: `feature/shreddedpaper/scheduler-arch`.

2) Region/Locking Model
   - Rationale: Lock contiguous regions at configured size (power of two); guarantee safety for cross-chunk interactions.
   - Touchpoints: Region grid management, lock acquisition order, redstone/neighbor updates, entity interactions across region borders.
   - Acceptance: No data races in redstone/entity interactions; 3x3 neighbor locking verified; deadlock detector in place.
   - Branch: `feature/shreddedpaper/region-locks`.

3) World/Chunk Async Pipeline
   - Rationale: Async chunk read/write, generation and lighting stages; ensure handoffs respect region ownership.
   - Touchpoints: `ChunkHolder` pipeline, IO threads, light engine, ticketing.
   - Acceptance: Stable async load/generate/light with bounded queues; no main-thread stalls for non-required paths.
   - Branch: `feature/shreddedpaper/chunk-pipeline`.

4) Entity Ticking Partition & Ownership
   - Rationale: Entities execute on their region thread; ownership rules maintained during movement.
   - Touchpoints: Entity scheduler hooks, movement across regions, handoff logic.
   - Acceptance: Movement between regions does not violate thread affinity; no crashes; counters for handoffs.
   - Branch: `feature/shreddedpaper/entity-partition`.

5) Networking Thread-safe Handoffs
   - Rationale: Encode/decode offload; player session state guarded; parallel flush options.
   - Touchpoints: `ServerGamePacketListenerImpl`, connection queues, flush strategies; config gates.
   - Acceptance: No concurrency violations in session state; measurable throughput gains under load; parity with vanilla semantics.
   - Branch: `feature/shreddedpaper/networking-handoffs`.

6) Locks & Invariants Documentation + Refactors
   - Rationale: Replace coarse locks; document invariants/ownership; evaluate lock-free structures where indicated.
   - Touchpoints: Common synchronized blocks, shared data structures, caches.
   - Acceptance: Reduced contention; doc of invariants; tests cover concurrent access patterns.
   - Branch: `feature/shreddedpaper/locks-and-invariants`.

7) Timing & Pacing (Budgets/Watchdogs/Back-pressure)
   - Rationale: Avoid tick starvation; ensure fairness and visibility.
   - Touchpoints: Tick budgeter, queue back-pressure, watchdog logging.
   - Acceptance: Stable TPS under synthetic load; logs show pacing adjustments; no runaway queues.
   - Branch: `feature/shreddedpaper/timing-and-pacing`.

8) Debug/Metrics/Profiling Hooks
   - Rationale: Visibility into each pipeline stage and locks; flamegraph integration.
   - Touchpoints: Timers per stage, contention counters, export to logs/JFR/Prometheus (as applicable).
   - Acceptance: Metrics present and documented; developers can reproduce hot paths with flamegraphs.
   - Branch: `feature/shreddedpaper/instrumentation`.

9) Config & Feature Gates
   - Rationale: Allow bisecting regressions and safe rollout.
   - Touchpoints: `herculi.yml` or equivalent; multithreading, networking, portal routing toggles.
   - Acceptance: All major features can be toggled; defaults conservative.
   - Branch: `feature/shreddedpaper/config-gates`.

10) Compatibility Shims for Bukkit/Paper Assumptions
    - Rationale: Plugin ecosystem expects main-thread-only patterns.
    - Touchpoints: Guarded event dispatch, region-aware schedulers, async-safe views.
    - Acceptance: Clear threading guarantees; events that must be sync are marshalled to main thread.
    - Branch: `feature/shreddedpaper/compat-shims`.

Capstone (must-have demo): RegionSpawnDemo plugin — see Section F tasks and Section C acceptance criteria.

## D. Phase 2 — Public API Extensions

- Surfaces impacted by Phase 1
  - Event threading rules, callback guarantees, thread-affinity APIs.

- Expose safe async capabilities
  - Region/affinity executors, async chunk operations, thread-safe world data views, guarded event dispatch utilities.

- Deprecations & migrations
  - Main-thread-only patterns flagged; documented replacements with examples (Paper region scheduler, `entity.teleportAsync`, asynchronous lookups).

- Versioning & semantic policy
  - Consider pre-release tags while multithreading stabilizes; clear semver notes for API changes.

## E. Phase 3 — MultiPaper (multi-server)

- Prereqs
  - Phase 1 stability, Phase 2 API finalized; instrumentation shows healthy contention and TPS.

- Tasks (catalog; no implementation yet)
  - Region ownership across nodes; handoff protocols; interest management; player routing; state replication.
  - Transport/codec choice; ordering/reliability; conflict resolution; failure handling (node loss, split-brain prevention).
  - Cluster config and discovery; test matrix for multi-node sims.

## F. Engineering Operations

- Branching strategy per phase with narrow PRs and granular commits.
- CI: build/unit/integration/soak jobs; perf dashboards and alerts (TPS, GC, stalls).
- Rollback & feature-flag plan for rapid disablement of risky features.

## G. Appendix

- Patch index mapping (initial)
  - ShreddedPaper patches to scan: `Yfolder/patches/{server,api,removed}/` (68 server, 5 api; counts to verify).
  - MultiPaper patches to catalog: `Xfolder/patches/{server,api,removed}/` (approx 150 server, 9 api; counts to verify).

- Glossary & invariants
  - Region: power-of-two chunk grouping for locking and scheduling.
  - Ownership: authoritative thread/server for a region or chunk.
  - Affinity: routing work to the entity’s or region’s executor.

- Links
  - ShreddedPaper docs: `README.md`, `HOW_IT_WORKS.md`, `DEVELOPING_A_MULTITHREAD_PLUGIN.md`, `SHREDDEDPAPER_YAML.md`.
  - MultiPaper docs: `README.md`, `DEVELOPING_A_MULTISERVER_PLUGIN.md`, `MULTIPAPER_YAML.md`.

## First 10 Executable Phase-1 Tasks (reference)

1. Scheduler backbone (regionized executors)
2. Region grid & lock manager
3. Async chunk IO + light pipeline
4. Entity scheduler & handoff
5. Network pipeline fencing
6. Event dispatch guardrails
7. Timing budgets & fairness
8. Config flags & rollout controls
9. Instrumentation & profiling
10. Compatibility shims & docs

# RegionSpawnDemo Plugin (Capstone for Phase 1)
- Summary and acceptance criteria moved here for reference.

# Appendix Tasks (discovery/backlog)
- ShreddedPaper patch enumeration — Pending
- MultiPaper patch catalog — Pending
- Diff map build — Pending

## H. ShreddedPaper Patch Summaries (Initial)

4) 0010-Add-task-scheduling-API.patch
- Intent
  - Introduces region-affine task scheduling APIs for both read-only and write tasks, plus Paper RegionScheduler impl.
  - Integrates internal task queues at region level and routes chunk system tasks accordingly.
- Key entry points
  - New: `io.multipaper.shreddedpaper.ShreddedPaper` helper (runSync/ensureSync variants)
  - `LevelChunkRegion`: delayed tasks and `PrioritisedThreadedTaskQueue`
  - `LevelChunkRegionMap`: `scheduleTask`, `execute`, `executorFor`
  - New: `ShreddedPaperRegionSchedulerApiImpl` implements `RegionScheduler`
  - Modified: `ChunkTaskScheduler` to use region internal queues for chunk tasks
  - Modified: `ChunkHolderManager` autosave queue guarded and run on chunk’s thread
- 1.21.8 anchors (candidates)
  - Paper’s `RegionScheduler` interface and package path should be similar; verify method names.
  - Chunk scheduling classes (`ChunkTaskScheduler`, `NewChunkHolder`, `ChunkHolderManager`) changed upstream; map insertion points carefully.
- Risks/Notes
  - Ensure main-thread task executor interplay remains valid; avoid running main-thread tasks from region threads.
  - Back-pressure and fairness across internal queues needed; metrics present in Phase-1 tasks.

5) 0020-Teleportation.patch
- Intent
  - Enforces teleports and related operations to run on owning region thread using `ShreddedPaper.ensureSync`.
  - Converts many paths to use `teleportAsync` and introduces async respawn (`PlayerList#respawnAsync`).
- Key entry points
  - `TeleportCommand`: wraps player/entity teleport in ensureSync to correct region thread
  - `ServerGamePacketListenerImpl`: destination-ownership checks; async spectate/respawn paths
  - `ServerPlayer`: tick-thread assertions for teleport; sync around other entity-based teleports
  - `PlayerList`: async respawn flows with cross-level scheduling
  - Various entities (FishingHook, ThrownEnderpearl) move via ensureSync/teleportAsync
- 1.21.8 anchors (candidates)
  - Command handler and packet listener code shapes similar; verify method names and event flows.
- Risks/Notes
  - Events like `PlayerTeleportEvent` must be fired on the main thread. Ensure that any `callEvent` invocations are marshalled back to main thread, even when the actual teleport is executed on a region thread.

6) 0030-Portal.patch
- Intent
  - Adds `ShreddedPaperDimensionChanger` orchestrating multi-stage portal dimension changes asynchronously while ensuring region-thread ownership for world operations.
  - Calls Bukkit portal/teleport events and handles player/world state transitions.
- Key entry points
  - New: `ShreddedPaperDimensionChanger` stages (find portal/exit, events, finalization)
  - `Entity` and `ServerPlayer`: several methods made public and redirect dimension changes through the new changer
  - Event emission paths for player and entity portal/teleport flows
- 1.21.8 anchors (candidates)
  - Dimension change code in `ServerPlayer`, `Entity`, and portal forcer routines exist but may differ in signatures/locations; map carefully.
- Risks/Notes
  - Critical: Bukkit events must be fired synchronously on the main thread. The stage that invokes `Bukkit.getServer().getPluginManager().callEvent(...)` must be marshalled back to main thread, not a region thread.
  - End platform creation and world border/portal search APIs may have deltas in 1.21.8; validate before port.

3) 0009-Multithread-entity-ticking.patch
- Intent
  - Moves entity ticking into region containers with a per-region ordered list.
  - Adds a dedicated `ShreddedPaperEntityTicker` and migrates ServerLevel hooks to add/move/remove entities among regions.
- Key entry points
  - `LevelChunkRegion` gains `tickingEntities` and iterators
  - `LevelChunkRegionMap` routes add/remove/move of ticking entities
  - New: `ShreddedPaperEntityTicker`
  - Modified: `ServerLevel` to stop using global `EntityTickList` and instead delegate to regions
  - Modified: `Entity` gains `previousTickingChunkPosRegion`
- 1.21.8 anchors (candidates)
  - `ServerLevel` entity tick loop structure; need to match points where entity ticking previously occurred.
  - Hooks for entity section change/movement to trigger region migration.
- Risks/Notes
  - Maintain tick ordering guarantees (IteratorSafeOrderedReferenceSet semantics) and activation range behavior.
  - Ensure vehicle/passenger logic and tick gating remain correct under region execution.
