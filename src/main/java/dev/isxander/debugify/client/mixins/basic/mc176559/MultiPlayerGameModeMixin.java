package dev.isxander.debugify.client.mixins.basic.mc176559;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;

@BugFix(id = "MC-176559", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(value = MultiPlayerGameMode.class, priority = 1010)
public class MultiPlayerGameModeMixin {
    // Fabric API also redirects here. WrapOperation is compatible
    @WrapOperation(method = "sameDestroyTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameTags(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean isSameItem(ItemStack mainHandItem, ItemStack destroyingItem, Operation<Boolean> original) {
        return isSameItemSameTagsIgnoringDurability(mainHandItem, destroyingItem);
    }

    @Unique
    private boolean isSameItemSameTagsIgnoringDurability(ItemStack stack1, ItemStack stack2) {
        if (!stack1.is(stack2.getItem())) {
            return false;
        } else if (stack1.isEmpty() && stack2.isEmpty()) {
            return true;
        } else {
            CompoundTag tag1 = stack1.getTag();
            CompoundTag tag2 = stack2.getTag();
            if (tag1 == null && tag2 == null) {
                return true;
            }
            if (tag1 == null || tag2 == null) {
                // One stack has a tag, the other doesn't. They are still equivalent
                // if the only extra entry is the Damage value.
                CompoundTag nonNull = tag1 != null ? tag1 : tag2;
                CompoundTag copy = nonNull.copy();
                copy.remove("Damage");
                return copy.isEmpty();
            }
            CompoundTag copy1 = tag1.copy();
            CompoundTag copy2 = tag2.copy();
            copy1.remove("Damage");
            copy2.remove("Damage");
            return copy1.equals(copy2);
        }
    }
}
