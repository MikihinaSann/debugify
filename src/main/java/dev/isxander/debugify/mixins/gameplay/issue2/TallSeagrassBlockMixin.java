package dev.isxander.debugify.mixins.gameplay.issue2;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optional tweak: tall seagrass may only be planted on dirt- or sand-type
 * blocks, instead of any block with a sturdy top face. Off by default.
 */
@Mixin(TallSeagrassBlock.class)
public class TallSeagrassBlockMixin {
    @ModifyReturnValue(method = "mayPlaceOn", at = @At("RETURN"))
    private boolean debugify$restrictSubstrate(boolean original, BlockState state, BlockGetter level, BlockPos pos) {
        if (!original || !Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.RESTRICT_SEAGRASS_SUBSTRATES)) {
            return original;
        }
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND);
    }
}
