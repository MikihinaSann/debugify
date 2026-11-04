# Debugify (NeoForge port)

## Toolchain

- Minecraft 1.21.1, NeoForge 21.1.236, ModDev Gradle 2.0.147, Java 21
  (`C:\Program Files\Java\jdk-21.0.12` works; `org.gradle.daemon=false` so each
  invocation spawns a single-use daemon).

## Useful commands

- `./gradlew.bat build` — full build (jar lands in `build/libs/`).
- `./gradlew.bat runGameTestServer` — runs all GameTests under the `debugify`
  namespace and exits with the server result summary.
- `./gradlew.bat runClient` — dev client (quick-plays `run/saves/testworld`).

## Test layout

- GameTests live in `src/gameTest/` (dev-only, never shipped in the jar):
  `DebugifyGameTests` (fixes batch), `TweaksGameTests` (issue2 on/off batches),
  `Issue3GameTests` (issue3On/issue3Off batches), `TestAccess` helpers.
- Structure template: `src/gameTest/resources/data/debugify/structure/empty.nbt`.
- The GameTest server shares `run/` (config at `run/config/debugify.json`);
  a fix can be disabled there to prove a test exercises its mixin.

## Conventions

- Bug fixes are declared with `@BugFix` and registered in
  `dev.isxander.debugify.mixinplugin.MixinPlugin`.
- Optional issue-2/issue-3 tweaks are off by default, gated on
  `Debugify.CONFIG` at runtime, always boolean-strict in `debugify.json`.
- MixinExtras available on this NeoForge version: `WrapOperation`,
  `WrapWithCondition`, `ModifyExpressionValue`, `ModifyReceiver`,
  `ModifyReturnValue` (`com.llamalad7.mixinextras.injector` / `.wrapoperation`).
  `ModifyArg`/`ModifyArgs` belong to `org.spongepowered.asm.mixin.injection`,
  not MixinExtras.
- GUI automation note: `orca computer` sees only window chrome for the GLFW
  Minecraft window (7-element a11y tree); coordinate clicks/keys are needed for
  in-game navigation and its screenshots do land, but verify clicks carefully.
