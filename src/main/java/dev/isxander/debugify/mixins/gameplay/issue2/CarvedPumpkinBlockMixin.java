package dev.isxander.debugify.mixins.gameplay.issue2;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optional tweak: building a snow golem with a jack o' lantern keeps the
 * lantern as the golem's head equipment instead of silently degrading to the
 * carved-pumpkin look. The equipped slot is what the faster attack interval
 * and lantern rendering hooks check. Off by default.
 */
@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {
    @WrapOperation(
            method = "trySpawnGolem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;spawnGolemInWorld(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)V")
    )
    private static void debugify$equipJackOLantern(Level level, BlockPattern.BlockPatternMatch patternMatch, Entity golem, BlockPos pos,
                                                   Operation<Void> original) {
        // Read the head block before the original call clears the pattern;
        // pos is the bottom pattern block (snow/iron) so the head sits 2 up.
        boolean jackOLantern = golem instanceof SnowGolem
                && level.getBlockState(pos.above(2)).is(Blocks.JACK_O_LANTERN);
        original.call(level, patternMatch, golem, pos);
        if (Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.JACK_O_LANTERN_SNOW_GOLEMS)
                && golem instanceof SnowGolem snowGolem
                && jackOLantern) {
            snowGolem.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.JACK_O_LANTERN));
        }
    }
}
