package dev.isxander.debugify.helpers.mc2164;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Finds a safe teleport destination for an ender pearl's owner.
 * <p>
 * The pearl's hitbox is much smaller than its owner's, so it can end up inside
 * blocks a player cannot stand in (glass panes, fences, walls). When the
 * landing position collides, exactly one candidate is tried: the position on
 * the incoming side of the struck face. No arbitrary neighbour search and no
 * floor requirement - a position is accepted only when the owner's real
 * bounding box fits there without colliding and inside the world border.
 */
public final class EnderPearlTeleportSafety {
    private static final double EPSILON = 1.0E-4;

    private EnderPearlTeleportSafety() {
    }

    /**
     * Whether the owner's current bounding box, moved so its position lands on
     * {@code destination}, fits without block collisions and inside the world
     * border. Uses the entity's live bounding box so crouching, swimming,
     * scaled and baby entities are all measured correctly.
     */
    public static boolean isDestinationSafe(ServerLevel level, Entity owner, Vec3 destination) {
        AABB translated = owner.getBoundingBox().move(destination.subtract(owner.position()));
        return level.noBlockCollision(owner, translated) && level.getWorldBorder().isWithinBounds(translated);
    }

    /**
     * Returns the destination the pearl's owner should teleport to.
     * The original {@code target} is returned unchanged when it is already
     * safe (including midair). On collision, a single position on the incoming
     * side of the struck face is tried; if it does not fit either, the result
     * is empty and the teleport should be skipped entirely.
     */
    public static Optional<Vec3> findDestination(ServerLevel level, Entity owner, Vec3 target, HitResult hit) {
        if (isDestinationSafe(level, owner, target)) {
            return Optional.of(target);
        }

        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        Direction face = blockHit.getDirection();
        Vec3 hitLocation = blockHit.getLocation();
        // Local offsets of the live bounding box relative to the entity's
        // position; correct for crouching, scaled and otherwise non-default
        // boxes, not just symmetric getBbWidth/getBbHeight ones.
        AABB local = owner.getBoundingBox().move(owner.position().reverse());
        double x = target.x;
        double y = target.y;
        double z = target.z;
        switch (face.getAxis()) {
            case X -> x = face.getStepX() > 0
                    ? hitLocation.x - local.minX + EPSILON
                    : hitLocation.x - local.maxX - EPSILON;
            case Z -> z = face.getStepZ() > 0
                    ? hitLocation.z - local.minZ + EPSILON
                    : hitLocation.z - local.maxZ - EPSILON;
            case Y -> y = face == Direction.UP
                    ? hitLocation.y - local.minY + EPSILON
                    : hitLocation.y - local.maxY - EPSILON;
        }

        Vec3 candidate = new Vec3(x, y, z);
        return isDestinationSafe(level, owner, candidate) ? Optional.of(candidate) : Optional.empty();
    }
}
