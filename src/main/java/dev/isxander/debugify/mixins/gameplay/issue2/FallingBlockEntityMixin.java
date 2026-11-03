package dev.isxander.debugify.mixins.gameplay.issue2;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optional tweak: an anvil landing on a slime or honey block keeps its damage
 * state instead of degrading or breaking. Only the {@code AnvilBlock.damage}
 * call is skipped - entities under the anvil still take fall damage and every
 * other support block behaves like vanilla. Off by default.
 */
@Mixin(FallingBlockEntity.class)
public class FallingBlockEntityMixin {
    @WrapOperation(
            method = "causeFallDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AnvilBlock;damage(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private BlockState debugify$cushionedAnvil(BlockState state, Operation<BlockState> original) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.CUSHIONED_ANVILS)) {
            // Honey is not full height: the entity sinks into it, so check
            // both the supporting block and the one the feet are inside.
            BlockState support = self.level().getBlockState(self.getOnPos());
            BlockState atFeet = self.level().getBlockState(self.blockPosition());
            if (isCushion(support) || isCushion(atFeet)) {
                return state;
            }
        }
        return original.call(state);
    }

    private static boolean isCushion(BlockState state) {
        return state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
    }
}
