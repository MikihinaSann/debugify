package dev.isxander.debugify.mixins.basic.mc261666;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@BugFix(id = "MC-261666", category = FixCategory.BASIC, env = BugFix.Env.SERVER)
@Mixin(Zombie.class)
public class ZombieMixin {
    @WrapOperation(method = "killedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;"))
    private Mob dropConvertedEquipment(Villager villager, EntityType<?> entityType, boolean transferInventory, Operation<Mob> original) {
        Mob converted = original.call(villager, entityType, transferInventory);
        if (converted != null && !transferInventory) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = villager.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    villager.spawnAtLocation(stack.copyAndClear());
                }
            }
        }
        return converted;
    }
}
