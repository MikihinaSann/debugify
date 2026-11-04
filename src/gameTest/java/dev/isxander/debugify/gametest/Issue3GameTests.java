package dev.isxander.debugify.gametest;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.DebugifyConfig;
import dev.isxander.debugify.config.Issue2Tweak;
import dev.isxander.debugify.config.Issue3Tweak;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.fixes.FixCategory;
import dev.isxander.debugify.fixes.OS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@GameTestHolder(Debugify.MODID)
@PrefixGameTestTemplate(false)
public class Issue3GameTests {
    private static final String EMPTY = "empty";
    private static final String ON = "issue3On";
    private static final String OFF = "issue3Off";

    private static final Field GOAL_STRAFING_TIME = findField("strafingTime");
    private static final Field GOAL_SEE_TIME = findField("seeTime");
    private static final Field MOVE_STRAFE_FORWARD = findMoveField("strafeForwards");
    private static final Field MOVE_STRAFE_RIGHT = findMoveField("strafeRight");

    private static Field findField(String name) {
        try {
            Field field = RangedBowAttackGoal.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field findMoveField(String name) {
        try {
            Field field = net.minecraft.world.entity.ai.control.MoveControl.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setInt(Field field, Object target, int value) {
        try {
            field.setInt(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static float getFloat(Field field, Object target) {
        try {
            return field.getFloat(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeBatch(batch = ON)
    public static void enableAll(ServerLevel level) {
        for (Issue3Tweak tweak : Issue3Tweak.values()) {
            Debugify.CONFIG.setIssue3TweakEnabled(tweak, true);
        }
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = ON)
    public static void resetAfterOn(ServerLevel level) {
        offAll();
    }

    @BeforeBatch(batch = OFF)
    public static void disableAll(ServerLevel level) {
        offAll();
    }

    private static void offAll() {
        for (Issue3Tweak tweak : Issue3Tweak.values()) {
            Debugify.CONFIG.setIssue3TweakEnabled(tweak, false);
        }
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void issue3ConfigDefaultsOff(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload();
            for (Issue3Tweak tweak : Issue3Tweak.values()) {
                helper.assertFalse(config.isIssue3TweakEnabled(tweak), tweak.key() + " must default to off");
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void issue3ConfigMalformedStaysOff(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            Files.writeString(file, """
                    {
                      "issue_3_tweaks": {
                        "dropped_items_ring_bells": "true",
                        "tame_skeleton_horses_on_interaction": 1,
                        "visible_villager_armor": true,
                        "bogus_key": true
                      }
                    }
                    """);
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload();
            helper.assertTrue(config.isIssue3TweakEnabled(Issue3Tweak.VISIBLE_VILLAGER_ARMOR),
                    "explicit boolean true must enable the tweak");
            helper.assertFalse(config.isIssue3TweakEnabled(Issue3Tweak.DROPPED_ITEMS_RING_BELLS),
                    "string \"true\" must not enable the tweak");
            helper.assertFalse(config.isIssue3TweakEnabled(Issue3Tweak.TAME_SKELETON_HORSES_ON_INTERACTION),
                    "numeric 1 must not enable the tweak");

            Path file2 = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            Files.writeString(file2, "{ \"issue_3_tweaks\": true }");
            DebugifyConfig config2 = new DebugifyConfig(file2);
            config2.preload();
            for (Issue3Tweak tweak : Issue3Tweak.values()) {
                helper.assertFalse(config2.isIssue3TweakEnabled(tweak), "non-object tweaks value must stay off");
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void issue3ConfigRoundtrip(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload();
            BugFixData fix = new BugFixData("MC-261666", FixCategory.BASIC, BugFix.Env.SERVER, true, List.of(), OS.UNKNOWN);
            config.registerBugFix(fix);
            config.setIssue2TweakEnabled(Issue2Tweak.BURNING_SNOWBALLS, true);
            config.setIssue3TweakEnabled(Issue3Tweak.DROPPED_ITEMS_RING_BELLS, true);
            config.setIssue3TweakEnabled(Issue3Tweak.VISIBLE_VILLAGER_ARMOR, true);
            config.save();

            String json = Files.readString(file);
            helper.assertTrue(json.contains("\"MC-261666\": true"), "saved file must keep MC-* settings: " + json);
            helper.assertTrue(json.contains("\"issue_3_tweaks\""), "saved file must contain the issue3 tweaks object");
            for (Issue3Tweak tweak : Issue3Tweak.values()) {
                helper.assertTrue(json.contains(tweak.key()), "saved file must contain key " + tweak.key());
            }

            DebugifyConfig reloaded = new DebugifyConfig(file);
            reloaded.preload();
            helper.assertTrue(reloaded.isIssue3TweakEnabled(Issue3Tweak.DROPPED_ITEMS_RING_BELLS),
                    "enabled tweak must survive the roundtrip");
            helper.assertTrue(reloaded.isIssue3TweakEnabled(Issue3Tweak.VISIBLE_VILLAGER_ARMOR),
                    "enabled tweak must survive the roundtrip");
            helper.assertFalse(reloaded.isIssue3TweakEnabled(Issue3Tweak.TAME_SKELETON_HORSES_ON_INTERACTION),
                    "disabled tweak must survive the roundtrip");
            helper.assertTrue(reloaded.isIssue2TweakEnabled(Issue2Tweak.BURNING_SNOWBALLS),
                    "issue2 tweak must survive untouched");
            BugFixData fix2 = new BugFixData("MC-261666", FixCategory.BASIC, BugFix.Env.SERVER, true, List.of(), OS.UNKNOWN);
            reloaded.registerBugFix(fix2);
            helper.assertTrue(reloaded.isBugFixEnabled(fix2), "MC flag must survive the roundtrip");
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void issue3UnaffectedByDefaultDisabled(GameTestHelper helper) {
        try {
            Path file = Files.createTempDirectory("debugify-test").resolve("debugify.json");
            Files.writeString(file, """
                    {
                      "default_disabled": true,
                      "issue_3_tweaks": { "dropped_items_ring_bells": true }
                    }
                    """);
            DebugifyConfig config = new DebugifyConfig(file);
            config.preload();
            helper.assertTrue(config.isIssue3TweakEnabled(Issue3Tweak.DROPPED_ITEMS_RING_BELLS),
                    "explicit true must win over default_disabled");
            helper.assertFalse(config.isIssue3TweakEnabled(Issue3Tweak.TAME_SKELETON_HORSES_ON_INTERACTION),
                    "absent key stays off under default_disabled");
            helper.assertFalse(config.isIssue3TweakEnabled(Issue3Tweak.VISIBLE_VILLAGER_ARMOR),
                    "absent key stays off under default_disabled");
            helper.succeed();
        } catch (IOException e) {
            helper.fail("temp config io failed: " + e);
        }
    }

    private BellBlockEntity floorBell(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, Blocks.BELL);
        return helper.getBlockEntity(rel);
    }

    private void snowballHit(GameTestHelper helper, BlockPos bellRel, Direction face, double hitY) {
        BlockPos bellAbs = helper.absolutePos(bellRel);
        Vec3 hitLoc = new Vec3(bellAbs.getX() + 0.5, bellAbs.getY() + hitY, bellAbs.getZ() + 0.5);
        TestAccess.TestSnowball snowball = new TestAccess.TestSnowball(helper.getLevel());
        snowball.setPos(hitLoc);
        snowball.hit(new BlockHitResult(hitLoc, face, bellAbs, false));
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void snowballRingsBellOnProperHit(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        snowballHit(helper, bell, Direction.SOUTH, 0.5);
        helper.assertTrue(bellEntity.shaking,
                "vanilla: a snowball on the bell's valid horizontal face must ring it");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void snowballDoesNotRingOnWrongAxisOrTop(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        snowballHit(helper, bell, Direction.EAST, 0.5);
        helper.assertFalse(bellEntity.shaking, "a hit on the wrong axis must not ring");
        snowballHit(helper, bell, Direction.UP, 0.99);
        helper.assertFalse(bellEntity.shaking, "a top hit must not ring");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void droppedItemRingsBellOnContact(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        ItemEntity item = helper.spawnItem(Items.DIRT, new Vec3(4.5, 3.4, 4.5));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(bellEntity.shaking, "a dropped item touching the bell must ring it");
            helper.assertValueEqual(item.getItem().getCount(), 1, "item count must be unchanged by the ring");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void restingDroppedItemDoesNotRetrigger(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        helper.spawnItem(Items.DIRT, new Vec3(4.5, 3.4, 4.5));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(bellEntity.shaking, "first contact should have rung the bell");
            bellEntity.shaking = false;
            bellEntity.ticks = 0;
            helper.runAfterDelay(30, () -> {
                helper.assertFalse(bellEntity.shaking,
                        "an item resting on the bell must not re-ring it after the cooldown");
                helper.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void removedItemTouchingBellDoesNotRing(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        Vec3 inside = helper.absoluteVec(new Vec3(4.5, 2.5, 4.5));
        ItemEntity item = new ItemEntity(helper.getLevel(), inside.x, inside.y, inside.z, new ItemStack(Items.DIRT));
        item.discard();
        item.tick();
        helper.assertFalse(bellEntity.shaking, "a removed item entity must not ring the bell");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON, timeoutTicks = 200)
    public void itemNearBellDoesNotRing(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        helper.setBlock(new BlockPos(4, 1, 6), Blocks.STONE);
        helper.spawnItem(Items.DIRT, new Vec3(4.5, 2.4, 6.5));
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(bellEntity.shaking, "an item merely near the bell must not ring it");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void droppedItemDoesNotRingWhenOff(GameTestHelper helper) {
        BlockPos bell = new BlockPos(4, 2, 4);
        BellBlockEntity bellEntity = floorBell(helper, bell);
        helper.spawnItem(Items.DIRT, new Vec3(4.5, 3.4, 4.5));
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(bellEntity.shaking, "with the tweak off a dropped item must not ring the bell");
            helper.succeed();
        });
    }

    private Player emptyHanded(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return player;
    }

    @GameTest(template = EMPTY, batch = ON)
    public void skeletonHorseEmptyHandTamesAndMounts(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        Player player = emptyHanded(helper);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(horse.isTamed(), "an empty-hand interaction must tame the adult skeleton horse");
        helper.assertValueEqual(player.getVehicle(), horse, "taming interaction must mount the player");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void skeletonHorseInteractOffStaysUntamed(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        Player player = emptyHanded(helper);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(horse.isTamed(), "with the tweak off an untamed skeleton horse must ignore the interact");
        helper.assertTrue(player.getVehicle() == null, "untamed skeleton horse must not mount the player");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void skeletonHorseBabyNotTamed(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        horse.setBaby(true);
        Player player = emptyHanded(helper);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(horse.isTamed(), "a baby skeleton horse must not be tamed by the tweak");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void skeletonHorseSecondaryUseNotTamed(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        Player player = emptyHanded(helper);
        player.setShiftKeyDown(true);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(horse.isTamed(), "a sneak interaction must not trigger the tame");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void skeletonHorseHeldItemNotTamed(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(horse.isTamed(), "a held-item interaction must not trigger the tame");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void skeletonHorseAlreadyTamedStillMounts(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        horse.setTamed(true);
        Player player = emptyHanded(helper);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(player.getVehicle(), horse,
                "an already-tamed skeleton horse must mount through the vanilla path");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void skeletonHorseSaddleRequiredForControl(GameTestHelper helper) {
        SkeletonHorse horse = helper.spawnWithNoFreeWill(EntityType.SKELETON_HORSE, new BlockPos(4, 1, 4));
        Player player = emptyHanded(helper);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(player.getVehicle(), horse, "taming interaction must mount the player");
        helper.assertTrue(horse.getControllingPassenger() == null,
                "an unsaddled skeleton horse must not be steerable");
        horse.equipSaddle(new ItemStack(Items.SADDLE), null);
        helper.assertValueEqual(horse.getControllingPassenger(), player,
                "a saddled skeleton horse must hand control back to the rider");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = ON)
    public void regularHorseInteractUntouched(GameTestHelper helper) {
        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, new BlockPos(4, 1, 4));
        Player player = emptyHanded(helper);
        horse.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertFalse(horse.isTamed(), "regular horses must not be tamed by the skeleton horse tweak");
        helper.succeed();
    }

    private void makeHard(ServerLevel level, Difficulty previous) {
        if (previous != Difficulty.HARD) {
            level.getServer().setDifficulty(Difficulty.HARD, true);
        }
    }

    private ItemStack namedGear(String name, net.minecraft.world.item.Item item, int damage) {
        ItemStack stack = new ItemStack(item);
        if (damage > 0) {
            stack.setDamageValue(damage);
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private void equipVillager(Villager villager, ItemStack helmet, ItemStack chest, ItemStack hand) {
        villager.setItemSlot(EquipmentSlot.HEAD, helmet);
        villager.setItemSlot(EquipmentSlot.CHEST, chest);
        villager.setItemSlot(EquipmentSlot.MAINHAND, hand);
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.TOOLSMITH));
        villager.setVillagerXp(77);
    }

    private List<ItemEntity> droppedItems(GameTestHelper helper, BlockPos rel) {
        return helper.findEntities(EntityType.ITEM, rel.getX(), rel.getY(), rel.getZ(), 5.0)
                .stream()
                .filter(item -> !item.getItem().isEmpty())
                .toList();
    }

    private int countMatchingDrops(List<ItemEntity> drops, ItemStack expected) {
        int count = 0;
        for (ItemEntity drop : drops) {
            if (ItemStack.isSameItemSameComponents(expected, drop.getItem())) {
                count += drop.getItem().getCount() == expected.getCount() ? 1 : 0;
            }
        }
        return count;
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void villagerConversionDropsEquipmentOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Difficulty previous = level.getDifficulty();
        try {
            makeHard(level, previous);
            BlockPos spot = new BlockPos(4, 1, 4);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
            Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, spot);
            ItemStack helmet = namedGear("keeper-hat", Items.DIAMOND_HELMET, 11);
            ItemStack chest = namedGear("keeper-coat", Items.LEATHER_CHESTPLATE, 7);
            ItemStack hand = new ItemStack(Items.STICK, 7);
            ItemStack expectedHelmet = helmet.copy();
            ItemStack expectedChest = chest.copy();
            ItemStack expectedHand = hand.copy();
            equipVillager(villager, helmet, chest, hand);

            zombie.killedEntity(level, villager);

            List<ItemEntity> drops = droppedItems(helper, spot);
            helper.assertValueEqual(countMatchingDrops(drops, expectedHelmet), 1,
                    "the villager's helmet must drop exactly once with its components");
            helper.assertValueEqual(countMatchingDrops(drops, expectedChest), 1,
                    "the villager's chestplate must drop exactly once with its components");
            helper.assertValueEqual(countMatchingDrops(drops, expectedHand), 1,
                    "the villager's held stack must drop exactly once with its count");
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                helper.assertTrue(villager.getItemBySlot(slot).isEmpty(),
                        "the discarded villager must not retain its " + slot + " stack");
            }

            ZombieVillager converted = helper.findOneEntity(EntityType.ZOMBIE_VILLAGER);
            helper.assertValueEqual(converted.getVillagerData().getProfession(), VillagerProfession.TOOLSMITH,
                    "the zombie villager must keep the villager's profession");
            helper.assertValueEqual(converted.getVillagerXp(), 77,
                    "the zombie villager must keep the villager's xp");
            helper.assertFalse(ItemStack.isSameItemSameComponents(expectedHelmet, converted.getItemBySlot(EquipmentSlot.HEAD)),
                    "the dropped helmet must not be silently moved onto the zombie villager");
        } finally {
            level.getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void villagerConversionRepeatCallDoesNotDuplicate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Difficulty previous = level.getDifficulty();
        try {
            makeHard(level, previous);
            BlockPos spot = new BlockPos(4, 1, 4);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
            Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, spot);
            ItemStack helmet = namedGear("keeper-hat", Items.DIAMOND_HELMET, 11);
            ItemStack expectedHelmet = helmet.copy();
            equipVillager(villager, helmet, ItemStack.EMPTY, ItemStack.EMPTY);

            zombie.killedEntity(level, villager);
            helper.assertValueEqual(countMatchingDrops(droppedItems(helper, spot), expectedHelmet), 1,
                    "first conversion must drop the helmet once");

            zombie.killedEntity(level, villager);
            helper.assertValueEqual(countMatchingDrops(droppedItems(helper, spot), expectedHelmet), 1,
                    "a repeated conversion call on the removed villager must not duplicate the gear");
        } finally {
            level.getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void conversionVetoKeepsEquipmentAndSkipsDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Difficulty previous = level.getDifficulty();
        Consumer<LivingConversionEvent.Pre> listener = event -> event.setCanceled(true);
        NeoForge.EVENT_BUS.addListener(LivingConversionEvent.Pre.class, listener);
        try {
            makeHard(level, previous);
            BlockPos spot = new BlockPos(4, 1, 4);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
            Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, spot);
            ItemStack helmet = namedGear("keeper-hat", Items.DIAMOND_HELMET, 11);
            ItemStack expectedHelmet = helmet.copy();
            equipVillager(villager, helmet, ItemStack.EMPTY, ItemStack.EMPTY);

            zombie.killedEntity(level, villager);

            helper.assertTrue(ItemStack.isSameItemSameComponents(expectedHelmet, villager.getItemBySlot(EquipmentSlot.HEAD)),
                    "a vetoed conversion must leave the villager's gear untouched");
            helper.assertFalse(villager.isRemoved(), "a vetoed conversion must not remove the villager");
            helper.assertValueEqual(droppedItems(helper, spot).size(), 0,
                    "a vetoed conversion must not drop any gear");
            helper.assertTrue(helper.findEntities(EntityType.ZOMBIE_VILLAGER, spot.getX(), spot.getY(), spot.getZ(), 10.0).isEmpty(),
                    "a vetoed conversion must not spawn a zombie villager");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            level.getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void removedVillagerConversionDropsNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Difficulty previous = level.getDifficulty();
        try {
            makeHard(level, previous);
            BlockPos spot = new BlockPos(4, 1, 4);
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 1, 2));
            Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, spot);
            ItemStack helmet = namedGear("keeper-hat", Items.DIAMOND_HELMET, 11);
            equipVillager(villager, helmet, ItemStack.EMPTY, ItemStack.EMPTY);
            villager.discard();

            zombie.killedEntity(level, villager);

            helper.assertValueEqual(droppedItems(helper, spot).size(), 0,
                    "a conversion that returns null must drop nothing");
            helper.assertTrue(helper.findEntities(EntityType.ZOMBIE_VILLAGER, spot.getX(), spot.getY(), spot.getZ(), 10.0).isEmpty(),
                    "a removed villager must not produce a zombie villager");
        } finally {
            level.getServer().setDifficulty(previous, true);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void villagerDoesNotWantDroppedArmor(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(4, 1, 4));
        helper.assertFalse(villager.wantsToPickUp(new ItemStack(Items.DIAMOND_CHESTPLATE)),
                "villagers must not want to pick up dropped armor");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void dispenserStillEquipsVillagerArmor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos dispenser = helper.absolutePos(new BlockPos(3, 1, 4));
        BlockState dispenserState = Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST);
        level.setBlock(dispenser, dispenserState, 3);
        DispenserBlockEntity blockEntity = (DispenserBlockEntity) level.getBlockEntity(dispenser);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(4, 1, 4));
        double armorBefore = villager.getAttributeValue(Attributes.ARMOR);

        boolean dispensed = ArmorItem.dispenseArmor(
                new BlockSource(level, dispenser, dispenserState, blockEntity),
                new ItemStack(Items.DIAMOND_CHESTPLATE));
        helper.assertTrue(dispensed, "the vanilla dispenser equip path must still run");
        helper.assertTrue(villager.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),
                "the dispenser must still equip the villager with armor");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(villager.getAttributeValue(Attributes.ARMOR) > armorBefore,
                    "equipped armor must raise the villager's armor attribute");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = OFF, timeoutTicks = 200)
    public void skeletonStrafeTracksTargetWithLookControl(GameTestHelper helper) {
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(2, 1, 4));
        skeleton.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        Pig target = helper.spawn(EntityType.PIG, new BlockPos(5, 1, 4));
        skeleton.setTarget(target);
        RangedBowAttackGoal<Skeleton> goal = new RangedBowAttackGoal<>(skeleton, 1.0, 20, 15.0F);
        setInt(GOAL_SEE_TIME, goal, 20);
        setInt(GOAL_STRAFING_TIME, goal, 5);
        skeleton.getLookControl().setLookAt(0.0, 0.0, 0.0);
        goal.tick();
        helper.assertTrue(Math.abs(skeleton.getLookControl().getWantedX() - target.getX()) < 0.001,
                "the strafing skeleton must feed its look control with the target");
        helper.assertTrue(Math.abs(skeleton.getLookControl().getWantedZ() - target.getZ()) < 0.001,
                "the strafing skeleton must feed its look control with the target");
        float forward = getFloat(MOVE_STRAFE_FORWARD, skeleton.getMoveControl());
        float right = getFloat(MOVE_STRAFE_RIGHT, skeleton.getMoveControl());
        helper.assertTrue(forward != 0.0F || right != 0.0F,
                "the strafe movement command must still be issued");
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = OFF)
    public void strafeGoalNoTargetDoesNotCrash(GameTestHelper helper) {
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(2, 1, 4));
        skeleton.setTarget(null);
        RangedBowAttackGoal<Skeleton> goal = new RangedBowAttackGoal<>(skeleton, 1.0, 20, 15.0F);
        goal.tick();
        helper.succeed();
    }
}
