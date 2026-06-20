package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.*;
import com.elfmcys.yesstevemodel.client.event.AnimationLockEvent;
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
public final class AnimationRouletteKey {
    public static final KeyMapping KEY_ROULETTE = KeyMappingFactory.createInGameNone("key.yes_steve_model.roulette.desc", InputConstants.Type.KEYSYM, 90, "key.category.yes_steve_model");
    public static final KeyMapping KEY_LOCK = KeyMappingFactory.createInGameNone("key.yes_steve_model.lock.desc", InputConstants.Type.KEYSYM, 76, "key.category.yes_steve_model");
    private AnimationRouletteKey() {} public static void register() {}
    @SubscribeEvent public static void onKey(InputEvent.Key event) {
        if (PlatformAPI.isServer() || !InputUtil.isPlayerReady() || event.getAction() != 1 || !YesSteveModel.isAvailable()) return;
        if (InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_ROULETTE)) Minecraft.getInstance().setScreen(new AnimationRouletteScreen("", null, null));
        if (InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_LOCK)) AnimationLockEvent.toggleLock();
    }
}