package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.NetworkOnlineDebugLog;
import com.elfmcys.yesstevemodel.util.PlayerDataSaveBridge;
import com.elfmcys.yesstevemodel.util.PlayerModelSelectionStore;
import dev.architectury.event.events.common.PlayerEvent;

public final class PlayerLogoutEvent {

    private PlayerLogoutEvent() {
    }

    public static void register() {
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            if (NetworkHandler.isPlayerConnected(player)) {
                ServerModelManager.syncModelToPlayer(player.getUUID());
            }
            ModelInfoCapability.get(player).ifPresent(cap -> PlayerModelSelectionStore.saveCurrentSelection(player, cap));
            PlayerDataSaveBridge.save(player);
            NetworkOnlineDebugLog.info("Forced player data save on logout: {}", player.getName().getString());
        });
    }
}
