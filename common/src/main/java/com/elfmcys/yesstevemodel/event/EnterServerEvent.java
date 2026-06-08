package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CSyncAuthModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncStarModelsPacket;
import com.elfmcys.yesstevemodel.network.message.S2CVersionCheckPacket;
import com.elfmcys.yesstevemodel.util.PlayerModelSelectionStore;
import dev.architectury.event.events.common.PlayerEvent;

public final class EnterServerEvent {

    private EnterServerEvent() {
    }

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!YesSteveModel.isAvailable()) {
                return;
            }
            NetworkHandler.sendToClientPlayer(new S2CVersionCheckPacket(), player);
            CapabilityEvent.getAuthModelsCap(player).ifPresent(authModelsCap -> {
                for (String modelId : ServerModelManager.getAuthModels()) {
                    authModelsCap.addModel(modelId);
                }
                NetworkHandler.sendToClientPlayer(new S2CSyncAuthModelsPacket(authModelsCap.getAuthModels()), player);
            });
            PlayerModelSelectionStore.restore(player);
            ServerModelManager.validatePlayerModel(player);
            CapabilityEvent.syncPlayerModelToSelf(player);
            CapabilityEvent.syncPlayerModelToTracking(player, false);
            CapabilityEvent.getStarModelsCap(player).ifPresent(starModelsCap -> NetworkHandler.sendToClientPlayer(new S2CSyncStarModelsPacket(starModelsCap.getStarModels()), player));
        });
    }
}
