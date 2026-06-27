package com.elfmcys.yesstevemodel.network.message;

import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CExecuteMolangPacket {

    final int[] entityIds;

    final String expression;

    public S2CExecuteMolangPacket(int entityIds, String expression) {
        this.entityIds = new int[]{entityIds};
        this.expression = expression;
    }

    public S2CExecuteMolangPacket(int[] entityIds, String expression) {
        this.entityIds = entityIds;
        this.expression = expression;
    }

    public static void encode(S2CExecuteMolangPacket message, FriendlyByteBuf buf) {
        buf.writeVarIntArray(message.entityIds);
        buf.writeUtf(message.expression);
    }

    public static S2CExecuteMolangPacket decode(FriendlyByteBuf buf) {
        return new S2CExecuteMolangPacket(buf.readVarIntArray(), buf.readUtf());
    }

    public static void handle(S2CExecuteMolangPacket message, PacketContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> ClientPacketHandler.handleExecuteMolang(message));
        }
    }
}