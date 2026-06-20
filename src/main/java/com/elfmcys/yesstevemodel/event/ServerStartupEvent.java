package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

public final class ServerStartupEvent {
    private ServerStartupEvent() {}
    public static void register() { NeoForge.EVENT_BUS.addListener(ServerStartupEvent::onStarting); }
    private static void onStarting(ServerStartingEvent event) {
        if (!YesSteveModel.isAvailable()) return;
        ServerModelManager.loadModels(r -> { if (!r.isSuccess()) event.getServer().execute(() -> { throw new RuntimeException("YSM Loading Failed: " + r.getErrorMessage().getString(256)); }); }, null);
    }
}