package com.elfmcys.yesstevemodel.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class GeneralConfig {

    public static ForgeConfigSpec.BooleanValue DISCLAIMER_SHOW;

    public static ForgeConfigSpec.BooleanValue PRINT_ANIMATION_ROULETTE_MSG;

    public static ForgeConfigSpec.BooleanValue DISABLE_SELF_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_OTHER_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_SELF_HANDS;

    public static ForgeConfigSpec.BooleanValue DISABLE_PROJECTILE_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_VEHICLE_MODEL;

    public static ForgeConfigSpec.BooleanValue DISABLE_EXTERNAL_FP_ANIM;

    public static ForgeConfigSpec.BooleanValue USE_COMPATIBILITY_RENDERER;

    public static ForgeConfigSpec.DoubleValue SOUND_VOLUME;

    public static ForgeConfigSpec.BooleanValue SHOW_MODEL_ID_FIRST;

    public static ForgeConfigSpec.BooleanValue SOPHISTICATEDBACKPACK;

    public static ForgeConfigSpec.BooleanValue PARCOOL;

    public static ForgeConfigSpec.BooleanValue USE_GPU_RENDERER;

    public static ForgeConfigSpec.BooleanValue ANIMATION_DISTANCE_LOD;

    public static ForgeConfigSpec.EnumValue<RouletteSettingsMode> ROULETTE_SETTINGS_MODE;

    public static ForgeConfigSpec.EnumValue<RouletteMode> ROULETTE_MODE;

    public static ForgeConfigSpec.BooleanValue BLUR_GUI;

    public static ForgeConfigSpec.EnumValue<TextureScreenMode> TEXTURE_SCREEN_MODE;

    public static ForgeConfigSpec.EnumValue<ModelInfoScreenMode> MODEL_INFO_SCREEN_MODE;

    public static ForgeConfigSpec.BooleanValue ANIMATION_FRAME_PROFILER;

    public static ForgeConfigSpec.BooleanValue ANIMATION_DEBUG_LOG;

    public static ForgeConfigSpec.BooleanValue WARN_REPEATED_ANIMATION_EVALUATION;

    public static ForgeConfigSpec.BooleanValue RESOURCE_STATION_MONITOR_LOG;

    public static ForgeConfigSpec.BooleanValue NETWORK_ONLINE_DEBUG_LOG;

    public static ForgeConfigSpec.BooleanValue MODEL_MEMORY_PROFILER;

    public static ForgeConfigSpec.BooleanValue RELEASE_TEXTURE_BYTES_AFTER_UPLOAD;

    public static ForgeConfigSpec.IntValue MAX_CACHED_GPU_MODELS;

    public static ForgeConfigSpec.IntValue UNUSED_MODEL_TTL_SECONDS;

    public enum RouletteSettingsMode {
        MODERN,
        CLASSIC
    }

    public enum RouletteMode {
        MODERN,
        CLASSIC
    }

    public enum TextureScreenMode {
        MODERN,
        CLASSIC
    }

    public enum ModelInfoScreenMode {
        MODERN,
        CLASSIC
    }

    public static boolean effectiveModernRoulette() {
        if (ROULETTE_MODE == null || ROULETTE_SETTINGS_MODE == null) return false;
        return ROULETTE_MODE.get() == RouletteMode.MODERN && ROULETTE_SETTINGS_MODE.get() == RouletteSettingsMode.MODERN;
    }

    public static boolean safeGet(ForgeConfigSpec.BooleanValue value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return value.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int safeInt(ForgeConfigSpec.IntValue value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return value.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static ForgeConfigSpec buildSpec() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        defineGeneral(builder);
        ExtraPlayerRenderConfig.define(builder);
        LoadingStateConfig.define(builder);
        return builder.build();
    }

    public static void defineGeneral(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        builder.comment("Whether to display disclaimer GUI");
        DISCLAIMER_SHOW = builder.define("DisclaimerShow", false);
        builder.comment("Whether to print animation roulette play message");
        PRINT_ANIMATION_ROULETTE_MSG = builder.define("PrintAnimationRouletteMsg", false);
        builder.comment("Prevents rendering of self player's model");
        DISABLE_SELF_MODEL = builder.define("DisableSelfModel", false);
        builder.comment("Prevents rendering of other player's model");
        DISABLE_OTHER_MODEL = builder.define("DisableOtherModel", false);
        builder.comment("Prevents rendering of self player's hand");
        DISABLE_SELF_HANDS = builder.define("DisableSelfHands", false);
        builder.comment("Prevents rendering of projectile model");
        DISABLE_PROJECTILE_MODEL = builder.define("DisableProjectileModel", false);
        builder.comment("Prevents rendering of vehicle model");
        DISABLE_VEHICLE_MODEL = builder.define("DisableVehicleModel", false);
        builder.comment("Disable first person animation from other mods.");
        DISABLE_EXTERNAL_FP_ANIM = builder.define("DisableExternalFirstPersonAnim", false);
        builder.comment("If rendering errors occur, try turning on this.");
        USE_COMPATIBILITY_RENDERER = builder.define("UseCompatibilityRenderer", false);
        builder.comment("Test renderer.");
        USE_GPU_RENDERER = builder.define("UseGpuRenderer", true);
        ROULETTE_SETTINGS_MODE = builder.defineEnum("RouletteSettingsMode", RouletteSettingsMode.MODERN);
        ROULETTE_MODE = builder.defineEnum("RouletteMode", RouletteMode.CLASSIC);
        BLUR_GUI = builder.define("BlurGui", true);
        TEXTURE_SCREEN_MODE = builder.defineEnum("TextureScreenMode", TextureScreenMode.MODERN);
        MODEL_INFO_SCREEN_MODE = builder.defineEnum("ModelInfoScreenMode", ModelInfoScreenMode.MODERN);
        builder.comment("The amount of volume when the animation is played.");
        SOUND_VOLUME = builder.defineInRange("SoundVolume", 100.0d, 0.0d, 100.0d);
        builder.comment("Whether to display model ID first in the model selection screen, instead of the model name filled in by the model author.");
        SHOW_MODEL_ID_FIRST = builder.define("ShowModelIdFirst", false);
        builder.pop();
        builder.push("Integration");
        SOPHISTICATEDBACKPACK = builder.define("SophisticatedBackpack", true);
        PARCOOL = builder.define("Parcool", true);
        builder.pop();
        builder.push("ExperimentalTesting");
        builder.comment("Per-frame animation profiler for diagnosing animation stutter. Default off.");
        ANIMATION_FRAME_PROFILER = builder.define("AnimationFrameProfiler", false);
        builder.comment("Verbose per-evaluation [YSM-ANIM] debug log. Very noisy, default off.");
        ANIMATION_DEBUG_LOG = builder.define("AnimationDebugLog", false);
        builder.comment("Warn when the same entity is fully evaluated more than once in a single render frame.");
        WARN_REPEATED_ANIMATION_EVALUATION = builder.define("WarnRepeatedAnimationEvaluation", true);
        builder.comment("Reduce animation update rates for distant entities. Disabled by default for smoother animation.");
        ANIMATION_DISTANCE_LOD = builder.define("AnimationDistanceLod", false);
        builder.comment("Verbose resource station network/probe logging. Default off.");
        RESOURCE_STATION_MONITOR_LOG = builder.define("ResourceStationMonitorLog", false);
        builder.comment("Verbose client/server online model sync diagnostics. Default off.");
        NETWORK_ONLINE_DEBUG_LOG = builder.define("NetworkOnlineDebugLog", false);
        builder.comment("Model memory profiler for read/decrypt/parse/texture/GPU/LRU checkpoints. Default off.");
        MODEL_MEMORY_PROFILER = builder.define("ModelMemoryProfiler", false);
        builder.comment("Release Java texture byte arrays after successful GPU upload. Default off.");
        RELEASE_TEXTURE_BYTES_AFTER_UPLOAD = builder.define("ReleaseTextureBytesAfterUpload", false);
        builder.comment("Maximum models whose GPU/native render caches stay resident. 0 disables LRU trimming.");
        MAX_CACHED_GPU_MODELS = builder.defineInRange("MaxCachedGpuModels", 0, 0, 512);
        builder.comment("Minimum idle time before GPU/native render caches can be trimmed.");
        UNUSED_MODEL_TTL_SECONDS = builder.defineInRange("UnusedModelTtlSeconds", 300, 30, 86400);
        builder.pop();
    }
}
