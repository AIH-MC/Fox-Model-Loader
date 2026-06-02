package rip.ysm.compat.playeranimator;

import net.minecraft.client.player.AbstractClientPlayer;

public final class PlayerAnimatorCompat {

    private PlayerAnimatorCompat() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isPlayerAnimated(AbstractClientPlayer abstractClientPlayer) {
        return false;
    }
}
