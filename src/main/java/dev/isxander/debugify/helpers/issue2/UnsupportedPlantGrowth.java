package dev.isxander.debugify.helpers.issue2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared check for the optional plant-growth tweak: a growing plant block is
 * unsupported when the block itself cannot survive, or when the bottom of the
 * contiguous column of the same plant sits on an invalid base.
 */
public final class UnsupportedPlantGrowth {
    private UnsupportedPlantGrowth() {
    }

    public static boolean isUnsupported(BlockState state, ServerLevel level, BlockPos pos) {
        if (!state.canSurvive(level, pos)) {
            return true;
        }

        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (cursor.getY() > level.getMinBuildHeight() && level.getBlockState(cursor.below()).is(state.getBlock())) {
            cursor.move(Direction.DOWN);
        }

        return !level.getBlockState(cursor).canSurvive(level, cursor);
    }
}
