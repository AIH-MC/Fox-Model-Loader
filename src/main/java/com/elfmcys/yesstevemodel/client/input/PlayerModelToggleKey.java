package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.*;
import com.elfmcys.yesstevemodel.config.ServerConfig;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class PlayerModelToggleKey {
    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameAlt("key.yes_steve_model.player_model.desc", InputConstants.Type.KEYSYM, 89, "key.category.yes_steve_model");
    private PlayerModelToggleKey() {}
    @SubscribeEvent public static void onKey(InputEvent.Key event) {
        if (PlatformAPI.isServer()) return;
        if (InputUtil.isPlayerReady() && event.getAction() == 1 && InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_MAPPING)) {
            if (!YesSteveModel.isAvailable()) { YesSteveModel.sendUnavailableMessage(); return; }
            if (NetworkHandler.isClientConnected() && !ServerConfig.CAN_SWITCH_MODEL.get()) Minecraft.getInstance().setScreen(new ExtraPlayerConfigScreen(null));
            else Minecraft.getInstance().setScreen(new PlayerModelScreen());
        }
    }
}