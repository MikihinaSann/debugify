package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional tweak: a snowball that is on fire (for example after flying
 * through lava) ignites the entity it hits for 5 seconds, like a flaming
 * arrow. Normal snowballs are unchanged. Off by default.
 */
@Mixin(Snowball.class)
public class SnowballMixin {
    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void debugify$igniteStruckEntity(EntityHitResult result, CallbackInfo ci) {
        Snowball self = (Snowball) (Object) this;
        if (Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.BURNING_SNOWBALLS)
                && !self.level().isClientSide
                && self.isOnFire()) {
            result.getEntity().igniteForSeconds(5.0F);
        }
    }
}
