package dev.isxander.debugify.mixins.gameplay.issue2;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.config.Issue2Tweak;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Optional tweak: arrows schedule a block tick on the falling block they hit,
 * so unsupported sand, gravel and friends drop when shot. The vanilla tick
 * still checks whether the block is actually free to fall, so supported
 * blocks stay put and non-arrow projectiles do nothing. Off by default.
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin extends Block {
    public FallingBlockMixin(Properties properties) {
        super(properties);
    }

    @Invoker("getDelayAfterPlace")
    abstract int debugify$delayAfterPlace();

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        super.onProjectileHit(level, state, hit, projectile);
        if (!level.isClientSide
                && Debugify.CONFIG.isIssue2TweakEnabled(Issue2Tweak.ARROWS_TRIGGER_FALLING_BLOCKS)
                && projectile instanceof AbstractArrow) {
            level.scheduleTick(hit.getBlockPos(), this, this.debugify$delayAfterPlace());
        }
    }
}
