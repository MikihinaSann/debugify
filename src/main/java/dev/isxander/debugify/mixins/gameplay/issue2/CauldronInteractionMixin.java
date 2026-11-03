package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional tweak: emptying a water bucket into a cauldron in an ultrawarm
 * dimension (the Nether) makes the water evaporate instead of filling it -
 * the bucket is consumed like a normal fill, the cauldron is left empty, and
 * an extinguish sound plus smoke plays. Off by default.
 */
@Mixin(CauldronInteraction.class)
public interface CauldronInteractionMixin {
    @Inject(method = "emptyBucket", at = @At("HEAD"), cancellable = true)
    private static void debugify$netherCauldronEvaporation(Level level, BlockPos pos, Player player, InteractionHand hand,
                                                           ItemStack filledStack, BlockState state, SoundEvent emptySound,
                                                           CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.NETHER_CAULDRON_EVAPORATION)
                || !state.is(Blocks.WATER_CAULDRON)
                || !level.dimensionType().ultraWarm()) {
            return;
        }

        if (!level.isClientSide) {
            Item item = filledStack.getItem();
            player.setItemInHand(hand, ItemUtils.createFilledResult(filledStack, player, new ItemStack(Items.BUCKET)));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(item));
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.25, 0.05, 0.25, 0.0);
            }
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }

        cir.setReturnValue(ItemInteractionResult.sidedSuccess(level.isClientSide));
    }
}
