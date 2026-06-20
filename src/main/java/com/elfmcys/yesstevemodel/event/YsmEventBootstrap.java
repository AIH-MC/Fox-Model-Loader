package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.client.event.*;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import rip.ysm.api.PlatformAPI;

public final class YsmEventBootstrap {
    private YsmEventBootstrap() {}
    public static void register() {
        ServerStartupEvent.register(); EnterServerEvent.register(); PlayerLogoutEvent.register();
        CommandRegistry.register(); CapabilityEvent.register(); LivingEventBridge.register();
        if (!PlatformAPI.isServer()) { EntityJoinCallbackEvent.register(); PlayerSkinTextureManager.register(); RendererManager.register(); }
    }
}