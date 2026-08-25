package dev.isxander.debugify.mixinplugin;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.api.DebugifyApi;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.fixes.OS;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.*;

public class MixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        Debugify.onPreInitialize();

        // Forge equivalent of Fabric's "debugify" entrypoint: other mods send
        // DebugifyApi suppliers via InterModComms targeting our mod id.
        // ModList may be null during early mixin loading — guard against that.
        ModList modList = ModList.get();
        InterModComms.getMessages(Debugify.MODID)
                .forEach(imc -> {
                    final Object message = imc.messageSupplier().get();
                    if (!(message instanceof DebugifyApi api)) return;
                    String containerModId = imc.senderModId();
                    for (String bugId : api.getDisabledFixes()) {
                        BugFixData.registerApiConflict(containerModId, bugId);
                    }

                    api.getProvidedDisabledFixes().forEach((modId, bugs) -> {
                        if (modList != null && modList.isLoaded(modId)) {
                            bugs.forEach(bugId -> BugFixData.registerApiConflict(modId, bugId));
                        }
                    });
                });
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }


    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        Optional<BugFixData> bugFixOptional = BugFixDataCache.getOrResolve(mixinClassName);

        if (bugFixOptional.isEmpty())
            return true;

        BugFixData bugFix = bugFixOptional.get();
        var multipleMixins = Debugify.CONFIG.getBugFixes().containsKey(bugFix);
        Debugify.CONFIG.registerBugFix(bugFix);

        if (DebugifyErrorHandler.hasErrored(bugFix)) {
            Debugify.LOGGER.warn("Preventing loading of {} mixin, {} because another mixin for the same bug fix failed to apply.", bugFix.bugId(), mixinClassName);
            return false;
        }

        Set<String> conflicts = bugFix.getActiveConflicts();
        if (!conflicts.isEmpty()) {
            if (Debugify.CONFIG.isBugFixEnabled(bugFix) && !multipleMixins)
                Debugify.LOGGER.warn("Force disabled {} because it's conflicting with: {}", bugFix.bugId(), String.join(", ", conflicts));
            return false;
        } else if (!bugFix.satisfiesOSRequirement()) {
            if (Debugify.CONFIG.isBugFixEnabled(bugFix) && !multipleMixins)
                Debugify.LOGGER.warn("Force disabled {} because it only applies to OS: {}", bugFix.bugId(), bugFix.requiredOs().name());
            return false;
        }

        return Debugify.CONFIG.isBugFixEnabled(bugFix);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
