package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Holds every reference to client-only Minecraft / NeoForge classes
 * (Screen, IConfigScreenFactory, ExtraPlayerConfigScreen ...).
 *
 * <p>Must only be touched from a {@code Dist.CLIENT} guarded branch in
 * {@link YesSteveModelNeoForge}. Keeping these references out of the
 * {@code @Mod} entry class's constant pool prevents a dedicated server
 * from trying to resolve {@code net.minecraft.client.gui.screens.Screen}
 * at mod load time.
 */
public final class NeoForgeClientBootstrap {
    private NeoForgeClientBootstrap() {
    }

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parentScreen) -> new ExtraPlayerConfigScreen(null));
    }
}
