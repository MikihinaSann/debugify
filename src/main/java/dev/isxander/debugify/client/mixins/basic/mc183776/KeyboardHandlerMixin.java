package dev.isxander.debugify.client.mixins.basic.mc183776;

import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@BugFix(id = "MC-183776", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow private boolean handledDebugKey;

    // ponytail: clearing the latch on every F3-release return covers both stuck-flag paths
    // (screen consumed the release, or release processed while a non-passive screen is open)
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void clearHandledDebugKeyOnF3Release(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action == 0 && key == 292) {
            this.handledDebugKey = false;
        }
    }
}
