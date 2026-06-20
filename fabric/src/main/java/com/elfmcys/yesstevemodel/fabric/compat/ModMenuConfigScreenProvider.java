package com.elfmcys.yesstevemodel.fabric.compat;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuConfigScreenProvider implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> new ExtraPlayerConfigScreen(null);
    }
}
