package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.config.LoadingStateConfig;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import rip.ysm.gui.Option;
import rip.ysm.gui.OptionGroup;
import rip.ysm.gui.OptionScreen;
import rip.ysm.gui.components.BooleanOptionRow;
import rip.ysm.gui.components.EnumOptionRow;
import rip.ysm.gui.components.SliderOptionRow;

public class ExtraPlayerConfigScreen extends OptionScreen {

    public ExtraPlayerConfigScreen(@Nullable PlayerModelScreen modelScreen) {
        super(Component.literal("OpenYSM"), modelScreen);
    }

    @Override
    protected void registerGroups() {
        OptionGroup general = new OptionGroup("general")
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofDouble("sound_volume", GeneralConfig.SOUND_VOLUME), 0.0d, 100.0d, 1.0d, "%"))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_self_model", GeneralConfig.DISABLE_SELF_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_other_model", GeneralConfig.DISABLE_OTHER_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_self_hands", GeneralConfig.DISABLE_SELF_HANDS)));

        OptionGroup rendering = new OptionGroup("rendering")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_player_render", ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_projectile_model", GeneralConfig.DISABLE_PROJECTILE_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_vehicle_model", GeneralConfig.DISABLE_VEHICLE_MODEL)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_external_first_person_anim", GeneralConfig.DISABLE_EXTERNAL_FP_ANIM)));

        OptionGroup performance = new OptionGroup("performance")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("use_compatibility_renderer", GeneralConfig.USE_COMPATIBILITY_RENDERER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("use_gpu_renderer", GeneralConfig.USE_GPU_RENDERER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("release_texture_bytes_after_upload", GeneralConfig.RELEASE_TEXTURE_BYTES_AFTER_UPLOAD)))
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofInt("max_cached_gpu_models", GeneralConfig.MAX_CACHED_GPU_MODELS), 0.0d, 512.0d, 1.0d, ""))
                .add(new SliderOptionRow(0, 0, 0, 22, Option.ofInt("unused_model_ttl_seconds", GeneralConfig.UNUSED_MODEL_TTL_SECONDS), 30.0d, 86400.0d, 30.0d, "s"));

        OptionGroup debug = new OptionGroup("debug")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("animation_frame_profiler", GeneralConfig.ANIMATION_FRAME_PROFILER)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("animation_debug_log", GeneralConfig.ANIMATION_DEBUG_LOG)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("warn_repeated_animation_evaluation", GeneralConfig.WARN_REPEATED_ANIMATION_EVALUATION)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("animation_distance_lod", GeneralConfig.ANIMATION_DISTANCE_LOD)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("network_online_debug_log", GeneralConfig.NETWORK_ONLINE_DEBUG_LOG)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("model_memory_profiler", GeneralConfig.MODEL_MEMORY_PROFILER)));

        OptionGroup misc = new OptionGroup("misc")
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("print_animation_roulette_msg", GeneralConfig.PRINT_ANIMATION_ROULETTE_MSG)))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("disable_loading_state_screen", LoadingStateConfig.DISABLE_LOADING_STATE_SCREEN)))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("loading_state_position", LoadingStateConfig.LOADING_STATE_POSITION), LoadingStateConfig.Position.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("roulette_settings_mode", GeneralConfig.ROULETTE_SETTINGS_MODE), GeneralConfig.RouletteSettingsMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("roulette_mode", GeneralConfig.ROULETTE_MODE), GeneralConfig.RouletteMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("texture_screen_mode", GeneralConfig.TEXTURE_SCREEN_MODE), GeneralConfig.TextureScreenMode.values()))
                .add(new EnumOptionRow<>(0, 0, 0, 22, Option.ofEnum("model_info_screen_mode", GeneralConfig.MODEL_INFO_SCREEN_MODE), GeneralConfig.ModelInfoScreenMode.values()))
                .add(new BooleanOptionRow(0, 0, 0, 22, Option.ofBoolean("blur_gui", GeneralConfig.BLUR_GUI)));

        groups.add(general);
        groups.add(rendering);
        groups.add(performance);
        groups.add(debug);
        groups.add(misc);
    }
}
