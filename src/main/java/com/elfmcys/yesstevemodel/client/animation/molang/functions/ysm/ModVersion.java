package com.elfmcys.yesstevemodel.client.animation.molang.functions.ysm;

import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import com.elfmcys.yesstevemodel.molang.runtime.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModVersion implements Function {
    @Override
    @Nullable
    public Object evaluate(@NotNull ExecutionContext<?> context, @NotNull Function.ArgumentCollection arguments) {
        String modid = arguments.getAsString(context, 0);
        if (modid == null) { return null; } return "1.3.0";
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}