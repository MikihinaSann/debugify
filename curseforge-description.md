# Debugify (Forge 1.20.1)

A Forge port of [isXander's Debugify](https://github.com/isXander/Debugify) for Minecraft 1.20.1. Debugify fixes bugs found on the [Mojang bug tracker](https://bugs.mojang.com) that Mojang hasn't gotten around to fixing yet — improving gameplay stability, multiplayer fairness, and quality of life without changing core game mechanics.

## Features

- **70+ bug fixes** from the Mojang bug tracker, organized into two categories:
  - **Basic Fixes** — Safe, non-intrusive bug fixes that don't alter gameplay balance (e.g. chunk saving, cross-dimensional teleport effects, spectator mode edge cases, RCON formatting, combat tracking).
  - **Gameplay Fixes** — Fixes that change gameplay mechanics and may provide an advantage in multiplayer. Disabled by default in multiplayer via the "Enable In Multiplayer" toggle.
- **Smart conflict detection** — Automatically skips fixes that have already been applied by Forge or other mods, preventing crashes and redundant patches. The startup log reports how many fixes were applied, skipped, or failed.
- **Works on both sides** — Install on a dedicated server to fix server-side bugs for all connected players, and/or on the client to fix client-side rendering, UI, and input bugs. Client and server can run independently without requiring each other.
- **In-game configuration screen** — A Forge-native config screen accessible from the mod list, letting you toggle individual fixes on or off without editing JSON files.
- **Mod compatibility** — Detects and avoids conflicts with other mods that fix the same bugs (Carpet, ChunkSavingFix, No Telemetry, flwr-8187, and more).
- **Persistent configuration** — Settings are saved to `config/debugify.json` and persist across restarts.

## Installation

1. Install [Minecraft Forge 47.x](https://files.minecraftforge.net/) for Minecraft 1.20.1.
2. Place the `debugify-1.20.1+1.0.jar` file in your `.minecraft/mods` folder (client) or `mods/` folder (server).
3. Launch the game. The mod works out of the box with sensible defaults.

**Server installation:** Drop the jar in your server's `mods/` folder. Server-side fixes (32 mixins) apply automatically to all connected players. No client-side installation required for server fixes.

**Client installation:** Drop the jar in your client's `mods/` folder. Client-side fixes (38 mixins) apply on top of vanilla behavior. A config screen is available from the Mods menu.

## Requirements

- Minecraft 1.20.1
- Forge 47.x (tested on 47.4.10)
- Java 17

## Credits

- **Original mod:** [isXander](https://github.com/isXander) — Debugify (Fabric, upstream `archive/1.20` branch)
- **Forge port:** MikihinaSann
- **Code used under LGPLv3:** j-Tai's TieFix, FlashyReese's Sodium Extra
- **Code used under Zlib:** Ampflower's 2x2 Surrounded Saplings Fix (MC-8187)

## License

LGPLv3 — see [LICENSE.txt](https://github.com/MikihinaSann/debugify/blob/main/LICENSE.txt)
