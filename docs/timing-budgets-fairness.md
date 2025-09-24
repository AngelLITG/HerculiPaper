# Timing Budgets & Fairness (Design, Phase 0)

Goals
- Smooth per-tick latency by enforcing soft budgets on non-critical work.
- Preserve gameplay parity under default-off settings.
- Provide debuggable knobs and safe rollbacks.

Core Concepts
- Budget: maximum work per tick for a category (e.g., broadcast drain, async IO completions, diagnostics, chunk saves).
- Queueing: when budget is exceeded, carry the remainder to subsequent ticks.
- Fairness: ensure no category or recipient is starved; rotate recipients when draining.

Initial Categories (candidates)
- Broadcast drain (threaded broadcasting): number of batches per tick.
- Diagnostics/log drains: capped lines/entries per tick.
- Non-critical async completions (e.g., best-effort prefetch callbacks): cap callbacks/tick.

Policy
- Budgets are soft; emergency paths can override (guarded by config) to avoid user-visible stalls.
- Per-category budget + optional backoff when repeatedly exceeding.
- Rotation order for per-recipient drains to avoid head-of-line blocking.

Config (default-off)
- fairness.enabled: false
- fairness.broadcastDrain.budget: 64
- fairness.broadcastDrain.rotateRecipients: true
- fairness.diagnosticsDrain.budget: 32
- fairness.asyncCompletions.budget: 64
- fairness.debug: false

Metrics
- per-category: enqueued, drained, over-budget occurrences, rotatedRecipients.

Rollout Plan
- Phase 0: This design doc + config scaffolding.
- Phase 1: Implement budget check wrappers for targeted categories.
- Phase 2: Add adaptive backoff and better recipient rotation.

Testing
- Synthetic worst-case broadcasts to many recipients; verify stable TPS and consistent draining.
- Logging bursts: ensure logs remain readable without stalling server tick.
