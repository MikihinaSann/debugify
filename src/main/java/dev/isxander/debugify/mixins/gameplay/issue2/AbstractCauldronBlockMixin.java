package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional tweak: bucket interactions on a submerged cauldron (water above
 * it). Powder snow buckets do nothing - powder cannot exist underwater - and
 * lava buckets replace the cauldron with obsidian outright, consuming the
 * bucket exactly like vanilla's empty-bucket interaction. Off by default.
 */
@Mixin(AbstractCauldronBlock.class)
public class AbstractCauldronBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void debugify$underwaterCauldronInteraction(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hitResult,
                                                        CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.UNDERWATER_CAULDRONS)
                || !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return;
        }

        if (stack.is(Items.POWDER_SNOW_BUCKET)) {
            cir.setReturnValue(ItemInteractionResult.sidedSuccess(level.isClientSide));
        } else if (stack.is(Items.LAVA_BUCKET)) {
            cir.setReturnValue(CauldronInteraction.emptyBucket(
                    level, pos, player, hand, stack, Blocks.OBSIDIAN.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA));
        }
    }
}
