package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import com.elfmcys.yesstevemodel.event.EntityJoinCallbackEvent;
import com.elfmcys.yesstevemodel.geckolib3.resource.GeckoLibCache;
import com.elfmcys.yesstevemodel.molang.parser.ParseException;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.network.PacketContext;
import rip.ysm.compat.touhoulittlemaid.TouhouMaidCompat;

public class ClientPacketHandler {

    public static void handleExecuteMolang(S2CExecuteMolangPacket message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (int i : message.entityIds) {
            Entity entity = minecraft.level.getEntity(i);
            if (entity instanceof Player) {
                PlayerCapability.get(entity).ifPresent(cap -> {
                    try {
                        cap.executeExpression(GeckoLibCache.parseSimpleExpression(message.expression), true, false, null);
                    } catch (ParseException e) {
                        YesSteveModel.LOGGER.error("Failed to execute molang " + message.expression, e);
                    }
                });
            } else if (TouhouMaidCompat.isMaidEntity(entity)) {
                TouhouMaidCompat.playMaidAnimation(entity, message.expression);
            }
        }
    }

    public static void handleModelSyncPayload(S2CModelSyncPayload message, PacketContext ctx) {
        ClientModelManager.startSync(ctx.getConnection(), message.data);
    }

    public static void handleModelUploadResult(S2CModelUploadResultPacket packet) {
        ModelUploadSession.onResult(packet.uploadId(), packet.status(), packet.modelId(), packet.h1(), packet.h2(), packet.message());
    }

    public static void handleModelUploadStart(S2CModelUploadStartPacket packet) {
        ModelUploadSession.onStartAck(packet.uploadId(), packet.status(), packet.chunkSize(), packet.maxTotalBytes(), packet.chunksPerTick(), packet.message());
    }

    public static void handleSetModelAndTexture(Entity entity, S2CSetModelAndTexturePacket other) {
        PlayerCapability.get(entity).ifPresent(cap -> {
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            boolean keepLocalOnlyModel = entity == localPlayer && ClientModelManager.isSelectedLocalOnlyModel(cap.getModelId());
            if (!keepLocalOnlyModel) {
                cap.initModelWithTexture(other.modelId, other.textureId);
            }
            cap.setForceDisabled(other.disabled);
            S2CSyncPlayerStatePacket.handleCapability(entity, other.entityModelSync);
        });
    }

    public static void handleSyncAnimationExpression(S2CSyncAnimationExpressionPacket message) {
        Entity entity = Minecraft.getInstance().level.getEntity(message.entityId);
        if (entity != null) {
            PlayerCapability.get(entity).ifPresent(cap -> cap.executeAnimationExpression(message.floatData));
        }
    }

    public static void handleSyncAuthModels(S2CSyncAuthModelsPacket message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            AuthModelsCapability.get(minecraft.player).ifPresent(cap -> {
                cap.setAuthModels(message.authModels);
            });
        }
    }

    public static void handleSyncStarModels(S2CSyncStarModelsPacket message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            StarModelsCapability.get(minecraft.player).ifPresent(cap -> cap.setStarModels(message.starModels));
        }
    }

    public static void handleVersionCheck(S2CVersionCheckPacket message, PacketContext ctx) {
        ClientModelManager.setOysmServer(message.oysmServer);
        ClientModelManager.setAllowUpload(message.allowUpload);
        if (NetworkHandler.setChannelVersion(ctx.getConnection(), message.version)) {
            ctx.enqueueWork(ClientModelManager::onSyncConnected);
        }
        if (NetworkHandler.VERSION.equals(message.version)) {
            NetworkHandler.markClientHandshakeComplete();
        }
        ctx.enqueueWork(() -> NetworkHandler.sendToServer(new C2SVersionCheckPacket()));
    }
}
