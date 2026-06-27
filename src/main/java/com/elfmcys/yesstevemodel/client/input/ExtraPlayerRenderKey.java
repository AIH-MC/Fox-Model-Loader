package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
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
public final class ExtraPlayerRenderKey {
    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameNone("key.yes_steve_model.open_extra_player_render.desc", InputConstants.Type.KEYSYM, 80, "key.category.yes_steve_model");
    private ExtraPlayerRenderKey() {} public static void register() {}
    @SubscribeEvent public static void onKey(InputEvent.Key event) { if (!PlatformAPI.isServer() && InputUtil.isPlayerReady() && event.getAction() == 1 && InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_MAPPING) && YesSteveModel.isAvailable()) Minecraft.getInstance().setScreen(new ExtraPlayerRenderScreen()); }
}
