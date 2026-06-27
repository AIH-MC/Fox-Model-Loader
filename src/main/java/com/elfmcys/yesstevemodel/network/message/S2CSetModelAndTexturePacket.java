package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import rip.ysm.api.network.PacketContext;
import com.elfmcys.yesstevemodel.event.EntityJoinCallbackEvent;

public class S2CSetModelAndTexturePacket {

    final int entityId;

    final String modelId;

    final String textureId;

    final boolean disabled;

    final S2CSyncPlayerStatePacket entityModelSync;

    public S2CSetModelAndTexturePacket(int entityId, String modelId, String textureId, boolean disabled, S2CSyncPlayerStatePacket playerState) {
        this.entityId = entityId;
        this.modelId = modelId;
        this.textureId = textureId;
        this.entityModelSync = playerState;
        this.disabled = disabled;
    }

    public static void encode(S2CSetModelAndTexturePacket other, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(other.entityId);
        friendlyByteBuf.writeUtf(other.modelId);
        friendlyByteBuf.writeUtf(other.textureId);
        friendlyByteBuf.writeBoolean(other.disabled);
        S2CSyncPlayerStatePacket.encode(other.entityModelSync, friendlyByteBuf);
    }

    public static S2CSetModelAndTexturePacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new S2CSetModelAndTexturePacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readUtf(), friendlyByteBuf.readUtf(), friendlyByteBuf.readBoolean(), S2CSyncPlayerStatePacket.decode(friendlyByteBuf));
    }

    public static void handle(S2CSetModelAndTexturePacket other, PacketContext ctx) {
        if (ctx.isClientSide()) {
            EntityJoinCallbackEvent.addCallback(other.entityId, entity -> ClientPacketHandler.handleSetModelAndTexture(entity, other));
        }
    }
}
