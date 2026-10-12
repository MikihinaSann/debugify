package dev.isxander.debugify.client.mixins.basic.mc93384;

import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@BugFix(id = "MC-93384", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(CommonHooks.class)
public abstract class CommonHooksMixin {
    /**
     * NeoForge reroutes vanilla's LivingEntity#baseTick drowning bubbles through
     * CommonHooks#onLivingBreathe, which keeps the same wrong-Y bug (particles
     * spawn at the feet instead of at eye height). Retarget the fix at the hook.
     */
    @Redirect(method = "onLivingBreathe", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private static void addBubbleAtEyeHeight(Level level, ParticleOptions particle, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, LivingEntity entity, int consumeAirAmount, int refillAirAmount) {
        level.addParticle(particle, x, y + entity.getEyeHeight(), z, xSpeed, ySpeed, zSpeed);
    }
}
