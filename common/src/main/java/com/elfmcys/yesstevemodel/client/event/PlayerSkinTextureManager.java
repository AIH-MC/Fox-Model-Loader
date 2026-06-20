package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import rip.ysm.architectury.event.EventResult;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.PlatformAPI;

public class PlayerSkinTextureManager {

    private PlayerSkinTextureManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        SpecialPlayerRenderEvent.EVENT.register(PlayerSkinTextureManager::onRenderTexture);
    }

    private static EventResult onRenderTexture(SpecialPlayerRenderEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return EventResult.pass();
        }
        // Skin models (misc/1_alex, misc/2_steve) have been removed.
        // The remaining built-in models have their own textures and should not be overridden.
        return EventResult.pass();
    }
}
