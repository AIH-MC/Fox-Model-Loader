package com.elfmcys.yesstevemodel.client.input;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.util.ItemTagsConstants;
import com.elfmcys.yesstevemodel.util.InputUtil;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rip.ysm.api.PlatformAPI;

public class InputStateKey {

    public static volatile boolean[] keyStates = new boolean[349];

    public static volatile boolean[] mouseStates = new boolean[8];

    private InputStateKey() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ClientRawInputEvent.KEY_PRESSED.register((client, keyCode, scanCode, action, modifiers) -> {
            onKeyInput(keyCode, action);
            return EventResult.pass();
        });
        ClientRawInputEvent.MOUSE_CLICKED_PRE.register((client, button, action, mods) -> {
            onMouseInput(button, action);
            return EventResult.pass();
        });
    }

    private static void onKeyInput(int keyCode, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 32 <= keyCode && keyCode <= 348) {
            if (action == 1) {
                keyStates[keyCode] = true;
            } else if (action == 0) {
                keyStates[keyCode] = false;
            }
        }
    }

    private static void onMouseInput(int button, int action) {
        if (YesSteveModel.isAvailable() && InputUtil.isPlayerReady() && 0 <= button && button <= 7) {
            if (action == 1) {
                mouseStates[button] = true;
            } else if (action == 0) {
                mouseStates[button] = false;
            }
            triggerHandAnimation(button, action);
        }
    }

    private static void triggerHandAnimation(int button, int action) {
        if (action != 1 || (button != 0 && button != 1)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (button == 1 && shouldSkipSyntheticMainHandRightClickSwing(player)) {
            return;
        }
        player.swing(InteractionHand.MAIN_HAND, false);
    }

    private static boolean shouldSkipSyntheticMainHandRightClickSwing(LocalPlayer player) {
        ItemStack offhandItem = player.getOffhandItem();
        if (offhandItem.isEmpty()) {
            return false;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        return isRightClickFallbackToolOrWeapon(mainHandItem);
    }

    private static boolean isRightClickFallbackToolOrWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTagsConstants.SWORDS)
                || stack.is(ItemTagsConstants.AXES)
                || stack.is(ItemTagsConstants.PICKAXES)
                || stack.is(ItemTagsConstants.SHOVELS)
                || stack.is(ItemTagsConstants.HOES)
                || stack.is(ItemTagsConstants.TRIDENTS)
                || stack.is(ItemTagsConstants.LANCES)
                || stack.is(ItemTagsConstants.MACE)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }
}
