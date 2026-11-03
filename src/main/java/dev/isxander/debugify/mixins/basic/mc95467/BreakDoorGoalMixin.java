package dev.isxander.debugify.mixins.basic.mc95467;

import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes <a href="https://bugs.mojang.com/browse/MC-95467">MC-95467</a>.
 * {@link DoorInteractGoal#isOpen()} is false both for a closed door and for a
 * non-door block, so the goal deletes whatever replaced the door at break
 * time. The original door state is cached on start and both canContinueToUse
 * and tick abort once the block is no longer that exact door.
 */
@BugFix(id = "MC-95467", category = FixCategory.BASIC, env = BugFix.Env.SERVER)
@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin extends DoorInteractGoal {
    @Unique
    private BlockState debugify$originalDoorState;

    public BreakDoorGoalMixin(Mob mob) {
        super(mob);
    }

    @Inject(method = "start", at = @At("TAIL"))
    private void debugify$cacheDoorState(CallbackInfo ci) {
        this.debugify$originalDoorState = this.mob.level().getBlockState(this.doorPos);
    }

    @Unique
    private boolean debugify$isOriginalDoor() {
        if (this.debugify$originalDoorState == null) {
            return true;
        }
        BlockState current = this.mob.level().getBlockState(this.doorPos);
        return DoorBlock.isWoodenDoor(current)
                && current.equals(this.debugify$originalDoorState)
                && !current.getValue(DoorBlock.OPEN);
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void debugify$stopWhenDoorReplaced(CallbackInfoReturnable<Boolean> cir) {
        if (!this.debugify$isOriginalDoor()) {
            cir.setReturnValue(false);
        }
    }

    // The door can still be replaced between the canContinueToUse check and the
    // actual removal inside tick(), so guard the removal branch itself too.
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void debugify$stopBeforeRemoval(CallbackInfo ci) {
        if (!this.debugify$isOriginalDoor()) {
            // stop() clears the destroy-progress overlay before we bail out.
            this.stop();
            ci.cancel();
        }
    }
}
