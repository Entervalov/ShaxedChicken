package org.entervalov.shaxedchicken.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;

import java.util.function.Supplier;

public class DroneInputPacket {
    private final int droneId;
    private final boolean attack;
    private final boolean exit;
    private final float yRot;
    private final float xRot;
    private final float forward;
    private final float strafe;
    private final boolean jump;
    private final boolean sneak;

    public DroneInputPacket(int droneId, boolean attack, boolean exit, float yRot, float xRot, float forward, float strafe, boolean jump, boolean sneak) {
        this.droneId = droneId;
        this.attack = attack;
        this.exit = exit;
        this.yRot = yRot;
        this.xRot = xRot;
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
    }

    public static void encode(DroneInputPacket msg, PacketBuffer buf) {
        buf.writeInt(msg.droneId);
        buf.writeBoolean(msg.attack);
        buf.writeBoolean(msg.exit);
        buf.writeFloat(msg.yRot);
        buf.writeFloat(msg.xRot);
        buf.writeFloat(msg.forward);
        buf.writeFloat(msg.strafe);
        buf.writeBoolean(msg.jump);
        buf.writeBoolean(msg.sneak);
    }

    public static DroneInputPacket decode(PacketBuffer buf) {
        return new DroneInputPacket(
                buf.readInt(), buf.readBoolean(), buf.readBoolean(),
                buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean()
        );
    }

    public static void handle(DroneInputPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayerEntity sender = ctx.getSender();
            if (sender == null) {
                return;
            }

            Entity entity = sender.getLevel().getEntity(msg.droneId);
            if (entity instanceof AdvancedDroneEntity) {
                AdvancedDroneEntity drone = (AdvancedDroneEntity) entity;
                if (drone.isHackedBy(sender.getUUID())) {
                    drone.handleInput(msg.attack, msg.exit, msg.yRot, msg.xRot, msg.forward, msg.strafe, msg.jump, msg.sneak);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
