package dev.isxander.debugify.gametest;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;

/**
 * Behaviour tests for the optional issue-2 tweaks. Batches run sequentially,
 * so a single @BeforeBatch per batch safely pins the global config for every
 * test in it: everything on in "tweaksOn", everything off in "tweaksOff".
 */
@GameTestHolder(Debugify.MODID)
@PrefixGameTestTemplate(false)
public class TweaksGameTests {
    private static final String EMPTY = "empty";
    private static final String ON = "tweaksOn";
    private static final String OFF = "tweaksOff";

    @BeforeBatch(batch = ON)
    public static void enableAll(ServerLevel level) {
        for (Issue2Tweak tweak : Issue2Tweak.values()) {
            Debugify.CONFIG.setIssue2TweakEnabled(tweak, true);
        }
    }

    /** Belt and suspenders: always leave ON batches with everything off. */
    @net.minecraft.gametest.framework.AfterBatch(batch = ON)
    public static void resetAfterOn(ServerLevel level) {
        disableAll(level);
    }

    @BeforeBatch(batch = OFF)
    public static void disableAll(ServerLevel level) {
        offAll();
    }

    // Delayed-effect OFF tests run in their own batch so the disabled state
    // is guaranteed adjacent to the ticks they observe.
    @BeforeBatch(batch = "tweaksOffDelayed")
    public static void disableAllDelayed(ServerLevel level) {
        offAll();
    }

    private static void offAll() {
        for (Issue2Tweak tweak : Issue2Tweak.values()) {
            Debugify.CONFIG.setIssue2TweakEnabled(tweak, false);
        }
    }

    // -- A: water_breathing_protects_fish --

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void waterBreathingKeepsFishAlive(GameTestHelper helper) {
        LivingEntity[] fish = {
                helper.spawnWithNoFreeWill(EntityType.COD, new BlockPos(2, 1, 2)),
                helper.spawnWithNoFreeWill(EntityType.PUFFERFISH, new BlockPos(4, 1, 2)),
                helper.spawnWithNoFreeWill(EntityType.SQUID, new BlockPos(6, 1, 2))
        };
        for (LivingEntity f : fish) {
            f.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.WATER_BREATHING, 100000));
        }
        helper.runAfterDelay(40, () -> {
            for (LivingEntity f : fish) {
                helper.assertTrue(f.getAirSupply() >= f.getMaxAirSupply() - 5,
                        f.getType() + " should keep its air topped up, has " + f.getAirSupply());
                helper.assertTrue(f.getHealth() >= f.getMaxHealth() - 0.001F,
                        f.getType() + " must not take drowning damage");
            }
            fish[0].removeEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(fish[0].getAirSupply() < fish[0].getMaxAirSupply() - 10,
                        "air must drain again once the effect is removed");
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void waterBreathingDoesNotHelpFishWhenOff(GameTestHelper helper) {
        LivingEntity cod = helper.spawnWithNoFreeWill(EntityType.COD, new BlockPos(4, 1, 4));
        cod.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WATER_BREATHING, 100000));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(cod.getAirSupply() < cod.getMaxAirSupply() - 10,
                    "with the tweak off the fish must suffocate as usual");
            helper.succeed();
        });
    }

    // -- B: jack_o_lantern_snow_golems --

    private static final Field ATTACK_TIME = findAttackTimeField();

    private static Field findAttackTimeField() {
        try {
            Field field = RangedAttackGoal.class.getDeclaredField("attackTime");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setAttackTime(RangedAttackGoal goal, int value) {
        try {
            ATTACK_TIME.setInt(goal, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getAttackTime(RangedAttackGoal goal) {
        try {
            return ATTACK_TIME.getInt(goal);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private SnowGolem buildGolem(GameTestHelper helper, BlockPos base, Block head) {
        helper.setBlock(base, Blocks.SNOW_BLOCK);
        helper.setBlock(base.above(), Blocks.SNOW_BLOCK);
        // Flag 3 so the pumpkin's onPlace fires trySpawnGolem (helper.setBlock
        // only updates clients and never builds the golem).
        helper.getLevel().setBlock(helper.absolutePos(base.above(2)), head.defaultBlockState(), 3);
        return helper.findOneEntity(EntityType.SNOW_GOLEM);
    }

    private int attackInterval(GameTestHelper helper, SnowGolem golem, BlockPos targetRel) {
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, targetRel);
        golem.setTarget(target);
        RangedAttackGoal goal = new RangedAttackGoal(golem, 1.0, 20, 20, 15.0F);
        goal.canUse();
        setAttackTime(goal, 1);
        goal.tick(); // fires once, then resets the interval
        return getAttackTime(goal);
    }

    @GameTest(template = EMPTY, batch = ON)
    public void lanternGolemKeepsHeadPersistsAndBoosts(GameTestHelper helper) {
        SnowGolem golem = buildGolem(helper, new BlockPos(4, 1, 4), Blocks.JACK_O_LANTERN);
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).is(Items.JACK_O_LANTERN),
                "lantern-built golem must keep its lantern in the head slot");
        helper.assertTrue(golem.hasPumpkin(), "golem must still count as pumpkin-headed");

        CompoundTag tag = new CompoundTag();
        golem.saveWithoutId(tag);
        helper.assertTrue(tag.toString().contains("jack_o_lantern"),
                "head equipment must persist in entity NBT");
        SnowGolem revived = EntityType.SNOW_GOLEM.create(helper.getLevel());
        revived.load(tag);
        helper.assertTrue(revived.getItemBySlot(EquipmentSlot.HEAD).is(Items.JACK_O_LANTERN),
                "head equipment must survive a save/load cycle");

        helper.assertValueEqual(attackInterval(helper, golem, new BlockPos(4, 1, 6)), 7, "lantern golem attack interval");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void shearedLanternGolemLosesBoost(GameTestHelper helper) {
        SnowGolem golem = buildGolem(helper, new BlockPos(4, 1, 4), Blocks.JACK_O_LANTERN);
        golem.shear(SoundSource.PLAYERS);
        helper.assertFalse(golem.hasPumpkin(), "sheared golem must lose its pumpkin flag");
        helper.assertValueEqual(attackInterval(helper, golem, new BlockPos(4, 1, 6)), 20, "sheared golem attack interval");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void carvedPumpkinGolemIsNotBoosted(GameTestHelper helper) {
        SnowGolem golem = buildGolem(helper, new BlockPos(4, 1, 4), Blocks.CARVED_PUMPKIN);
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                "carved-pumpkin golem must not gain head equipment");
        helper.assertValueEqual(attackInterval(helper, golem, new BlockPos(4, 1, 6)), 20, "carved golem attack interval");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void lanternGolemVanillaWhenOff(GameTestHelper helper) {
        SnowGolem golem = buildGolem(helper, new BlockPos(4, 1, 4), Blocks.JACK_O_LANTERN);
        helper.assertTrue(golem.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                "with the tweak off a lantern golem gets no head equipment");
        helper.assertValueEqual(attackInterval(helper, golem, new BlockPos(4, 1, 6)), 20, "lantern golem attack interval when off");
        helper.succeed();
    }

    // -- C: underwater_cauldrons --

    private static BlockHitResult hitAt(ServerLevel level, BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @GameTest(template = EMPTY, batch = ON)
    public void underwaterCauldronBehaviour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        // Placing a cauldron into water yields a full water cauldron.
        BlockPos spot = new BlockPos(4, 1, 4);
        helper.setBlock(spot, Blocks.WATER);
        Player placer = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absSpot = helper.absolutePos(spot);
        ((BlockItem) Items.CAULDRON).place(new BlockPlaceContext(
                level, placer, InteractionHand.MAIN_HAND, new ItemStack(Items.CAULDRON), hitAt(level, absSpot)));
        helper.assertBlockState(spot, s -> s.is(Blocks.WATER_CAULDRON) && s.getValue(LayeredCauldronBlock.LEVEL) == 3,
                () -> "expected a full water cauldron, got " + helper.getBlockState(spot));

        // A submerged empty cauldron ignores powder snow buckets entirely.
        BlockPos submerged = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlock(submerged, Blocks.CAULDRON.defaultBlockState(), 3);
        level.setBlock(submerged.above(), Blocks.WATER.defaultBlockState(), 3);
        Player powderUser = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack powder = new ItemStack(Items.POWDER_SNOW_BUCKET);
        ItemInteractionResult powderResult = level.getBlockState(submerged)
                .useItemOn(powder, level, powderUser, InteractionHand.MAIN_HAND, hitAt(level, submerged));
        helper.assertTrue(powderResult.consumesAction(), "submerged powder bucket should report success");
        helper.assertBlock(new BlockPos(6, 1, 6), b -> b == Blocks.CAULDRON, "cauldron must stay empty under water");
        helper.assertTrue(powder.getCount() == 1, "powder bucket must not be consumed");

        // A lava bucket on a submerged cauldron replaces it with obsidian.
        Player lavaUser = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
        lavaUser.setItemInHand(InteractionHand.MAIN_HAND, lava);
        level.getBlockState(submerged)
                .useItemOn(lava, level, lavaUser, InteractionHand.MAIN_HAND, hitAt(level, submerged));
        helper.assertBlock(new BlockPos(6, 1, 6), b -> b == Blocks.OBSIDIAN,
                "lava bucket must replace a submerged cauldron with obsidian");
        helper.assertTrue(lavaUser.getMainHandItem().is(Items.BUCKET),
                "survival lava bucket must be consumed to an empty bucket");

        // Creative players keep their bucket, like vanilla empty-bucket logic.
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        ItemStack creativeLava = new ItemStack(Items.LAVA_BUCKET);
        creative.setItemInHand(InteractionHand.MAIN_HAND, creativeLava);
        level.setBlock(submerged, Blocks.CAULDRON.defaultBlockState(), 3);
        level.getBlockState(submerged)
                .useItemOn(creativeLava, level, creative, InteractionHand.MAIN_HAND, hitAt(level, submerged));
        helper.assertBlock(new BlockPos(6, 1, 6), b -> b == Blocks.OBSIDIAN, "obsidian expected for creative too");
        helper.assertTrue(creative.getMainHandItem().is(Items.LAVA_BUCKET),
                "creative lava bucket must be preserved");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void underwaterCauldronVanillaWhenOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spot = new BlockPos(4, 1, 4);
        helper.setBlock(spot, Blocks.WATER);
        Player placer = helper.makeMockPlayer(GameType.SURVIVAL);
        ((BlockItem) Items.CAULDRON).place(new BlockPlaceContext(
                level, placer, InteractionHand.MAIN_HAND, new ItemStack(Items.CAULDRON), hitAt(level, helper.absolutePos(spot))));
        helper.assertBlockPresent(Blocks.CAULDRON, spot);

        // Submerged cauldron still fills with powder snow like vanilla.
        BlockPos submerged = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlock(submerged, Blocks.CAULDRON.defaultBlockState(), 3);
        level.setBlock(submerged.above(), Blocks.WATER.defaultBlockState(), 3);
        level.getBlockState(submerged).useItemOn(new ItemStack(Items.POWDER_SNOW_BUCKET),
                level, placer, InteractionHand.MAIN_HAND, hitAt(level, submerged));
        helper.assertBlock(new BlockPos(6, 1, 6), b -> b == Blocks.POWDER_SNOW_CAULDRON,
                "vanilla must fill a submerged cauldron with powder snow");
        helper.succeed();
    }

    // -- D: nether_cauldron_evaporation --

    private static final BlockState FULL_WATER_CAULDRON =
            Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);

    @GameTest(template = EMPTY, batch = ON)
    public void netherCauldronEvaporatesWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether.dimensionType().ultraWarm(), "nether must be ultrawarm");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(500, 80, 500);
        nether.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        ItemStack water = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        CauldronInteraction.emptyBucket(nether, pos, player, InteractionHand.MAIN_HAND, water,
                FULL_WATER_CAULDRON, SoundEvents.BUCKET_EMPTY);
        helper.assertValueEqual(nether.getBlockState(pos).getBlock(), Blocks.CAULDRON,
                "nether cauldron must be left empty after evaporation");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET),
                "the water bucket must be consumed to an empty bucket");

        // Overworld control: identical call fills normally.
        BlockPos ow = helper.absolutePos(new BlockPos(4, 1, 4));
        level.setBlock(ow, Blocks.CAULDRON.defaultBlockState(), 3);
        ItemStack water2 = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, water2);
        CauldronInteraction.emptyBucket(level, ow, player, InteractionHand.MAIN_HAND, water2,
                FULL_WATER_CAULDRON, SoundEvents.BUCKET_EMPTY);
        helper.assertBlock(new BlockPos(4, 1, 4), b -> b == Blocks.WATER_CAULDRON,
                "overworld cauldron must still fill with water");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void netherCauldronFillsWhenOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = new BlockPos(500, 80, 500);
        nether.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
        ItemStack water = new ItemStack(Items.WATER_BUCKET);
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        CauldronInteraction.emptyBucket(nether, pos, player, InteractionHand.MAIN_HAND, water,
                FULL_WATER_CAULDRON, SoundEvents.BUCKET_EMPTY);
        helper.assertValueEqual(nether.getBlockState(pos).getBlock(), Blocks.WATER_CAULDRON,
                "with the tweak off a nether cauldron fills like vanilla");
        nether.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
        helper.succeed();
    }

    // -- E: cushioned_anvils --

    private void dropDamagedAnvil(GameTestHelper helper, BlockPos support, Block supportBlock) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(support, supportBlock);
        BlockPos anvilPos = helper.absolutePos(support.above(4));
        level.setBlock(anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState(), 3);
        FallingBlockEntity falling = FallingBlockEntity.fall(level, anvilPos, Blocks.DAMAGED_ANVIL.defaultBlockState());
        falling.setHurtsEntities(2.0F, 40); // Fallable#falling does this for real block ticks
        falling.fallDistance = 30.0F; // deterministic >100% damage chance
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void anvilCushionedBySlimeAndHoney(GameTestHelper helper) {
        dropDamagedAnvil(helper, new BlockPos(2, 1, 2), Blocks.SLIME_BLOCK);
        dropDamagedAnvil(helper, new BlockPos(6, 1, 2), Blocks.HONEY_BLOCK);
        dropDamagedAnvil(helper, new BlockPos(4, 1, 6), Blocks.STONE);
        helper.runAfterDelay(30, () -> {
            helper.assertBlockPresent(Blocks.DAMAGED_ANVIL, new BlockPos(2, 2, 2));
            // Entities sink into honey's non-full shape, so the cushioned
            // anvil cannot be placed there and drops intact as an item.
            net.minecraft.world.entity.item.ItemEntity honeyDrop =
                    (net.minecraft.world.entity.item.ItemEntity) helper.findOneEntity(EntityType.ITEM);
            helper.assertTrue(honeyDrop.getItem().is(Items.DAMAGED_ANVIL),
                    "honey-cushioned anvil must survive as a dropped item, got " + honeyDrop.getItem());
            helper.assertBlock(new BlockPos(4, 2, 6), b -> !b.defaultBlockState().is(net.minecraft.tags.BlockTags.ANVIL),
                    "the anvil on stone must be destroyed by the huge fall");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void anvilBreaksOnSlimeWhenOff(GameTestHelper helper) {
        dropDamagedAnvil(helper, new BlockPos(4, 1, 4), Blocks.SLIME_BLOCK);
        helper.runAfterDelay(30, () -> {
            helper.assertBlock(new BlockPos(4, 2, 4), b -> !b.defaultBlockState().is(net.minecraft.tags.BlockTags.ANVIL),
                    "with the tweak off a damaged anvil must still be destroyed on slime");
            helper.succeed();
        });
    }

    // -- F: burning_snowballs --

    private TestAccess.TestSnowball burningSnowball(GameTestHelper helper, Vec3 pos) {
        TestAccess.TestSnowball snowball = new TestAccess.TestSnowball(helper.getLevel());
        snowball.setPos(pos);
        helper.getLevel().addFreshEntity(snowball);
        snowball.igniteForSeconds(10.0F);
        snowball.setSharedFlagOnFire(true);
        return snowball;
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void burningSnowballIgnitesTarget(GameTestHelper helper) {
        Pig victim = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        // An initially unlit snowball left in lava: the reported scenario.
        helper.setBlock(new BlockPos(4, 0, 6), Blocks.LAVA);
        helper.setBlock(new BlockPos(4, 1, 6), Blocks.LAVA);
        TestAccess.TestSnowball snowball = new TestAccess.TestSnowball(helper.getLevel());
        snowball.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 6))));
        helper.getLevel().addFreshEntity(snowball);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(snowball.isOnFire(), "a snowball resting in lava must catch fire");
            snowball.hitEntity(victim);
            helper.assertTrue(victim.isOnFire() || victim.getRemainingFireTicks() > 0,
                    "a burning snowball must ignite its victim");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void prelitSnowballIgnitesTarget(GameTestHelper helper) {
        Pig victim = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        TestAccess.TestSnowball snowball =
                burningSnowball(helper, Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 6))));
        snowball.hitEntity(victim);
        helper.assertTrue(victim.getRemainingFireTicks() > 0,
                "an already-burning snowball must ignite its victim");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void normalSnowballDoesNotIgnite(GameTestHelper helper) {
        Pig victim = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        TestAccess.TestSnowball snowball = new TestAccess.TestSnowball(helper.getLevel());
        snowball.hitEntity(victim);
        helper.assertFalse(victim.isOnFire(), "a normal snowball must not ignite its victim");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void burningSnowballColdWhenOff(GameTestHelper helper) {
        Pig victim = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 4));
        TestAccess.TestSnowball snowball = burningSnowball(helper, Vec3.atCenterOf(helper.absolutePos(new BlockPos(4, 1, 6))));
        snowball.hitEntity(victim);
        helper.assertFalse(victim.isOnFire(), "with the tweak off even a burning snowball must not ignite");
        helper.succeed();
    }

    // -- G: arrows_trigger_falling_blocks --

    private void shootSand(GameTestHelper helper, BlockPos sandPos) {
        ServerLevel level = helper.getLevel();
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        BlockPos abs = helper.absolutePos(sandPos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        level.getBlockState(abs).onProjectileHit(level, level.getBlockState(abs), hit, arrow);
    }

    private void floatSand(GameTestHelper helper, BlockPos sand) {
        // FallingBlock.onPlace schedules its own tick, so floating sand is made
        // by letting a supported block consume that tick, then silently
        // removing the support with no neighbour update.
        helper.setBlock(sand.below(), Blocks.STONE);
        helper.setBlock(sand, Blocks.SAND);
        helper.runAfterDelay(5, () ->
                helper.getLevel().setBlock(helper.absolutePos(sand.below()), Blocks.AIR.defaultBlockState(), 2));
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void arrowDropsUnsupportedSand(GameTestHelper helper) {
        floatSand(helper, new BlockPos(4, 2, 4));
        helper.runAfterDelay(10, () -> {
            shootSand(helper, new BlockPos(4, 2, 4));
            // Supported control: sand directly on stone must stay put.
            helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
            helper.setBlock(new BlockPos(6, 2, 6), Blocks.SAND);
            shootSand(helper, new BlockPos(6, 2, 6));
            helper.runAfterDelay(40, () -> {
                helper.assertBlockPresent(Blocks.AIR, new BlockPos(4, 2, 4));
                helper.assertBlockPresent(Blocks.SAND, new BlockPos(6, 2, 6));
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void arrowLeavesUnsupportedSandWhenOff(GameTestHelper helper) {
        // Sand's own onPlace schedules a tick; wait it out while supported,
        // then an arrow must not schedule a new one when the tweak is off.
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.SAND);
        helper.runAfterDelay(10, () -> {
            shootSand(helper, new BlockPos(4, 2, 4));
            BlockPos abs = helper.absolutePos(new BlockPos(4, 2, 4));
            helper.assertFalse(level.getBlockTicks().hasScheduledTick(abs, Blocks.SAND),
                    "with the tweak off the arrow must not schedule a falling-block tick");
            helper.assertBlockPresent(Blocks.SAND, new BlockPos(4, 2, 4));
            helper.succeed();
        });
    }

    // -- H: restrict_seagrass_substrates --

    private boolean seagrassSurvives(GameTestHelper helper, BlockPos rel, Block substrate) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(rel);
        helper.setBlock(rel.below(), substrate);
        helper.setBlock(rel, Blocks.WATER);
        return Blocks.SEAGRASS.defaultBlockState().canSurvive(level, pos);
    }

    @GameTest(template = EMPTY, batch = ON)
    public void seagrassSubstrateRestricted(GameTestHelper helper) {
        helper.assertFalse(seagrassSurvives(helper, new BlockPos(2, 2, 2), Blocks.STONE),
                "seagrass must not survive on stone with the tweak on");
        helper.assertFalse(seagrassSurvives(helper, new BlockPos(4, 2, 2), Blocks.EMERALD_BLOCK),
                "seagrass must not survive on emerald with the tweak on");
        helper.assertTrue(seagrassSurvives(helper, new BlockPos(2, 2, 4), Blocks.DIRT),
                "seagrass must still survive on dirt");
        helper.assertTrue(seagrassSurvives(helper, new BlockPos(4, 2, 4), Blocks.SAND),
                "seagrass must still survive on sand");

        // Tall seagrass uses the same substrate rule.
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(6, 2, 6);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, Blocks.WATER);
        helper.setBlock(rel.above(), Blocks.WATER);
        helper.assertFalse(Blocks.TALL_SEAGRASS.defaultBlockState().canSurvive(level, helper.absolutePos(rel)),
                "tall seagrass must not survive on stone with the tweak on");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void seagrassSturdySubstrateAllowedWhenOff(GameTestHelper helper) {
        helper.assertTrue(seagrassSurvives(helper, new BlockPos(4, 2, 4), Blocks.STONE),
                "with the tweak off seagrass survives on sturdy stone");
        helper.succeed();
    }

    // -- I: prevent_unsupported_plant_growth --

    @GameTest(template = EMPTY, batch = ON)
    public void unsupportedCactusDoesNotGrow(GameTestHelper helper) {
        // Floating cactus: remove the sand support without a neighbour update.
        BlockPos base = new BlockPos(2, 1, 2);
        helper.setBlock(base.below(), Blocks.SAND);
        helper.setBlock(base, Blocks.CACTUS);
        helper.setBlock(base.above(), Blocks.CACTUS);
        helper.getLevel().setBlock(helper.absolutePos(base.below()), Blocks.AIR.defaultBlockState(), 2);
        helper.setBlock(base.above(), Blocks.CACTUS.defaultBlockState().setValue(
                net.minecraft.world.level.block.CactusBlock.AGE, 15));
        helper.randomTick(base.above());
        helper.assertBlockNotPresent(Blocks.CACTUS, base.above(2));

        // Control: supported cactus still grows.
        BlockPos good = new BlockPos(6, 1, 6);
        helper.setBlock(good.below(), Blocks.SAND);
        helper.setBlock(good, Blocks.CACTUS.defaultBlockState().setValue(
                net.minecraft.world.level.block.CactusBlock.AGE, 15));
        helper.randomTick(good);
        helper.assertBlockPresent(Blocks.CACTUS, good.above());
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void unsupportedSugarCaneDoesNotGrow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Supported cane: sand with adjacent water, cane column, then destroy
        // the water so the base is invalid while the column survives.
        BlockPos base = new BlockPos(4, 1, 4);
        helper.setBlock(base.below(), Blocks.SAND);
        helper.setBlock(base.below().north(), Blocks.WATER);
        helper.setBlock(base, Blocks.SUGAR_CANE);
        helper.setBlock(base.above(), Blocks.SUGAR_CANE.defaultBlockState().setValue(
                net.minecraft.world.level.block.SugarCaneBlock.AGE, 15));
        level.setBlock(helper.absolutePos(base.below().north()), Blocks.AIR.defaultBlockState(), 2);
        helper.randomTick(base.above());
        helper.assertBlockNotPresent(Blocks.SUGAR_CANE, base.above(2));
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void unsupportedCactusGrowsWhenOff(GameTestHelper helper) {
        BlockPos base = new BlockPos(4, 1, 4);
        helper.setBlock(base.below(), Blocks.SAND);
        helper.setBlock(base, Blocks.CACTUS.defaultBlockState().setValue(
                net.minecraft.world.level.block.CactusBlock.AGE, 15));
        helper.getLevel().setBlock(helper.absolutePos(base.below()), Blocks.AIR.defaultBlockState(), 2);
        helper.randomTick(base);
        helper.assertBlockPresent(Blocks.CACTUS, base.above());
        helper.succeed();
    }

    // -- J: end_portals_require_frames --

    private void buildPortal(GameTestHelper helper, BlockPos center) {
        ServerLevel level = helper.getLevel();
        BlockPos absCenter = helper.absolutePos(center);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int i = -1; i <= 1; i++) {
                BlockPos frame = absCenter.relative(facing.getOpposite(), 2).relative(facing.getClockWise(), i);
                level.setBlock(frame, Blocks.END_PORTAL_FRAME.defaultBlockState()
                        .setValue(EndPortalFrameBlock.FACING, facing)
                        .setValue(EndPortalFrameBlock.HAS_EYE, true), 3);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(absCenter.offset(dx, 0, dz), Blocks.END_PORTAL.defaultBlockState(), 3);
            }
        }
    }

    @GameTest(template = EMPTY, batch = ON)
    public void breakingAnyFrameExtinguishesPortal(GameTestHelper helper) {
        BlockPos center = new BlockPos(4, 1, 4);
        // All 12 non-corner frame positions must extinguish the portal.
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int i = -1; i <= 1; i++) {
                BlockPos frame = center.relative(facing.getOpposite(), 2).relative(facing.getClockWise(), i);
                buildPortal(helper, center);
                helper.getLevel().setBlock(helper.absolutePos(frame), Blocks.AIR.defaultBlockState(), 3);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        helper.assertBlockNotPresent(Blocks.END_PORTAL, center.offset(dx, 0, dz));
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void cornerFillerAndNearbyPortalSurvive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = new BlockPos(4, 1, 4);
        // Corner filler is not a frame: removing it must not extinguish.
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.DIRT);
        buildPortal(helper, center);
        level.setBlock(helper.absolutePos(new BlockPos(2, 1, 2)), Blocks.AIR.defaultBlockState(), 3);
        helper.assertBlockPresent(Blocks.END_PORTAL, center);
        // A second complete portal one level up stays when the lower ring breaks.
        BlockPos upper = new BlockPos(4, 5, 4);
        buildPortal(helper, upper);
        level.setBlock(helper.absolutePos(center.relative(Direction.NORTH, 2)), Blocks.AIR.defaultBlockState(), 3);
        helper.assertBlockPresent(Blocks.END_PORTAL, upper);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void incompleteRingKeepsPortal(GameTestHelper helper) {
        BlockPos center = new BlockPos(4, 1, 4);
        buildPortal(helper, center);
        // Remove the eye from one frame so the ring is incomplete, then break
        // a different frame: nothing may happen to the portal blocks.
        BlockPos absEyeless = helper.absolutePos(center.relative(Direction.NORTH, 2));
        ServerLevel level = helper.getLevel();
        level.setBlock(absEyeless, level.getBlockState(absEyeless)
                .setValue(EndPortalFrameBlock.HAS_EYE, false), 3);
        level.setBlock(helper.absolutePos(center.relative(Direction.EAST, 2)), Blocks.AIR.defaultBlockState(), 3);
        helper.assertBlockPresent(Blocks.END_PORTAL, center);
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void breakingFrameKeepsPortalWhenOff(GameTestHelper helper) {
        BlockPos center = new BlockPos(4, 1, 4);
        buildPortal(helper, center);
        helper.getLevel().setBlock(helper.absolutePos(center.relative(Direction.NORTH, 2)),
                Blocks.AIR.defaultBlockState(), 3);
        helper.assertBlockPresent(Blocks.END_PORTAL, center);
        helper.succeed();
    }

    // -- K: lava_converts_waterlogged_coral --

    private void waterloggedCoralUnderLava(GameTestHelper helper, BlockPos coral) {
        helper.setBlock(coral, Blocks.FIRE_CORAL_FAN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true));
        helper.setBlock(coral.above(), Blocks.LAVA);
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 300)
    public void lavaHardensWaterloggedCoral(GameTestHelper helper) {
        waterloggedCoralUnderLava(helper, new BlockPos(4, 1, 4));
        // A waterlogged non-coral block must be untouched.
        helper.setBlock(new BlockPos(6, 1, 6), Blocks.STONE_SLAB.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true));
        helper.setBlock(new BlockPos(6, 2, 6), Blocks.LAVA);
        helper.runAfterDelay(80, () -> {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(4, 1, 4));
            helper.assertBlockPresent(Blocks.STONE_SLAB, new BlockPos(6, 1, 6));
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void lavaLeavesWaterloggedCoralWhenOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        waterloggedCoralUnderLava(helper, new BlockPos(4, 1, 4));
        // Drive the fluid tick synchronously instead of waiting on the queue.
        BlockPos lava = helper.absolutePos(new BlockPos(4, 2, 4));
        for (int i = 0; i < 10; i++) {
            level.getFluidState(lava).tick(level, lava);
        }
        helper.assertBlockPresent(Blocks.FIRE_CORAL_FAN, new BlockPos(4, 1, 4));
        helper.succeed();
    }

    // -- L: villager_beds_explode_in_invalid_dimensions --

    private static void placeBed(ServerLevel level, BlockPos foot, Direction facing) {
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, facing);
        level.setBlock(foot, bed, 3);
        level.setBlock(foot.relative(facing), bed.setValue(net.minecraft.world.level.block.BedBlock.PART,
                net.minecraft.world.level.block.state.properties.BedPart.HEAD), 3);
    }

    private static net.minecraft.world.entity.npc.Villager villager(ServerLevel level, BlockPos near) {
        net.minecraft.world.entity.npc.Villager villager = EntityType.VILLAGER.create(level);
        villager.setPos(Vec3.atCenterOf(near));
        level.addFreshEntity(villager);
        return villager;
    }

    @GameTest(template = EMPTY, batch = ON)
    public void villagerBedExplodesInInvalidDimensions(GameTestHelper helper) {
        ServerLevel server = helper.getLevel();
        for (net.minecraft.resources.ResourceKey<Level> dim : new net.minecraft.resources.ResourceKey[]{Level.NETHER, Level.END}) {
            ServerLevel level = server.getServer().getLevel(dim);
            BlockPos foot = new BlockPos(500, 80, dim == Level.END ? 600 : 520);
            placeBed(level, foot, Direction.NORTH);
            Vec3[] explosionCenter = new Vec3[1];
            java.util.function.Consumer<net.neoforged.neoforge.event.level.ExplosionEvent.Start> listener =
                    event -> explosionCenter[0] = event.getExplosion().center();
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    net.neoforged.neoforge.event.level.ExplosionEvent.Start.class, listener);
            net.minecraft.world.entity.npc.Villager villager = villager(level, foot.south(2));
            try {
                villager.startSleeping(foot);
            } finally {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
            }
            helper.assertValueEqual(explosionCenter[0], foot.north().getCenter(),
                    "explosion must be centred on the bed head block in " + dim.location());
            helper.assertValueEqual(level.getBlockState(foot).getBlock(), Blocks.AIR,
                    "bed foot must be removed in " + dim.location());
            helper.assertValueEqual(level.getBlockState(foot.north()).getBlock(), Blocks.AIR,
                    "bed head must be removed in " + dim.location());
            helper.assertFalse(villager.isSleeping(), "sleep must be cancelled in " + dim.location());
            level.setBlock(foot.south(2), Blocks.BEDROCK.defaultBlockState(), 3);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void villagerBedSafeInOverworldAndForNonVillagers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 1, 4));
        placeBed(level, foot, Direction.NORTH);
        net.minecraft.world.entity.npc.Villager villager = villager(level, foot.south(2));
        villager.startSleeping(foot);
        helper.assertTrue(villager.isSleeping(), "overworld villager must sleep normally");
        helper.assertBlock(new BlockPos(4, 1, 4), b -> b == Blocks.RED_BED, "overworld bed must stay");

        // Non-villager entities calling startSleeping are untouched.
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));
        BlockPos pigBed = helper.absolutePos(new BlockPos(6, 1, 6).north(2));
        placeBed(level, pigBed, Direction.NORTH);
        pig.startSleeping(pigBed);
        helper.assertBlock(new BlockPos(6, 1, 4), b -> b == Blocks.RED_BED, "non-villager bed must stay");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void villagerBedInNetherWhenOff(GameTestHelper helper) {
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        BlockPos foot = new BlockPos(500, 80, 560);
        placeBed(nether, foot, Direction.NORTH);
        net.minecraft.world.entity.npc.Villager villager = villager(nether, foot.south(2));
        villager.startSleeping(foot);
        helper.assertTrue(villager.isSleeping(), "with the tweak off a nether villager sleeps normally");
        helper.assertValueEqual(nether.getBlockState(foot).getBlock(), Blocks.RED_BED, "bed must stay when off");
        helper.succeed();
    }
}
