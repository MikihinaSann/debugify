package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Optional tweak: lava flowing over a waterlogged coral plant or fan hardens
// it into stone. spreadTo only fires for LiquidBlock targets so vanilla never
// reaches coral; a HEAD hook on spread covers the block directly below the
// lava. Off by default.
@Mixin(FlowingFluid.class)
public class LavaFluidMixin {
    @Inject(method = "spread", at = @At("HEAD"))
    private void debugify$hardenWaterloggedCoral(Level level, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (!((Object) this instanceof LavaFluid)
                || !Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.LAVA_CONVERTS_WATERLOGGED_CORAL)
                || state.isEmpty()) {
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!(belowState.getBlock() instanceof BaseCoralPlantTypeBlock)
                || !belowState.getFluidState().is(FluidTags.WATER)) {
            return;
        }
        BlockState replacement = EventHooks.fireFluidPlaceBlockEvent(level, below, pos, Blocks.STONE.defaultBlockState());
        if (!replacement.equals(belowState) && level.setBlock(below, replacement, 3)) {
            level.levelEvent(1501, below, 0); // LavaFluid.fizz
        }
    }
}
