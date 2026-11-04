package dev.isxander.debugify.mixins.gameplay.issue3;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue3Tweak;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkeletonHorse.class)
public abstract class SkeletonHorseMixin extends AbstractHorse {
    protected SkeletonHorseMixin(EntityType<? extends AbstractHorse> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void debugify$tameOnEmptyHandInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Debugify.CONFIG.isIssue3TweakEnabled(Issue3Tweak.TAME_SKELETON_HORSES_ON_INTERACTION)
                || this.isTamed()
                || this.isBaby()
                || this.isVehicle()
                || player.isSecondaryUseActive()
                || !player.getItemInHand(hand).isEmpty()) {
            return;
        }
        if (!this.level().isClientSide) {
            this.setTamed(true);
        }
        cir.setReturnValue(super.mobInteract(player, hand));
    }
}
