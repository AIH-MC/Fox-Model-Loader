package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import rip.ysm.api.network.neoforge.YSMChannelImpl;

@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelNeoForge {

    public YesSteveModelNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // Register configs in constructor so they're available before any events fire
        modContainer.registerConfig(ModConfig.Type.CLIENT, GeneralConfig.buildSpec());
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.buildSpec());

        // Register config screen so the Settings button in the Mods menu works.
        // IMPORTANT: do NOT inline any client-only references here. Any Screen /
        // IConfigScreenFactory reference inside this class would put
        // net/minecraft/client/gui/screens/Screen into the @Mod entry class's
        // constant pool and crash a dedicated server at mod-load time. Keep
        // them inside NeoForgeClientBootstrap.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForgeClientBootstrap.init(modContainer);
        }

        // Init network channel and register packets early (before any player join events)
        YSMChannelImpl.init(NetworkHandler.CHANNEL_ID, NetworkHandler.VERSION);
        NetworkHandler.init();

        YesSteveModel.registerModBusEvents(modEventBus);
        modEventBus.addListener(YSMChannelImpl::registerPayloadHandlers);
        modEventBus.addListener(YesSteveModelNeoForge::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(YesSteveModel::init);
    }
}
