package com.elfmcys.yesstevemodel.network.message;

import com.elfmcys.yesstevemodel.model.ServerModelManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import rip.ysm.api.network.PacketContext;

public class S2CVersionCheckPacket {

    final String version;
    final boolean oysmServer;
    final boolean allowUpload;

    public S2CVersionCheckPacket() {
        this(NetworkHandler.VERSION, true, ServerModelManager.isModelUploadAllowed());
    }

    private S2CVersionCheckPacket(String version, boolean oysmServer, boolean allowUpload) {
        this.version = version;
        this.oysmServer = oysmServer;
        this.allowUpload = allowUpload;
    }

    public static S2CVersionCheckPacket decode(FriendlyByteBuf buf) {
        String version = buf.readUtf();
        boolean oysmServer = NetworkHandler.VERSION.equals(version);
        boolean allowUpload = oysmServer;
        if (buf.readableBytes() == 1) {
            oysmServer = true;
            allowUpload = buf.readBoolean();
        } else if (buf.readableBytes() > 1) {
            int readerIndex = buf.readerIndex();
            try {
                String brand = buf.readUtf();
                if ("open_ysm:v1".equals(brand)) {
                    oysmServer = true;
                    if (buf.readableBytes() > 0) {
                        allowUpload = buf.readBoolean();
                    }
                }
            } catch (RuntimeException ignored) {
                buf.readerIndex(readerIndex);
                if (buf.readableBytes() > 0) {
                    oysmServer = true;
                    allowUpload = buf.readBoolean();
                }
            }
        }
        return new S2CVersionCheckPacket(version, oysmServer, allowUpload);
    }

    public static void encode(S2CVersionCheckPacket message, FriendlyByteBuf buf) {
        buf.writeUtf(message.version);
        buf.writeUtf("open_ysm:v1");
        buf.writeBoolean(message.allowUpload);
    }

    public static void handle(S2CVersionCheckPacket message, PacketContext ctx) {
        ClientPacketHandler.handleVersionCheck(message, ctx);
    }
}
