package org.entervalov.shaxedchicken;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class ModCommonEvents {

    private static int tickCounter = 0;
    private static final Random random = new Random();

    private static final int SPAWN_INTERVAL = 1200;
    private static final int MAX_DRONES_NEAR_PLAYER = 2;
    private static final double CHECK_RANGE = 100.0;
    private static final double SPAWN_MIN_DISTANCE = 40.0;
    private static final double SPAWN_MAX_DISTANCE = 80.0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.world instanceof ServerWorld)) return;

        ServerWorld world = (ServerWorld) event.world;

        if (world.dimension() != World.OVERWORLD) return;

        tickCounter++;
        if (tickCounter % SPAWN_INTERVAL != 0) return;

        List<ServerPlayerEntity> players = world.players();
        if (players.isEmpty()) return;

        ServerPlayerEntity player = players.get(random.nextInt(players.size()));

        if (random.nextFloat() > 0.15f) return;

        List<AdvancedDroneEntity> nearbyDrones = world.getEntitiesOfClass(
                AdvancedDroneEntity.class,
                new AxisAlignedBB(player.blockPosition()).inflate(CHECK_RANGE)
        );

        if (nearbyDrones.size() >= MAX_DRONES_NEAR_PLAYER) {
            return;
        }

        BlockPos spawnPos = calculateSpawnPosition(player.blockPosition());
        if (isValidSpawnLocation(world, spawnPos)) {
            spawnDrone(world, spawnPos);
        }
    }

    private static BlockPos calculateSpawnPosition(BlockPos playerPos) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = SPAWN_MIN_DISTANCE + random.nextDouble() *
                (SPAWN_MAX_DISTANCE - SPAWN_MIN_DISTANCE);

        int x = playerPos.getX() + (int)(Math.cos(angle) * distance);
        int z = playerPos.getZ() + (int)(Math.sin(angle) * distance);
        int y = playerPos.getY() + 25 + random.nextInt(25);

        return new BlockPos(x, y, z);
    }

    private static boolean isValidSpawnLocation(ServerWorld world, BlockPos pos) {
        return pos.getY() > 60 &&
                pos.getY() < 180 &&
                world.isEmptyBlock(pos) &&
                world.isEmptyBlock(pos.above());
    }

    private static void spawnDrone(ServerWorld world, BlockPos pos) {
        AdvancedDroneEntity drone = new AdvancedDroneEntity(
                ModEntityTypes.DRONE_CHICKEN.get(),
                world
        );

        DroneType type = DroneType.getRandomType(random);
        drone.setDroneType(type);

        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                random.nextFloat() * 360, 0);

        world.addFreshEntity(drone);
    }
}
