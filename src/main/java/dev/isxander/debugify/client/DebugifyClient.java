package dev.isxander.debugify.client;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.client.gui.DebugifyConfigScreen;
import dev.isxander.debugify.client.utils.BugFixDescriptionCache;
import dev.isxander.debugify.client.utils.ClientUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only mod entrypoint. NeoForge never class-loads this on a dedicated
 * server, so it is safe to reference client-only classes here.
 */
@Mod(value = Debugify.MODID, dist = Dist.CLIENT)
public class DebugifyClient {
    public static BugFixDescriptionCache bugFixDescriptionCache;

    public DebugifyClient(ModContainer container) {
        bugFixDescriptionCache = new BugFixDescriptionCache();
        bugFixDescriptionCache.loadDescriptions();
        bugFixDescriptionCache.cacheMissingDescriptions();

        // In-game config screen opened from the Mods screen (NeoForge's
        // equivalent of upstream's ModMenu integration).
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new DebugifyConfigScreen(parent));
    }

    public static boolean isGameplayFixesEnabled() {
        return !ClientUtils.isInMultiplayerWorld() || Debugify.CONFIG.gameplayFixesInMultiplayer;
    }
}
