package com.elfmcys.yesstevemodel.util;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.config.GeneralConfig;

public final class NetworkOnlineDebugLog {

    private NetworkOnlineDebugLog() {
    }

    public static boolean enabled() {
        return GeneralConfig.safeGet(GeneralConfig.NETWORK_ONLINE_DEBUG_LOG, false);
    }

    public static void info(String message, Object... args) {
        if (enabled()) {
            YesSteveModel.LOGGER.info("[YSM-NetDebug] " + message, args);
        }
    }

    public static void warn(String message, Object... args) {
        if (enabled()) {
            YesSteveModel.LOGGER.warn("[YSM-NetDebug] " + message, args);
        }
    }
}
