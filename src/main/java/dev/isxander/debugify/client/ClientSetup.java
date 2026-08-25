package dev.isxander.debugify.client;

import dev.isxander.debugify.client.gui.DebugifyConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.function.Function;

/**
 * Client-only setup. This class is only loaded on the client dist via
 * {@link net.minecraftforge.fml.DistExecutor#unsafeRunWhenOn}, so it is safe
 * to reference client-only classes here.
 */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init() {
        DebugifyClient.onClientInit();

        // Register the in-game config screen shown by the Mods button.
        Function<Screen, Screen> factory = parent -> new DebugifyConfigScreen(parent);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(factory));
    }
}
