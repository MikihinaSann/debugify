package dev.isxander.debugify.client.mixins.basic.mc249414;

import com.google.gson.JsonElement;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@BugFix(id = "MC-249414", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {
    @Shadow @Final private String name;

    /**
     * rendertype_entity_translucent_emissive declares Sampler2 but never
     * samples it, so updateLocations warns it cannot bind it. Skipping the
     * declaration leaves identical runtime state minus the log warning.
     */
    @Inject(method = "parseSamplerNode", at = @At("HEAD"), cancellable = true)
    private void skipUnusedSampler(JsonElement element, CallbackInfo ci) {
        if (this.name.equals("rendertype_entity_translucent_emissive")
                && element.getAsJsonObject().get("name").getAsString().equals("Sampler2")) {
            ci.cancel();
        }
    }
}
