package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import java.io.IOException;

public final class CommonEvent {
    private CommonEvent() {}
    public static void register() {}
    public static void init() {
        if (!YesSteveModel.isAvailable()) { YesSteveModel.LOGGER.error(YesSteveModel.getErrorMessage()); return; }
        NetworkHandler.init(); TouhouMaidCompat.init();
        try { ServerModelManager.reloadPacks(); } catch (IOException e) { throw new RuntimeException(e); }
    }
}