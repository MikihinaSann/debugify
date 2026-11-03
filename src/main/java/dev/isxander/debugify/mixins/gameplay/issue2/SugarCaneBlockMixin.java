package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import dev.isxander.debugify.helpers.issue2.UnsupportedPlantGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional tweak: sugar cane only grows while its whole column is supported -
 * the block itself must survive AND the base of the contiguous cane column
 * must survive. Vanilla lets orphaned floating segments grow until a
 * neighbour update removes them. Off by default.
 */
@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void debugify$preventUnsupportedGrowth(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.PREVENT_UNSUPPORTED_PLANT_GROWTH)
                && UnsupportedPlantGrowth.isUnsupported(state, level, pos)) {
            ci.cancel();
        }
    }
}
