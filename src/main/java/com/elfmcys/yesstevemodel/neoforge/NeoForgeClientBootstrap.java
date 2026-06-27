package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerConfigScreen;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Holds everything that touches client-only Minecraft / NeoForge classes
 * (Screen, IConfigScreenFactory, ClientEventBridge ...).
 *
 * <p>This class must only be referenced inside a {@code Dist.CLIENT} guarded
 * branch in {@link YesSteveModelNeoForge}. Keeping the references out of the
 * {@code @Mod} entry class's constant pool is what prevents a dedicated server
 * from trying to resolve {@code net.minecraft.client.gui.screens.Screen} at
 * mod load time.
 */
public final class NeoForgeClientBootstrap {
    private NeoForgeClientBootstrap() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        // Wire in the real client-connection check so that NetworkHandler
        // never needs net.minecraft.client.* in its own constant pool.
        NetworkHandler.setClientConnectionChecker(() -> {
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            return connection != null && connection.getConnection() != null;
        });
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> new ExtraPlayerConfigScreen(parent));
        NeoForgeClientEventBridge.register(modBus);
    }
}
