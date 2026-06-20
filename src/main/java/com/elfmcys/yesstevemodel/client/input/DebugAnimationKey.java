package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.renderer.AnimationDebugOverlay;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class DebugAnimationKey {
    public static final KeyMapping KEY_MAPPING = KeyMappingFactory.createInGameNone("key.yes_steve_model.debug_animation.desc", InputConstants.Type.KEYSYM, 66, "key.category.yes_steve_model");
    private DebugAnimationKey() {} public static void register() {}
    @SubscribeEvent public static void onKey(InputEvent.Key event) { if (!PlatformAPI.isServer() && InputUtil.isPlayerReady() && event.getAction() == 1 && InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_MAPPING) && YesSteveModel.isAvailable()) AnimationDebugOverlay.toggle(); }
}