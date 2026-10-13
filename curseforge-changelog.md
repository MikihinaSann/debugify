# Debugify NeoForge 1.21.1 — Changelog

## 1.21.1+1.1 (Initial NeoForge Release)

### Added

- **NeoForge 1.21.1 port** of isXander's Debugify (upstream Fabric `archive/1.21` branch).
- **58 bug fixes** from the Mojang bug tracker across two categories:
  - **Basic Fixes (26 server + 29 client)** — Safe, non-intrusive fixes including:
    - MC-2025: Entity hitbox floating-point inaccuracy on chunk load
    - MC-7569: RCON output missing newlines between messages
    - MC-224729: Chunks not saved during `/save-all` if never accessed
    - MC-119417, MC-129909, MC-215530, MC-69216: Spectator mode edge cases (sleeping, item use, frozen state, fishing hook)
    - MC-135971: Ctrl+Q not emptying crafting result slot
    - MC-100991: Fishing hook damage not recorded in combat tracker
    - MC-237493: Telemetry disable option in telemetry menu
    - MC-199467: Sin/cos integer overflow causing entity jitter
    - MC-223153: Copper blocks missing sounds
    - MC-93384: Drowning bubbles spawning at feet instead of eye level
    - MC-121706: Bow-wielding mobs not looking at their target
    - MC-132878: Armor stands not spawning break particles
    - MC-179072: Creepers igniting on non-enemy entities
    - MC-200418: Baby villagers not dismounting chickens on conversion
    - MC-160095: Cactus breaking wrong block vs piston head
    - MC-155509: Dead pufferfish still stinging
    - MC-79545: XP level not clamped at integer limit
    - ...and many more
  - **Gameplay Fixes (3)** — Fixes that alter gameplay mechanics:
    - MC-12829: Spectator/flying players can climb ladders (client)
    - MC-231097: Client sends "release using item" packet on drop (client)
    - MC-8187: 2x2 sapling placement offset at map edges (server)
- **Smart conflict detection** — Mixin error handler detects injection targets already fixed by NeoForge or another mod and skips gracefully instead of crashing. Startup log reports enabled, skipped, and failed fix counts.
- **NeoForge-native config screen** — In-game configuration accessible from the Mods menu, with per-fix toggles and "Enable In Multiplayer" gate for gameplay fixes.
- **Persistent configuration** — Settings saved to `config/debugify.json`.
- **Mod conflict detection** — Automatically disables fixes that conflict with Carpet, Carpet-Fixes, ChunkSavingFix, Ctrl-Q, flwr-8187, MCMouser, No Telemetry, Title-Fix, and NeoForge itself where it already ships the same fix.
- **Client/server independence** — Client and server can run Debugify independently without requiring it on both sides.
- **Multi-language support** — English, Greek, French, Italian, Portuguese (Brazil), Russian, Turkish, Ukrainian, Chinese (Simplified & Traditional).
- **Third-party conflict API** — Other mods can ask Debugify to disable specific fixes via `[modproperties.debugify]` in their own `neoforge.mods.toml` (replaces upstream's Fabric entrypoint API).

### Changed

- Ported all Fabric-specific code to NeoForge equivalents:
  - `FabricLoader` → `ModList` / `LoadingModList` / `FMLPaths`
  - Fabric entrypoints → `@Mod` / NeoForge `IConfigScreenFactory`
  - Fabric access widener → `META-INF/accesstransformer.cfg`
  - YACL/Mod Menu config → custom NeoForge config screen
  - Flattened `src/client` into `src/main` (single source set, client classes behind `dist = Dist.CLIENT`)
  - `fabric.mod.json` → `neoforge.mods.toml`
- Uses NeoForge-bundled MixinExtras (`mixinextras-neoforge`).

### Fixed (NeoForge Patch Adaptations)

NeoForge rewrites parts of vanilla bytecode, so several upstream mixins needed retargeting:

- **MC-93384**: Vanilla's `LivingEntity#baseTick` bubble code is dead under NeoForge (`if (false)`); the bug moved to `CommonHooks#onLivingBreathe`. Retargeted there via `@Redirect`.
- **MC-577**: `mouseClicked` local-variable table shifted by NeoForge's patch; dropped local capture, recomputes hovered slot via `@Invoker`.
- **MC-121706**: `mob` field erases to `Mob` under NeoForge generics; shadow retyped and `lookAt` retargeted.
- **MC-176559**: Marked as conflicting with `neoforge` — NeoForge's `ItemStack#shouldCauseBlockBreakReset` already ignores the `DAMAGE` component for damageable items, so the fix ships in NeoForge itself.
- **Mixin compatibility**: `isModLoaded` now falls back to `LoadingModList` so `modConflicts` actually resolve during mixin loading (previously every conflict silently returned false).
- Fixed upstream lang-key typo `mc_176559` → `mc-176559`.

### Verified

- `gradlew build` produces `debugify-1.21.1+1.1.jar`.
- Client: boots to title screen and joins a singleplayer world with **zero mixin errors**; plays in-world cleanly.
- Dedicated server: boots clean, all common mixins apply, no client classes loaded.
- All `@Shadow`/`@Invoker`/`@Accessor` members and `method=` selectors verified against the patched NeoForge runtime.

### Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x (tested on 21.1.251)
- Java 21

### Credits

- **Original mod:** isXander — [Debugify](https://github.com/isXander/Debugify) (Fabric)
- **NeoForge port:** MikihinaSann
- **Code used under LGPLv3:** j-Tai's TieFix, FlashyReese's Sodium Extra
- **Code used under Zlib:** Ampflower's 2x2 Surrounded Saplings Fix (MC-8187)

### License

LGPLv3
