package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional tweak: placing a cauldron into water yields a full water cauldron
 * instead of an empty one that displaces the water. The method lives on
 * {@link Block} because cauldron does not override it; the identity check
 * keeps every other block on the vanilla path. Off by default.
 */
@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void debugify$underwaterCauldronPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if ((Object) this == Blocks.CAULDRON
                && Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.UNDERWATER_CAULDRONS)
                && context.getLevel().getFluidState(context.getClickedPos()).is(FluidTags.WATER)) {
            cir.setReturnValue(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        }
    }
}
