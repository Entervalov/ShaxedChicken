package org.entervalov.shaxedchicken.utils;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;

import java.util.List;
import java.util.function.Supplier;

public class DebugSystem {

    public static boolean SHOW_DRONE_DEBUG = false;
    public static boolean FREEZE_DRONES = false; // Флаг заморозки AI
    public static boolean SHOW_PATH_LINES = true;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new net.minecraft.util.ResourceLocation("shaxedchicken", "debug"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void registerCommand(CommandDispatcher<CommandSource> dispatcher) {
        // Команда DEBUG (клиентская)
        dispatcher.register(Commands.literal("shaxed:debug")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayerEntity) {
                        ServerPlayerEntity player = (ServerPlayerEntity) context.getSource().getEntity();
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ToggleDebugPacket());
                        context.getSource().sendSuccess(new StringTextComponent("Shaxed Debug: Toggled"), true);
                    }
                    return 1;
                }));

        // Команда STOP (серверная)
        dispatcher.register(Commands.literal("shaxed:stop")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    FREEZE_DRONES = !FREEZE_DRONES; // Переключаем на сервере

                    // Опционально: можно отправить пакет всем игрокам, чтобы они знали
                    String status = FREEZE_DRONES ? "FROZEN" : "ACTIVE";
                    context.getSource().sendSuccess(new StringTextComponent("All Drones are now: " + status), true);

                    // Если заморозили - останавливаем всех активных дронов
                    if (FREEZE_DRONES) {
                        assert context.getSource().getEntity() != null;
                        List<AdvancedDroneEntity> drones = context.getSource().getLevel().getEntitiesOfClass(AdvancedDroneEntity.class,
                                context.getSource().getEntity().getBoundingBox().inflate(200));
                        for (AdvancedDroneEntity drone : drones) {
                            drone.setDeltaMovement(0, 0, 0);
                            drone.getNavigation().stop();
                        }
                    }
                    return 1;
                }));
    }

    public static void registerPackets() {
        CHANNEL.registerMessage(0, ToggleDebugPacket.class,
                ToggleDebugPacket::encode, ToggleDebugPacket::decode, ToggleDebugPacket::handle);
    }

    public static class ToggleDebugPacket {
        public ToggleDebugPacket() {}
        public ToggleDebugPacket(PacketBuffer buffer) {}
        public void encode(PacketBuffer buffer) {}
        public static ToggleDebugPacket decode(PacketBuffer buffer) { return new ToggleDebugPacket(buffer); }
        public void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> SHOW_DRONE_DEBUG = !SHOW_DRONE_DEBUG);
            context.get().setPacketHandled(true);
        }
    }
}