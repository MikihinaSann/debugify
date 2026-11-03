# GitHub issue #2 - reports, fixes and optional tweaks

This document maps every report/screenshot from
[issue #2](https://github.com/MikihinaSann/debugify/issues/2) to what Debugify now
does about it. Two of the reports are real bugs fixed by default; the rest are
optional behaviour tweaks that are **disabled by default**, applied
server-side only, and never negotiated with clients.

## Actual bug fixes (enabled by default, like every fix)

| Bug | Report | What we do |
|-----|--------|------------|
| MC-95467 | Zombies and vindicators keep "breaking" a door position that was replaced | `BreakDoorGoal` caches the original door `BlockState` on start. Both `canContinueToUse` and `tick` abort (and clear destroy progress) as soon as the block is no longer that exact door - whether it became stone, bedrock, obsidian, air, iron, or a different wooden door. Covers zombies and vindicators via the shared goal class. |
| MC-2164 | Ender pearls teleport entities inside solid blocks | Replaced the old dismount-neighbour search with a precise check: if the destination fits the owner's *actual current* bounding box (no block collision + inside world border), it is used unchanged - including midair and externally redirected valid targets. On collision with a block, exactly one candidate on the incoming side of the struck face is tried; if that does not fit either, the teleport is rejected and the owner stays put. Crouching, swimming, scaled and baby hitboxes are handled via live bounding-box offsets. The pearl is still consumed and normal landing effects apply; ender pearl teleport event cancellation and valid redirected targets are preserved, unsafe ones rejected. |

## Optional tweaks - all OFF by default

Enable via the config screen (*Optional Issue #2 Tweaks* section) or in
`config/debugify.json` under `"issue_2_tweaks": { "key": true }`. Keys must be
literal JSON booleans; absent or malformed entries stay off. All twelve keys:

| Config key | Behaviour when enabled |
|------------|------------------------|
| `water_breathing_protects_fish` | Water animals (fish, squid, pufferfish…) with Water Breathing keep their air supply out of water. |
| `jack_o_lantern_snow_golems` | Snow golems built with a jack o'lantern keep it as head equipment (persisted + rendered) and attack ~3x faster (~7 instead of 20 tick interval). Shearing removes the boost. |
| `underwater_cauldrons` | Cauldrons placed into water are placed full. Submerged cauldrons ignore powder snow buckets; lava buckets replace the cauldron with **obsidian** (documented design choice - vanilla has lava-filled cauldrons, but no obsidian-filled cauldron state, so the whole block is replaced). |
| `nether_cauldron_evaporation` | Water buckets poured into a cauldron in ultrawarm dimensions (e.g. the Nether) evaporate instead of filling. |
| `cushioned_anvils` | Falling anvils landing on slime or honey blocks keep their damage state. On honey the anvil may drop as an item because entities sink into the non-full block. |
| `burning_snowballs` | Snowballs that are on fire ignite their victim for 5 seconds. |
| `arrows_trigger_falling_blocks` | Arrows schedule a block tick on the falling block they hit, so unsupported sand/gravel drops when shot. |
| `restrict_seagrass_substrates` | Seagrass and tall seagrass only survive on dirt or sand. The issue screenshot actually showed **seagrass**, despite a caption implying sugar cane - this is a caption mismatch, not a second bug. |
| `prevent_unsupported_plant_growth` | Cactus/sugar cane random ticks refuse to grow when the column's base cannot survive. Vanilla still removes the invalid plant via its own scheduled ticks (MC-177987 is Won't Fix upstream). |
| `end_portals_require_frames` | Breaking a frame that completed an end portal removes the 9 portal blocks inside that original ring only - incomplete rings, corner filler and unrelated portals are untouched. |
| `lava_converts_waterlogged_coral` | Lava flowing over a waterlogged coral plant or fan hardens it into stone . Only coral plant/fan blocks; full coral blocks and other waterlogged blocks are untouched. |
| `villager_beds_explode_in_invalid_dimensions` | A villager attempting to sleep in a bed where `dimensionType.bedWorks` is false (Nether, End…) explodes the bed like a player respawn explosion instead of sleeping. This is destructive gameplay - it is opt-in. |

## Explicitly deferred

- **Screenshot 6 ("last bug")**: pistons pushing a furnace/chest block entity. Not
  implemented - flipping a movable-block-entity flag loses inventories and
  block-entity NBT. Requires an explicit movable-BE design plus persistence
  tests.
- **`/kill @e[type=!Minecraft:player]`**: the reported selector uses an invalid
  uppercase namespace. Selectors are not rewritten; the correct command is
  `/kill @e[type=!minecraft:player]`, which is covered by a test (mock players
  survive, non-players are killed).

## Testing

Run the dev-only GameTest suite:

```
gradlew runGameTestServer
```

The `gameTest` source set (tests + `empty.nbt` template) is only wired into the
`gameTestServer` run; none of it ships in the production jar. 57 tests cover
both fixes, every tweak on/off, config defaults/malformed/roundtrip, the
selector deferral, and mixin health.
