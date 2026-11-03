package dev.isxander.debugify.mixins.gameplay.issue2;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optional tweak: a snow golem wearing a jack o' lantern attacks roughly 3x
 * faster (its 20-tick attack interval becomes 7 ticks). Both interval
 * computations in {@link RangedAttackGoal#tick()} are scaled; shearing the
 * pumpkin off removes the boost again. Off by default.
 */
@Mixin(RangedAttackGoal.class)
public abstract class RangedAttackGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(F)I")
    )
    private int debugify$boostAttackIntervalFloat(int original) {
        return debugify$isLanternGolem() ? Math.max(1, Math.round(original / 3.0F)) : original;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I")
    )
    private int debugify$boostAttackIntervalDouble(int original) {
        return debugify$isLanternGolem() ? Math.max(1, Math.round(original / 3.0F)) : original;
    }

    private boolean debugify$isLanternGolem() {
        return Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.JACK_O_LANTERN_SNOW_GOLEMS)
                && this.mob instanceof SnowGolem snowGolem
                && snowGolem.hasPumpkin()
                && snowGolem.getItemBySlot(EquipmentSlot.HEAD).is(Items.JACK_O_LANTERN);
    }
}
