package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Optional tweak: villagers explode beds they try to sleep in where beds do
// not work (dimensionType.bedWorks == false), mirroring player respawn
// explosions. The sleep is cancelled so no stale sleeping pose/POI remains.
// Off by default.
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "startSleeping", at = @At("HEAD"), cancellable = true)
    private void debugify$explodeBedInInvalidDimension(BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Level level = self.level();
        if (!(self instanceof Villager)
                || !(level instanceof ServerLevel)
                || !Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.VILLAGER_BEDS_EXPLODE_IN_INVALID_DIMENSIONS)
                || BedBlock.canSetSpawn(level)) {
            return;
        }
        BlockState bed = level.getBlockState(pos);
        if (!(bed.getBlock() instanceof BedBlock bedBlock)) {
            return;
        }
        // Normalise to the head half of this same bed, then clear its two
        // halves only - never an unrelated bed block.
        BlockPos head = pos;
        if (bed.getValue(BedBlock.PART) != BedPart.HEAD) {
            head = pos.relative(bed.getValue(BedBlock.FACING));
            bed = level.getBlockState(head);
            if (bed.getBlock() != bedBlock) {
                return;
            }
        }
        Direction facing = bed.getValue(BedBlock.FACING);
        BlockPos foot = head.relative(facing.getOpposite());
        level.removeBlock(head, false);
        if (level.getBlockState(foot).getBlock() == bedBlock) {
            level.removeBlock(foot, false);
        }
        Vec3 center = head.getCenter();
        level.explode(null, level.damageSources().badRespawnPointExplosion(center), null,
                center, 5.0F, true, Level.ExplosionInteraction.BLOCK);
        ci.cancel();
    }
}
