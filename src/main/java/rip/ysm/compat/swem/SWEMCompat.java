package rip.ysm.compat.swem;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;

public final class SWEMCompat {

    private SWEMCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static String getHorseGaitName(LivingEntity livingEntity) {
        return "";
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
    }
}
