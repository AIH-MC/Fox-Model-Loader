package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.*;
import com.elfmcys.yesstevemodel.util.PlayerModelSelectionStore;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class EnterServerEvent {
    private EnterServerEvent() {}
    public static void register() { NeoForge.EVENT_BUS.addListener(EnterServerEvent::onJoin); }
    private static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) return;
        if (!YesSteveModel.isAvailable()) return;
        NetworkHandler.sendToClientPlayer(new S2CVersionCheckPacket(), p);
        CapabilityEvent.getAuthModelsCap(p).ifPresent(c -> { for (String m : ServerModelManager.getAuthModels()) c.addModel(m); NetworkHandler.sendToClientPlayer(new S2CSyncAuthModelsPacket(c.getAuthModels()), p); });
        PlayerModelSelectionStore.restore(p); ServerModelManager.validatePlayerModel(p);
        CapabilityEvent.syncPlayerModelToSelf(p); CapabilityEvent.syncPlayerModelToTracking(p, false);
        CapabilityEvent.getStarModelsCap(p).ifPresent(c -> NetworkHandler.sendToClientPlayer(new S2CSyncStarModelsPacket(c.getStarModels()), p));
    }
}