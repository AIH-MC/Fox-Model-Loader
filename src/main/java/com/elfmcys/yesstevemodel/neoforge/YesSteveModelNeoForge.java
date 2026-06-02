package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import dev.architectury.event.events.common.LifecycleEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import rip.ysm.api.config.ConfigRegistration;

@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelNeoForge {
    public YesSteveModelNeoForge(IEventBus modBus, ModContainer container) {
        ConfigRegistration.setContainer(container);
        NeoForgeCapabilityTypes.register(modBus);
        NeoForgeEventBridge.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            NeoForgeClientEventBridge.register(modBus);
        }
        YesSteveModel.init();
        NetworkHandler.init();
        LifecycleEvent.fireSetup();
    }
}
