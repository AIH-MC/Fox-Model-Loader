package com.elfmcys.yesstevemodel.fabric;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.network.NetworkHandler;

public final class YesSteveModelFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YesSteveModel.init();
        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (!YesSteveModel.isAvailable()) return;
            if (trackedEntity instanceof ServerPlayer serverPlayer) {
                ModelInfoCapability.get(serverPlayer).ifPresent(c -> {
                    c.createSyncMessage(serverPlayer, false).ifPresent(m -> NetworkHandler.sendToClientPlayer(m, player));
                });
            }
        });
    }
}
