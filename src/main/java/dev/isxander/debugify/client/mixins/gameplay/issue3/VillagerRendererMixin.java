package dev.isxander.debugify.client.mixins.gameplay.issue3;

import dev.isxander.debugify.client.render.issue3.VillagerArmorLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerRenderer.class)
public class VillagerRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void debugify$addVillagerArmorLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        VillagerRenderer self = (VillagerRenderer) (Object) this;
        self.addLayer(new VillagerArmorLayer(self, context));
    }
}
