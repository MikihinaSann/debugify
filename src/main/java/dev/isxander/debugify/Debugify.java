package dev.isxander.debugify;

import dev.isxander.debugify.config.DebugifyConfig;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.mixinplugin.DebugifyErrorHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.LoadingModList;
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

    public Debugify(ModContainer container) {
        onInitialize();
        // Debugify registers no network channels, so NeoForge never requires it
        // on the remote side — matching upstream's optional-everywhere behaviour.
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

        var alreadyFixed = DebugifyErrorHandler.getAlreadyFixed();
        if (!alreadyFixed.isEmpty()) {
            List<String> skipped = alreadyFixed.stream()
                    .map(BugFixData::bugId)
                    .sorted()
                    .toList();
            LOGGER.info("Skipped {} bug fixes (already fixed by NeoForge or another mod): {}", skipped.size(), skipped);
        }

        var errored = DebugifyErrorHandler.getErrored();
        if (!errored.isEmpty()) {
            List<String> errors = errored.stream()
                    .map(BugFixData::bugId)
                    .sorted()
                    .toList();
            LOGGER.warn("Failed to apply {} bug fixes: {}", errors.size(), errors);
        }

        LOGGER.info("Successfully Debugify'd your game!");
    }

    /**
     * ModList is not populated during mixin loading, so early lookups fall back
     * to LoadingModList, which is built during mod discovery before mixin
     * configs are evaluated.
     */
    public static boolean isModLoaded(String modId) {
        ModList modList = ModList.get();
        if (modList != null) return modList.isLoaded(modId);
        LoadingModList loadingModList = LoadingModList.get();
        return loadingModList != null && loadingModList.getModFileById(modId) != null;
    }

    public static String getVersion() {
        ModList modList = ModList.get();
        if (modList == null) return "unknown";
        return modList.getModContainerById(MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    public static BugFix.Env getEnv() {
        return FMLEnvironment.dist == Dist.CLIENT ? BugFix.Env.CLIENT : BugFix.Env.SERVER;
    }
}
