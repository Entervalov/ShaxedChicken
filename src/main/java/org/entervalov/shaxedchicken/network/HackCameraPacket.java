package org.entervalov.shaxedchicken.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class HackCameraPacket {
    private final int entityId;
    private final boolean enable;

    public HackCameraPacket(int entityId, boolean enable) {
        this.entityId = entityId;
        this.enable = enable;
    }

    public static void encode(HackCameraPacket msg, PacketBuffer buffer) {
        buffer.writeInt(msg.entityId);
        buffer.writeBoolean(msg.enable);
    }

    public static HackCameraPacket decode(PacketBuffer buffer) {
        return new HackCameraPacket(buffer.readInt(), buffer.readBoolean());
    }

    public static void handle(HackCameraPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                org.entervalov.shaxedchicken.client.HackCameraClient.setDroneCamera(msg.entityId, msg.enable)
        ));
        ctx.setPacketHandled(true);
    }
}
