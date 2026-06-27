package com.elfmcys.yesstevemodel.neoforge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import rip.ysm.architectury.event.events.common.LifecycleEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import rip.ysm.api.config.ConfigRegistration;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;

@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelNeoForge {
    public YesSteveModelNeoForge(IEventBus modBus, ModContainer container) {
        ConfigRegistration.setContainer(container);
        NeoForgeCapabilityTypes.register(modBus);
        NeoForgeEventBridge.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            // IMPORTANT: do NOT inline any client-only references here.
            // Any Screen / IConfigScreenFactory reference inside this class
            // would put net/minecraft/client/gui/screens/Screen into the
            // @Mod entry class's constant pool and crash a dedicated server
            // at mod-load time. Keep them inside NeoForgeClientBootstrap.
            NeoForgeClientBootstrap.init(modBus, container);
        }
        YesSteveModel.init();
        NetworkHandler.init();
        LifecycleEvent.fireSetup();
        
        NeoForge.EVENT_BUS.addListener(YesSteveModelNeoForge::onPlayerStartTracking);
    }
    
    private static void onPlayerStartTracking(PlayerEvent.StartTracking event) {
        if (!YesSteveModel.isAvailable()) return;
        if (event.getTarget() instanceof ServerPlayer targetPlayer) {
            ServerPlayer tracker = (ServerPlayer) event.getEntity();
            ModelInfoCapability.get(targetPlayer).ifPresent(c -> {
                c.createSyncMessage(targetPlayer, false).ifPresent(m -> NetworkHandler.sendToClientPlayer(m, tracker));
            });
        }
    }
}
