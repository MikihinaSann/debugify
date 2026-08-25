# Debugify Forge 1.20.1 — Changelog

## 1.20.1+1.0 (Initial Forge Release)

### Added

- **Forge 1.20.1 port** of isXander's Debugify (upstream Fabric `archive/1.20` branch, originally targeting MC 1.20.6).
- **70+ bug fixes** from the Mojang bug tracker across two categories:
  - **Basic Fixes (34 server + 38 client)** — Safe, non-intrusive fixes including:
    - MC-2025: Entity hitbox floating-point inaccuracy on chunk load
    - MC-7569: RCON output missing newlines between messages
    - MC-14923: Singleplayer host kicked for chat spam despite being owner
    - MC-224729: Chunks not saved during `/save-all` if never accessed
    - MC-124117: Effects, XP, and abilities lost after cross-dimensional teleport
    - MC-119417, MC-129909, MC-215530, MC-69216: Spectator mode edge cases (sleeping, item use, frozen state, fishing hook)
    - MC-135971: Ctrl+Q not emptying crafting result slot
    - MC-100991: Fishing hook damage not recorded in combat tracker
    - MC-237493: Telemetry disable option in telemetry menu
    - MC-199467: Sin/cos integer overflow causing entity jitter
    - ...and many more
  - **Gameplay Fixes (3)** — Fixes that alter gameplay mechanics:
    - MC-31819: Peaceful difficulty no longer adds exhaustion (server)
    - MC-8187: 2x2 sapling placement offset at map edges (server)
    - MC-12829: Spectator/flying players can climb ladders (client, gated behind "Enable In Multiplayer" toggle)
- **Smart conflict detection** — Mixin error handler detects when an injection target is not found (because Forge or another mod already fixed the bug) and skips gracefully instead of crashing. Startup log reports applied, skipped, and failed fix counts.
- **Forge-native config screen** — In-game configuration accessible from the Mods menu, with per-fix toggles, "Enable In Multiplayer" gate for gameplay fixes, and "Default to Disabled" option.
- **Persistent configuration** — Settings saved to `config/debugify.json`.
- **Mod conflict detection** — Automatically disables fixes that conflict with Carpet, ChunkSavingFix, No Telemetry, flwr-8187, MCMouser, Title-Fix, and Ctrl-Q.
- **Client/server independence** — Client and server can run Debugify independently without requiring it on both sides.
- **Multi-language support** — English, Spanish (Mexico), French, Portuguese (Brazil), Russian, Turkish, Ukrainian, Vietnamese, Chinese (Simplified & Traditional), Greek.

### Changed

- Ported all Fabric-specific code to Forge equivalents:
  - `FabricLoader` → `Forge ModList` / `FMLPaths`
  - Fabric entrypoints → `InterModComms`
  - Fabric config paths → `FMLPaths.CONFIGDIR`
  - YACL/Mod Menu config → custom Forge `ConfigScreenHandler.ConfigScreenFactory`
  - Client init routed through `DistExecutor.unsafeRunWhenOn` for dedicated-server safety
- Bundled MixinExtras Forge via Jar-in-Jar.
- Access transformer at `META-INF/accesstransformer.cfg`.
- Replaced `fabric.mod.json` and accesswidener with `mods.toml` and `pack.mcmeta`.

### Fixed (Porting Adjustments for 1.20.1)

- **MC-93384**: Retargeted mixin from `LivingEntity.baseTick` → `spawnSoulSpeedParticle` (particle call was inlined differently in 1.20.1).
- **MC-112730**: Retargeted from `SectionRenderDispatcher$RenderSection$RebuildTask` → `ChunkRenderDispatcher$RenderChunk$RebuildTask` (class renamed in 1.20.2+).
- **MC-577**: Replaced `LocalCapture` (local variable table changed in 1.20.1) with `@Shadow hoveredSlot` field.
- **MC-215531**: Retargeted from `Gui.renderCameraOverlays` → `Gui.render` (method inlined in 1.20.1).
- **MC-4490**: Retargeted from `FishingHookRenderer.getPlayerHandPos` → full `render` method (helper method inlined in 1.20.1).
- **MC-176559**: Rewrote item comparison from 1.20.6 data-component API to 1.20.1 NBT `CompoundTag` comparison.
- **MC-237493**: Adjusted telemetry method from `createTelemetryCheckbox` → `createTelemetryButton`.
- **MC-231743**: Downgraded `ItemInteractionResult`/`useItemOn` → `InteractionResult`/`use`.
- **MC-124117**: Removed 1.20.2+ 3-argument `ClientboundUpdateMobEffectPacket` constructor.
- **MC-143474, MC-159163**: Removed 1.20.2+ `ClientCommonPacketListenerImpl`/`CommonListenerCookie` hierarchy.
- **MC-14923**: Removed 1.20.2+ `ServerCommonPacketListenerImpl`/`CommonListenerCookie` hierarchy.
- **MC-111516**: Adjusted `setupRotations` descriptor from 4 floats to 3 floats.
- **MC-121706**: Adjusted shadow generics for `Mob` & `RangedAttackMob`, retargeted `lookAt` to `Mob`.

### Verified

- Compilation: `compileJava` passes with 0 errors.
- Dedicated server: Starts successfully, 32 fixes enabled, 33 mixins applied, 0 errors.
- Client: Reaches main menu, all 38 client mixins apply cleanly, clean shutdown.
- Final artifact: `debugify-1.20.1+1.0.jar` (~309 KB, reobfuscated).

### Requirements

- Minecraft 1.20.1
- Forge 47.x (tested on 47.4.10)
- Java 17

### Credits

- **Original mod:** isXander — [Debugify](https://github.com/isXander/Debugify) (Fabric)
- **Forge port:** MikihinaSann
- **Code used under LGPLv3:** j-Tai's TieFix, FlashyReese's Sodium Extra
- **Code used under Zlib:** Ampflower's 2x2 Surrounded Saplings Fix (MC-8187)

### License

LGPLv3
