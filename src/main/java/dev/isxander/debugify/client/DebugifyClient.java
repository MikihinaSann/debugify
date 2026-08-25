package dev.isxander.debugify.client;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.client.utils.BugFixDescriptionCache;

public class DebugifyClient {
    public static BugFixDescriptionCache bugFixDescriptionCache;

    public static void onClientInit() {
        bugFixDescriptionCache = new BugFixDescriptionCache();
        bugFixDescriptionCache.loadDescriptions();
        bugFixDescriptionCache.cacheMissingDescriptions();
    }

    public static boolean isGameplayFixesEnabled() {
        return Debugify.CONFIG.gameplayFixesInMultiplayer;
    }
}
