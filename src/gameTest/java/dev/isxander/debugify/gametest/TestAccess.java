package dev.isxander.debugify.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Test-only subclasses that expose protected methods/fields needed to drive
 * the real, mixin-transformed code paths through GameTests.
 */
final class TestAccess {
    private TestAccess() {
    }

    /** Exposes the protected {@link ThrownEnderpearl#onHit(HitResult)}. */
    static class Pearl extends ThrownEnderpearl {
        Pearl(Level level, LivingEntity shooter) {
            super(level, shooter);
        }

        void hit(HitResult result) {
            this.onHit(result);
        }
    }

    /** Exposes the protected {@link Snowball#onHitEntity(EntityHitResult)}. */
    static class TestSnowball extends Snowball {
        TestSnowball(Level level) {
            super(EntityType.SNOWBALL, level);
        }

        void hitEntity(Entity entity) {
            this.onHitEntity(new EntityHitResult(entity));
        }
    }

    /** Lets tests aim the goal at a specific door position like canUse() would. */
    static class DoorGoal extends BreakDoorGoal {
        DoorGoal(Mob mob) {
            super(mob, difficulty -> difficulty == Difficulty.HARD || difficulty == Difficulty.NORMAL
                    || difficulty == Difficulty.EASY || difficulty == Difficulty.PEACEFUL);
        }

        void targetDoor(BlockPos pos) {
            this.doorPos = pos;
            this.hasDoor = true;
        }
    }
}
