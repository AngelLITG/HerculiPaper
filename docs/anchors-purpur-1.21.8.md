# Anchors — Purpur 1.21.8 (Summary)

This document collects anchor checks/findings against Purpur 1.21.8.

- Networking
  - `net.minecraft.network.Connection#send(...)`: present.
  - `ServerGamePacketListenerImpl#keepConnectionAlive()`: present.
- Commands/UI
  - `GiveCommand`, `ClearInventoryCommands`: present.
  - `BaseCommandBlock#sendSystemMessage(...)`: present.
- Entity/Tracking
  - `ChunkMap` (TrackedEntity.seenBy), `ServerPlayer`, `ServerPlayerGameMode`: present.

For detailed notes per patch, see `docs/patch-summaries.md`.
