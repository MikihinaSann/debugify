package dev.isxander.debugify.client.mixins.gameplay.issue2;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.SnowGolemHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optional tweak: renders a jack o' lantern on snow golems equipped with one,
 * instead of always showing a carved pumpkin. Both the item stack and the
 * glowing-outline block state read the same equipped-head predicate. Off by
 * default.
 */
@Mixin(SnowGolemHeadLayer.class)
public class SnowGolemHeadLayerMixin {
    @ModifyExpressionValue(
            method = "render",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;CARVED_PUMPKIN:Lnet/minecraft/world/level/block/Block;", opcode = org.objectweb.asm.Opcodes.GETSTATIC)
    )
    private Block debugify$renderJackOLantern(Block original, PoseStack poseStack, MultiBufferSource buffer, int packedLight, SnowGolem livingEntity) {
        if (Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.JACK_O_LANTERN_SNOW_GOLEMS)
                && livingEntity.hasPumpkin()
                && livingEntity.getItemBySlot(EquipmentSlot.HEAD).is(Items.JACK_O_LANTERN)) {
            return Blocks.JACK_O_LANTERN;
        }
        return original;
    }
}
