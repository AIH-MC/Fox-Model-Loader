package com.elfmcys.yesstevemodel.util;

import net.minecraft.server.level.ServerPlayer;

public final class PlayerDataSaveBridge {

    private PlayerDataSaveBridge() {
    }

    public static void save(ServerPlayer player) {
        com.elfmcys.yesstevemodel.util.fabric.PlayerDataSaveBridgeImpl.save(player);
    }
}
