package dev.isxander.debugify.mixins.gameplay.issue3;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue3Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @Unique
    private BlockPos debugify$lastBell;
    @Unique
    private int debugify$bellCooldown;

    @Inject(method = "tick", at = @At("RETURN"))
    private void debugify$ringTouchedBell(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide
                || self.isRemoved()
                || self.getItem().isEmpty()
                || !Debugify.CONFIG.isIssue3TweakEnabled(Issue3Tweak.DROPPED_ITEMS_RING_BELLS)) {
            return;
        }
        if (debugify$bellCooldown > 0) {
            debugify$bellCooldown--;
        }
        BlockPos bell = debugify$bellTouching(self, level);
        if (bell == null) {
            debugify$lastBell = null;
        } else if (!bell.equals(debugify$lastBell)) {
            debugify$lastBell = bell;
            if (debugify$bellCooldown == 0) {
                debugify$bellCooldown = 20;
                ((BellBlock) level.getBlockState(bell).getBlock()).attemptToRing(level, bell, null);
            }
        }
    }

    @Unique
    private static BlockPos debugify$bellTouching(ItemEntity item, Level level) {
        AABB box = item.getBoundingBox().inflate(0.05);
        for (BlockPos pos : BlockPos.betweenClosed(
                (int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.floor(box.maxX), (int) Math.floor(box.maxY), (int) Math.floor(box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BellBlock)) {
                continue;
            }
            for (AABB bellBox : state.getCollisionShape(level, pos).toAabbs()) {
                if (bellBox.move(pos).intersects(box)) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }
}
