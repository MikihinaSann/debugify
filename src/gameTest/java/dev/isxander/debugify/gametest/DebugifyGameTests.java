package dev.isxander.debugify.gametest;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.DebugifyConfig;
import dev.isxander.debugify.config.Issue2Tweak;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.fixes.FixCategory;
import dev.isxander.debugify.fixes.OS;
import dev.isxander.debugify.helpers.mc2164.EnderPearlTeleportSafety;
import dev.isxander.debugify.mixinplugin.DebugifyErrorHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * GameTests for the two real bug fixes (MC-95467, MC-2164), the issue-2 tweak
 * config model and misc assertions that must hold regardless of tweak state.
 */
@GameTestHolder(Debugify.MODID)
@PrefixGameTestTemplate(false)
public class DebugifyGameTests {
    static final String EMPTY = "empty";
    static final String BATCH = "fixes";

    /** Fixes are tweak-independent; baseline everything to off regardless of run order. */
    @BeforeBatch(batch = BATCH)
    public static void fixesBaseline(ServerLevel level) {
        for (Issue2Tweak tweak : Issue2Tweak.values()) {
            Debugify.CONFIG.setIssue2TweakEnabled(tweak, false);
        }
    }

    // -- Config model --

    @GameTest(template = EMPTY, batch = BATCH)
    public void configDefaultsAllTweaksOff(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload(); // file absent -> pure defaults
            for (Issue2Tweak tweak : Issue2Tweak.values()) {
                helper.assertFalse(config.isIssue2TweakEnabled(tweak), tweak.key() + " must default to off");
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void configMalformedTweakValuesStayOff(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            Files.writeString(file, """
                    {
                      "default_disabled": false,
                      "issue_2_tweaks": {
                        "water_breathing_protects_fish": "true",
                        "burning_snowballs": 1,
                        "cushioned_anvils": true,
                        "bogus_key": true
                      }
                    }
                    """);
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload();
            helper.assertTrue(config.isIssue2TweakEnabled(Issue2Tweak.CUSHIONED_ANVILS),
                    "explicit boolean true must enable the tweak");
            helper.assertFalse(config.isIssue2TweakEnabled(Issue2Tweak.WATER_BREATHING_PROTECTS_FISH),
                    "string \"true\" must not enable the tweak");
            helper.assertFalse(config.isIssue2TweakEnabled(Issue2Tweak.BURNING_SNOWBALLS),
                    "numeric 1 must not enable the tweak");
            helper.assertFalse(config.isIssue2TweakEnabled(Issue2Tweak.NETHER_CAULDRON_EVAPORATION),
                    "absent key must stay off");
            helper.assertFalse(config.isIssue2TweakEnabled(Issue2Tweak.END_PORTALS_REQUIRE_FRAMES),
                    "absent key must stay off");

            // A non-object issue_2_tweaks value must leave everything off too.
            Path file2 = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            Files.writeString(file2, "{ \"issue_2_tweaks\": true }");
            DebugifyConfig config2 = new DebugifyConfig(file2);
            config2.preload();
            for (Issue2Tweak tweak : Issue2Tweak.values()) {
                helper.assertFalse(config2.isIssue2TweakEnabled(tweak), "non-object tweaks value must stay off");
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void configRoundtripPreservesFixesAndTweaks(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload();
            BugFixData fix = new BugFixData("MC-95467", FixCategory.BASIC, BugFix.Env.SERVER, true, List.of(), OS.UNKNOWN);
            config.registerBugFix(fix);
            config.setIssue2TweakEnabled(Issue2Tweak.UNDERWATER_CAULDRONS, true);
            config.save();

            String json = Files.readString(file);
            helper.assertTrue(json.contains("\"MC-95467\": true"), "saved file must keep MC-* settings: " + json);
            helper.assertTrue(json.contains("\"issue_2_tweaks\""), "saved file must contain the tweaks object");
            for (Issue2Tweak tweak : Issue2Tweak.values()) {
                helper.assertTrue(json.contains(tweak.key()), "saved file must contain key " + tweak.key());
            }

            DebugifyConfig reloaded = new DebugifyConfig(file);
            reloaded.preload();
            helper.assertTrue(reloaded.isIssue2TweakEnabled(Issue2Tweak.UNDERWATER_CAULDRONS),
                    "enabled tweak must survive the roundtrip");
            helper.assertFalse(reloaded.isIssue2TweakEnabled(Issue2Tweak.CUSHIONED_ANVILS),
                    "disabled tweak must survive the roundtrip");
            BugFixData fix2 = new BugFixData("MC-95467", FixCategory.BASIC, BugFix.Env.SERVER, true, List.of(), OS.UNKNOWN);
            reloaded.registerBugFix(fix2);
            helper.assertTrue(reloaded.isBugFixEnabled(fix2), "MC flag must survive the roundtrip");
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    // -- Mixin application health --

    @GameTest(template = EMPTY, batch = BATCH)
    public void fixesAppliedWithoutMixinErrors(GameTestHelper helper) {
        Set<String> ours = Set.of("MC-2164", "MC-95467");
        for (BugFixData data : DebugifyErrorHandler.getErrored()) {
            helper.assertFalse(ours.contains(data.bugId()), "mixin errored: " + data.bugId());
        }
        for (BugFixData data : DebugifyErrorHandler.getAlreadyFixed()) {
            helper.assertFalse(ours.contains(data.bugId()), "mixin skipped as already-fixed: " + data.bugId());
        }
        helper.succeed();
    }

    // -- MC-95467 (door breaking) --

    private TestAccess.DoorGoal doorGoal(GameTestHelper helper, Mob mob, BlockPos doorPos) {
        TestAccess.DoorGoal goal = new TestAccess.DoorGoal(mob);
        goal.targetDoor(helper.absolutePos(doorPos));
        goal.start();
        return goal;
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void zombieBreaksOriginalWoodenDoor(GameTestHelper helper) {
        BlockPos door = new BlockPos(4, 1, 4);
        helper.setBlock(door, Blocks.OAK_DOOR);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 3));
        TestAccess.DoorGoal goal = doorGoal(helper, zombie, door);
        for (int i = 0; i < 245 && goal.canContinueToUse(); i++) {
            goal.tick();
        }
        helper.assertBlockNotPresent(Blocks.OAK_DOOR, door);
        helper.succeed();
    }

    private void replacedDoorSurvives(GameTestHelper helper, Mob mob, net.minecraft.world.level.block.Block replacement) {
        BlockPos door = new BlockPos(4, 1, 4);
        helper.setBlock(door, Blocks.OAK_DOOR);
        TestAccess.DoorGoal goal = doorGoal(helper, mob, door);
        for (int i = 0; i < 200 && goal.canContinueToUse(); i++) {
            goal.tick();
        }
        helper.getLevel().setBlock(helper.absolutePos(door), replacement.defaultBlockState(), 3);
        helper.assertFalse(goal.canContinueToUse(), "goal must stop once the door is replaced with " + replacement);
        for (int i = 0; i < 60; i++) {
            goal.tick();
        }
        helper.assertBlockPresent(replacement, door);
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void zombieCannotBreakBedrockReplacingDoor(GameTestHelper helper) {
        replacedDoorSurvives(helper, helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 3)), Blocks.BEDROCK);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void zombieCannotBreakStoneReplacingDoor(GameTestHelper helper) {
        replacedDoorSurvives(helper, helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 3)), Blocks.STONE);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void zombieCannotBreakIronDoorReplacingDoor(GameTestHelper helper) {
        replacedDoorSurvives(helper, helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 3)), Blocks.IRON_DOOR);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void zombieCannotBreakAlternateWoodenDoor(GameTestHelper helper) {
        replacedDoorSurvives(helper, helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 3)), Blocks.SPRUCE_DOOR);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void vindicatorCannotBreakObsidianReplacingDoor(GameTestHelper helper) {
        replacedDoorSurvives(helper, helper.spawnWithNoFreeWill(EntityType.VINDICATOR, new BlockPos(4, 1, 3)), Blocks.OBSIDIAN);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH, timeoutTicks = 200)
    public void replacementOnFinalTickIsNotRemoved(GameTestHelper helper) {
        BlockPos door = new BlockPos(4, 1, 4);
        helper.setBlock(door, Blocks.OAK_DOOR);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 1, 3));
        TestAccess.DoorGoal goal = doorGoal(helper, zombie, door);
        for (int i = 0; i < 239 && goal.canContinueToUse(); i++) {
            goal.tick();
        }
        helper.getLevel().setBlock(helper.absolutePos(door), Blocks.BEDROCK.defaultBlockState(), 3);
        // The 240th tick is where vanilla would delete the block; the tick
        // guard must veto it even though canContinueToUse already ran.
        goal.tick();
        helper.assertBlockPresent(Blocks.BEDROCK, door);
        helper.succeed();
    }

    // -- MC-2164 (ender pearl destination) --

    private ServerPlayer mockPlayerAt(GameTestHelper helper, BlockPos rel) {
        @SuppressWarnings("deprecation")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(rel)));
        return player;
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlFreeTargetTeleportsPlayer(GameTestHelper helper) {
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 target = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 5, 6)));
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
        pearl.setPos(target);
        pearl.hit(new BlockHitResult(target, Direction.UP, BlockPos.containing(target), false));
        helper.assertTrue(player.position().distanceTo(target) < 0.01,
                "player should teleport to the free landing spot, at " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlFreeTargetTeleportsNonPlayer(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        Vec3 target = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 5, 6)));
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), pig);
        pearl.setPos(target);
        pearl.hit(new BlockHitResult(target, Direction.UP, BlockPos.containing(target), false));
        helper.assertTrue(pig.position().distanceTo(target) < 0.01,
                "pig should teleport to the free landing spot, at " + pig.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlWallStaysOnIncomingSide(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(2, 1, 4));
        // Pearl lands embedded against the east face of the wall.
        Vec3 hitLoc = helper.absoluteVec(new Vec3(6.0, 2.0, 4.0));
        BlockHitResult hit = new BlockHitResult(hitLoc, Direction.EAST, helper.absolutePos(new BlockPos(5, 2, 4)), false);
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
        pearl.setPos(hitLoc);
        pearl.hit(hit);
        helper.assertTrue(player.getX() > hitLoc.x, "player must land on the incoming side, at " + player.position());
        helper.assertTrue(player.getX() < hitLoc.x + 1.0, "player must stay adjacent to the face, at " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlFullyBlockedIsRejected(GameTestHelper helper) {
        // Solid 3x3x3 stone box; the destination sits at its centre.
        for (int x = 3; x <= 5; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 3; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 start = player.position();
        Vec3 hitLoc = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
        pearl.setPos(hitLoc);
        pearl.hit(new BlockHitResult(hitLoc, Direction.UP, helper.absolutePos(new BlockPos(4, 1, 4)), false));
        helper.assertTrue(player.position().distanceTo(start) < 0.01,
                "rejected teleport must leave the player put, moved to " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlUndersideHitLandsBelow(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 hitLoc = helper.absoluteVec(new Vec3(4.5, 3.0, 4.5));
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
        pearl.setPos(hitLoc);
        pearl.hit(new BlockHitResult(hitLoc, Direction.DOWN, helper.absolutePos(new BlockPos(4, 3, 4)), false));
        helper.assertTrue(player.getY() < helper.absolutePos(new BlockPos(0, 3, 0)).getY(),
                "player should land below the struck ceiling, at " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlEntityHitCollisionIsRejected(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE);
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(7, 1, 7));
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 start = player.position();
        Vec3 hitLoc = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
        pearl.setPos(hitLoc);
        pearl.hit(new EntityHitResult(pig));
        helper.assertTrue(player.position().distanceTo(start) < 0.01,
                "entity-hit collision must be rejected, moved to " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlSafetyUsesLiveBoundingBox(GameTestHelper helper) {
        // Floor top at y=1.5 (4-layer snow), ceiling slab bottom at y=2.0 -> 0.5 gap.
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 4));
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE_SLAB);
        Vec3 dest = helper.absoluteVec(new Vec3(4.5, 1.5, 4.5));
        Pig adult = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 4));
        Pig baby = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 5));
        baby.setBaby(true);
        helper.assertFalse(EnderPearlTeleportSafety.isDestinationSafe(helper.getLevel(), adult, dest),
                "adult pig box must not fit a half-block gap");
        helper.assertTrue(EnderPearlTeleportSafety.isDestinationSafe(helper.getLevel(), baby, dest),
                "baby pig box must fit a half-block gap");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlRedirectedTargetHonouredWhenSafe(GameTestHelper helper) {
        // A wall sits where the pearl lands; another mod redirects the event
        // target to a free spot - it must be honoured exactly, not adjusted.
        for (int y = 1; y <= 3; y++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 redirect = Vec3.atCenterOf(helper.absolutePos(new BlockPos(7, 4, 7)));
        Vec3 hitLoc = Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 2, 4)));
        Consumer<EntityTeleportEvent.EnderPearl> listener = event -> {
            event.setTargetX(redirect.x);
            event.setTargetY(redirect.y);
            event.setTargetZ(redirect.z);
        };
        NeoForge.EVENT_BUS.addListener(EntityTeleportEvent.EnderPearl.class, listener);
        try {
            TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
            pearl.setPos(hitLoc);
            pearl.hit(new BlockHitResult(hitLoc, Direction.EAST, helper.absolutePos(new BlockPos(5, 2, 4)), false));
            helper.assertTrue(player.position().distanceTo(redirect) < 0.01,
                    "redirected event target must be used verbatim, at " + player.position());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlEventCancellationStillApplies(GameTestHelper helper) {
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 start = player.position();
        Vec3 target = Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 4, 6)));
        Consumer<EntityTeleportEvent.EnderPearl> listener = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(EntityTeleportEvent.EnderPearl.class, listener);
        try {
            TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
            pearl.setPos(target);
            pearl.hit(new BlockHitResult(target, Direction.UP, BlockPos.containing(target), false));
            helper.assertTrue(player.position().distanceTo(start) < 0.01,
                    "cancelled pearl event must not teleport, moved to " + player.position());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlThinBlocksStayOnIncomingSide(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int y = 1; y <= 3; y++) {
            level.setBlock(helper.absolutePos(new BlockPos(5, y, 3)), Blocks.CACTUS.defaultBlockState(), 2);
            level.setBlock(helper.absolutePos(new BlockPos(5, y, 4)), Blocks.OAK_FENCE.defaultBlockState(), 2);
            level.setBlock(helper.absolutePos(new BlockPos(5, y, 5)), Blocks.GLASS_PANE.defaultBlockState(), 2);
        }
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(2, 1, 3));
        // Hit each block's actual east collision face: cactus +15/16,
        // fence +10/16, glass pane +9/16 of its block position.
        assertThinFace(helper, level, player, new BlockPos(5, 2, 3), 15.0 / 16.0, "cactus");
        assertThinFace(helper, level, player, new BlockPos(5, 2, 4), 10.0 / 16.0, "fence");
        assertThinFace(helper, level, player, new BlockPos(5, 2, 5), 9.0 / 16.0, "glass pane");
        helper.succeed();
    }

    private void assertThinFace(GameTestHelper helper, ServerLevel level, ServerPlayer player,
                                BlockPos rel, double faceOffset, String name) {
        BlockPos absBlock = helper.absolutePos(rel);
        Vec3 hitLoc = new Vec3(absBlock.getX() + faceOffset, absBlock.getY() + 0.5, absBlock.getZ() + 0.5);
        TestAccess.Pearl pearl = new TestAccess.Pearl(level, player); // fresh pearl per hit
        pearl.setPos(hitLoc);
        pearl.hit(new BlockHitResult(hitLoc, Direction.EAST, absBlock, false));
        helper.assertTrue(player.getX() > hitLoc.x, name + ": owner must stay on incoming side, at " + player.position());
        helper.assertTrue(player.getX() < hitLoc.x + 1.0, name + ": owner must stay adjacent, at " + player.position());
        helper.assertTrue(level.noBlockCollision(player, player.getBoundingBox()),
                name + ": final bbox must be collision free, at " + player.position());
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlCornerBlockedIsRejected(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Wall struck face plus a second wall blocking the incoming side.
        for (int y = 1; y <= 3; y++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
            helper.setBlock(new BlockPos(6, y, 4), Blocks.STONE);
        }
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(2, 1, 4));
        Vec3 start = player.position();
        Vec3 hitLoc = helper.absoluteVec(new Vec3(6.0, 2.0, 4.0));
        TestAccess.Pearl pearl = new TestAccess.Pearl(level, player);
        pearl.setPos(hitLoc);
        pearl.hit(new BlockHitResult(hitLoc, Direction.EAST, helper.absolutePos(new BlockPos(5, 2, 4)), false));
        helper.assertTrue(player.position().distanceTo(start) < 0.01,
                "blocked incoming side must reject the teleport, moved to " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlShrunkPoseFitsSmallGap(GameTestHelper helper) {
        // 1.0 block gap: floor top y=1.0, ceiling bottom-slab at y=2.
        helper.setBlock(new BlockPos(4, 0, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.STONE_SLAB);
        Vec3 gap = helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        player.setPose(net.minecraft.world.entity.Pose.SWIMMING);
        player.refreshDimensions();
        TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
        pearl.setPos(gap);
        pearl.hit(new BlockHitResult(gap, Direction.UP, helper.absolutePos(new BlockPos(4, 0, 4)), false));
        helper.assertTrue(player.position().distanceTo(gap) < 0.01,
                "swimming-sized owner must fit the small gap, at " + player.position());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public void pearlUnsafeRedirectIsRejected(GameTestHelper helper) {
        for (int y = 1; y <= 3; y++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(5, y, z), Blocks.STONE);
            }
        }
        ServerPlayer player = mockPlayerAt(helper, new BlockPos(1, 1, 1));
        Vec3 start = player.position();
        Vec3 inside = Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 2, 4)));
        Vec3 hitLoc = Vec3.atCenterOf(helper.absolutePos(new BlockPos(7, 3, 4)));
        Consumer<EntityTeleportEvent.EnderPearl> listener = event -> {
            event.setTargetX(inside.x);
            event.setTargetY(inside.y);
            event.setTargetZ(inside.z);
        };
        NeoForge.EVENT_BUS.addListener(EntityTeleportEvent.EnderPearl.class, listener);
        try {
            TestAccess.Pearl pearl = new TestAccess.Pearl(helper.getLevel(), player);
            pearl.setPos(hitLoc);
            pearl.hit(new BlockHitResult(hitLoc, Direction.UP, BlockPos.containing(hitLoc), false));
            helper.assertTrue(player.position().distanceTo(start) < 0.01,
                    "an unsafe redirected target must be rejected, moved to " + player.position());
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    // -- Deferred request: correct /kill selector --

    @GameTest(template = EMPTY, batch = BATCH)
    public void validExcludePlayerKillSelector(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 4));
        Player player = mockPlayerAt(helper, new BlockPos(4, 1, 4));
        Vec3 center = Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
        ServerLevel level = helper.getLevel();
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack(),
                "execute positioned %f %f %f run kill @e[type=!minecraft:player,distance=..6]".formatted(center.x, center.y, center.z));
        helper.assertTrue(player.isAlive(), "the !minecraft:player selector must keep players alive");
        helper.assertFalse(pig.isAlive(), "the !minecraft:player selector must kill non-players");
        helper.succeed();
    }
}
