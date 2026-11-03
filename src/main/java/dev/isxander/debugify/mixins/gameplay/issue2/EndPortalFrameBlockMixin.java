package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Optional tweak: breaking a frame that was part of a complete end portal
 * extinguishes the portal - removing the portal blocks inside that portal's
 * 3x3 opening only. The original full ring (all 12 frames with eyes, facing
 * inward) must still be present for anything to happen, so standalone or
 * neighbouring portals and partial rings are untouched. Off by default.
 */
@Mixin(EndPortalFrameBlock.class)
public abstract class EndPortalFrameBlockMixin extends Block {
    public EndPortalFrameBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level.isClientSide
                || !Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.END_PORTALS_REQUIRE_FRAMES)
                || state.is(newState.getBlock())
                || !state.getValue(EndPortalFrameBlock.HAS_EYE)) {
            return;
        }

        Direction facing = state.getValue(EndPortalFrameBlock.FACING);
        // Frame eyes point toward their portal centre, which sits 2 blocks
        // inward from a middle edge frame or diagonally from an edge-end one.
        for (int offset = -1; offset <= 1; offset++) {
            BlockPos center = pos.relative(facing, 2).relative(facing.getClockWise(), offset);
            if (debugify$isCompleteRing(level, pos, state, center)) {
                debugify$clearPortalInterior(level, center);
            }
        }
    }

    /**
     * Whether {@code center} is still surrounded by a complete 5x5 ring of
     * end portal frames with eyes, all facing inward. The state at
     * {@code removedPos} is substituted with the state it had before removal.
     */
    @Unique
    private boolean debugify$isCompleteRing(Level level, BlockPos removedPos, BlockState oldState, BlockPos center) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos edgeMiddle = center.relative(facing.getOpposite(), 2);
            for (int i = -1; i <= 1; i++) {
                BlockPos framePos = edgeMiddle.relative(facing.getClockWise(), i);
                BlockState frame = framePos.equals(removedPos) ? oldState : level.getBlockState(framePos);
                if (!frame.is(Blocks.END_PORTAL_FRAME)
                        || !frame.getValue(EndPortalFrameBlock.HAS_EYE)
                        || frame.getValue(EndPortalFrameBlock.FACING) != facing) {
                    return false;
                }
            }
        }
        return true;
    }

    @Unique
    private void debugify$clearPortalInterior(Level level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos interior = center.offset(dx, 0, dz);
                if (level.getBlockState(interior).is(Blocks.END_PORTAL)) {
                    level.removeBlock(interior, false);
                }
            }
        }
    }
}
