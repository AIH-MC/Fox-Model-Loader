package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.gui.*;
import com.elfmcys.yesstevemodel.client.event.AnimationLockEvent;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.util.InputUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.client.KeyMappingFactory;

@EventBusSubscriber(modid = YesSteveModel.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class AnimationRouletteKey {
    public static final KeyMapping KEY_ROULETTE = KeyMappingFactory.createInGameNone("key.yes_steve_model.animation_roulette.desc", InputConstants.Type.KEYSYM, 90, "key.category.yes_steve_model");
    public static final KeyMapping KEY_LOCK = KeyMappingFactory.createInGameNone("key.yes_steve_model.lock_roulette.desc", InputConstants.Type.KEYSYM, 76, "key.category.yes_steve_model");
    private AnimationRouletteKey() {} public static void register() {}
    @SubscribeEvent public static void onKey(InputEvent.Key event) {
        if (PlatformAPI.isServer() || !InputUtil.isPlayerReady() || event.getAction() != 1 || !YesSteveModel.isAvailable()) return;
        if (InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_ROULETTE)) openRoulette();
        if (InputUtil.isKeyPressed(event.getKey(), event.getScanCode(), KEY_LOCK)) AnimationLockEvent.toggleLock();
    }

    private static void openRoulette() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        PlayerCapability.get(player).ifPresent(cap -> {
            String modelId = cap.getModelId();
            ModelAssembly modelAssembly = cap.getModelAssembly();
            if (modelAssembly != null && !modelAssembly.getModelData().getModelProperties().getExtraAnimation().isEmpty()) {
                minecraft.setScreen(new AnimationRouletteScreen(modelId, modelAssembly, cap));
            }
        });
    }
}