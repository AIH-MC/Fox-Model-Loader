package com.elfmcys.yesstevemodel;

import com.elfmcys.yesstevemodel.config.*;
import com.elfmcys.yesstevemodel.event.CommonEvent;
import com.elfmcys.yesstevemodel.event.YsmEventBootstrap;
import com.elfmcys.yesstevemodel.util.obfuscate.Keep;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.*;
import net.neoforged.fml.config.ModConfig;
import org.apache.logging.log4j.*;
import rip.ysm.api.PlatformAPI;
import rip.ysm.api.config.ConfigRegistration;
import java.io.IOException;

public class YesSteveModel {
    public static final String MOD_ID = "yes_steve_model";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private YesSteveModel() {}
    public static void init() {
        LOGGER.info("Initializing YesSteveModel, platform: " + PlatformAPI.getPlatformName());
        try { NativeLibLoader.init(); } catch (IOException e) { LOGGER.error("Failed to initialize native lib", e); }
        if (!NativeLibLoader.isAvailable()) LOGGER.error(getErrorMessage());
        CommonEvent.init();
        YsmEventBootstrap.register();
    }
    private static void initConfig() {
        java.io.File old = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("yes_steve_model-common.toml").toFile();
        if (old.isFile()) { java.io.File f2 = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("yes_steve_model-client.toml").toFile(); if (!f2.isFile()) old.renameTo(f2); else old.delete(); }
        ConfigRegistration.register(MOD_ID, ModConfig.Type.CLIENT, GeneralConfig.buildSpec());
        ConfigRegistration.register(MOD_ID, ModConfig.Type.SERVER, ServerConfig.buildSpec());
    }
    public static void registerModBusEvents(net.neoforged.bus.api.IEventBus bus) {
        ModSoundEvents.REGISTER.register(bus);
        com.elfmcys.yesstevemodel.neoforge.capability.NeoForgeCapabilities.register(bus);
    }
    @Keep public static boolean isAvailable() { return NativeLibLoader.isAvailable(); }
    public static boolean isOnAndroid() { return NativeLibLoader.isOnAndroid(); }
    @OnlyIn(Dist.CLIENT) public static void sendUnavailableMessage() { LocalPlayer p = Minecraft.getInstance().player; if (p != null) p.sendSystemMessage(getUnavailableComponent()); }
    public static Component getUnavailableComponent() { return NativeLibLoader.getErrorComponent(); }
    public static String getErrorMessage() { return NativeLibLoader.getErrorMessage(); }
}