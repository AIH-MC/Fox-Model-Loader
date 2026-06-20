package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerLogoutEvent {
    private PlayerLogoutEvent() {}
    public static void register() { NeoForge.EVENT_BUS.addListener(PlayerLogoutEvent::onQuit); }
    private static void onQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (!YesSteveModel.isAvailable()) return;
        if (NetworkHandler.isPlayerConnected(p)) ServerModelManager.syncModelToPlayer(p.getUUID());
        ModelInfoCapability.get(p).ifPresent(c -> PlayerModelSelectionStore.saveCurrentSelection(p, c));
        PlayerDataSaveBridge.save(p);
    }
}