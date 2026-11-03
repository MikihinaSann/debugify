package dev.isxander.debugify.mixins.gameplay.mc2164;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import dev.isxander.debugify.helpers.mc2164.EnderPearlTeleportSafety;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Fixes <a href="https://bugs.mojang.com/browse/MC-2164">MC-2164</a>.
 * A pearl's hitbox is much smaller than its owner's, so the owner can land
 * inside solid collision. On collision the owner moves to the incoming side
 * of the struck face; if that does not fit either, the teleport is rejected
 * (the pearl is still consumed, only the position is left unchanged).
 */
@BugFix(id = "MC-2164", category = FixCategory.GAMEPLAY, env = BugFix.Env.SERVER)
@Mixin(ThrownEnderpearl.class)
public class ThrownEnderpearlMixin {
    @WrapOperation(
            method = "onHit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;")
    )
    private Entity debugify$teleportToSafeLocation(Entity owner, DimensionTransition transition, Operation<Entity> original, HitResult hitResult) {
        ThrownEnderpearl pearl = (ThrownEnderpearl) (Object) this;
        Vec3 target = transition.pos();

        if (!target.equals(pearl.position())) {
            // Another mod redirected the destination through the EnderPearl
            // event: honour it exactly as given when it is safe, never
            // second-guess it with our own adjustment.
            return EnderPearlTeleportSafety.isDestinationSafe(transition.newLevel(), owner, target)
                    ? original.call(owner, transition)
                    : null;
        }

        Optional<Vec3> destination = EnderPearlTeleportSafety.findDestination(transition.newLevel(), owner, target, hitResult);
        if (destination.isEmpty()) {
            // No safe spot on the near side of the struck block: reject the
            // teleport, leaving the owner where they are.
            return null;
        }

        Vec3 pos = destination.get();
        if (pos.equals(target)) {
            return original.call(owner, transition);
        }

        return original.call(owner, new DimensionTransition(
                transition.newLevel(),
                pos,
                transition.speed(),
                transition.yRot(),
                transition.xRot(),
                transition.missingRespawnBlock(),
                transition.postDimensionTransition()));
    }
}
