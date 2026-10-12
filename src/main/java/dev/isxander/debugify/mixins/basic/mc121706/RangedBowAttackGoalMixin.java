package dev.isxander.debugify.mixins.basic.mc121706;

import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@BugFix(id = "MC-121706", env = BugFix.Env.SERVER, category = FixCategory.BASIC)
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalMixin {
    @Shadow @Final private Mob mob;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;lookAt(Lnet/minecraft/world/entity/Entity;FF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void lookAtTarget(CallbackInfo ci) {
        mob.getLookControl().setLookAt(mob.getTarget(), 30f, 30f);
    }
}
