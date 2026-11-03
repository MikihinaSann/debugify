package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.WaterAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional tweak: a {@link MobEffects#WATER_BREATHING} water breathing effect
 * keeps fish, squid and other water animals' air topped up out of water
 * instead of letting them suffocate. Off by default.
 */
@Mixin(WaterAnimal.class)
public class WaterAnimalMixin {
    @Inject(method = "handleAirSupply", at = @At("HEAD"), cancellable = true)
    private void debugify$waterBreathingProtectsFish(int airSupply, CallbackInfo ci) {
        WaterAnimal self = (WaterAnimal) (Object) this;
        if (Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.WATER_BREATHING_PROTECTS_FISH)
                && self.hasEffect(MobEffects.WATER_BREATHING)) {
            self.setAirSupply(self.getMaxAirSupply());
            ci.cancel();
        }
    }
}
