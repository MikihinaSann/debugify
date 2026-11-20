package dev.isxander.debugify.mixins.basic.mc214147;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Skeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@BugFix(id = "MC-214147", category = FixCategory.BASIC, env = BugFix.Env.SERVER)
@Mixin(Skeleton.class)
public class SkeletonMixin {
    /**
     * Skeleton#canFreeze is hard-false (it converts instead of freezing),
     * so mirror the freeze-immune gear check from LivingEntity#canFreeze here.
     */
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Skeleton;startFreezeConversion(I)V"))
    private boolean shouldStartConverting(Skeleton instance, int time) {
        return !instance.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
            && !instance.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
            && !instance.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES)
            && !instance.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
    }
}
