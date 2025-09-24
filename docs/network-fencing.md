# Network Pipeline Fencing (Scaffolding)

Status: Phase 0 scaffolding (default-off)

Goal: Provide a safe, opt-in mechanism to fence/buffer network emissions so that cross-thread producers (region threads) can preserve ordering and avoid races when writing to player connections.

Config gates
- networking.fencing.enabled: false (default)
- networking.fencing.debug: false (default)

API surface
- Class: `io.multipaper.herculi.net.NetworkFence`
  - `enabled()`: consults config
  - `beforeWrite(String context)`: no-op unless enabled; optional debug log
  - `afterWrite(String context)`: no-op unless enabled; optional debug log

Phase plan
- Phase 0 (this change): gates + no-op hooks + docs.
- Phase 1: instrument choke points (packet flush/send locales) with `beforeWrite/afterWrite` (still logically no-op) to validate overhead and logging.
- Phase 2: introduce per-recipient (player) lightweight queueing/fencing that maintains intra-recipient ordering; keep default-off.

Design notes
- Preserve ordering per player connection; do not introduce cross-region reordering for the same recipient.
- Never fire Bukkit events off-main; fence those to main thread regardless of net fencing.
- Keep debug logs throttled to avoid spam.

Validation
- Enable `networking.fencing.enabled` and `networking.fencing.debug` in `config/herculi.yml` on a test server.
- Exercise high-volume sends (entity tracking, sounds/particles) and verify ordered delivery and absence of concurrent modification issues.
