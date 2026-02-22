package org.entervalov.shaxedchicken.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static boolean packetsRegistered = false;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("shaxedchicken", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerPackets() {
        if (packetsRegistered) {
            return;
        }
        packetsRegistered = true;

        int id = 0;

        CHANNEL.messageBuilder(HackCameraPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HackCameraPacket::encode)
                .decoder(HackCameraPacket::decode)
                .consumer(HackCameraPacket::handle)
                .add();

        CHANNEL.messageBuilder(DroneInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DroneInputPacket::encode)
                .decoder(DroneInputPacket::decode)
                .consumer(DroneInputPacket::handle)
                .add();
    }
}
