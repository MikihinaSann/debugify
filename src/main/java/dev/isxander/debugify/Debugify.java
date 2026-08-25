package dev.isxander.debugify;

import dev.isxander.debugify.config.DebugifyConfig;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.mixinplugin.DebugifyErrorHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixins;

import java.util.List;
import java.util.Map;

@Mod(Debugify.MODID)
public class Debugify {
    public static final String MODID = "debugify";
    public static final Logger LOGGER = LoggerFactory.getLogger("Debugify");
    public static final DebugifyConfig CONFIG = new DebugifyConfig();

    public Debugify() {
        onInitialize();
        // Allow client/server to differ in presence (matches Fabric behaviour where Debugify is optional on both sides).
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "1.20.1+1.0", (remoteVersion, isRemote) -> true));

        // Client-only setup (config screen registration + description cache) is routed
        // through DistExecutor so client classes are never class-loaded on a dedicated server.
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> dev.isxander.debugify.client.ClientSetup::init);
    }

    /**
     * Called from mixin plugin to manage
     * disabled bug fixes
     */
    public static void onPreInitialize() {
        CONFIG.preload();
        Mixins.registerErrorHandlerClass(DebugifyErrorHandler.class.getName());
    }

    public static void onInitialize() {
        List<String> enabledBugs = CONFIG.getBugFixes().entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(entry -> entry.getKey().bugId())
                .toList();
        LOGGER.info("Enabled {} bug fixes: {}", enabledBugs.size(), enabledBugs);

        // Report fixes that were skipped because the bug was already fixed
        // by Forge or another mod (injection targets not found).
        var alreadyFixed = dev.isxander.debugify.mixinplugin.DebugifyErrorHandler.getAlreadyFixed();
        if (!alreadyFixed.isEmpty()) {
            List<String> skipped = alreadyFixed.stream()
                    .map(BugFixData::bugId)
                    .sorted()
                    .toList();
            LOGGER.info("Skipped {} bug fixes (already fixed by Forge or another mod): {}", skipped.size(), skipped);
        }

        var errored = dev.isxander.debugify.mixinplugin.DebugifyErrorHandler.getErrored();
        if (!errored.isEmpty()) {
            List<String> errors = errored.stream()
                    .map(BugFixData::bugId)
                    .sorted()
                    .toList();
            LOGGER.warn("Failed to apply {} bug fixes: {}", errors.size(), errors);
        }

        LOGGER.info("Successfully Debugify'd your game!");
    }

    public static BugFix.Env getEnv() {
        return FMLEnvironment.dist == Dist.CLIENT ? BugFix.Env.CLIENT : BugFix.Env.SERVER;
    }
}
