package org.entervalov.shaxedchicken;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class DroneSpawnHandler {

    @SubscribeEvent
    public static void onEntitySpawn(LivingSpawnEvent.SpecialSpawn event) {
        if (!(event.getEntity() instanceof AdvancedDroneEntity)) return;

        AdvancedDroneEntity drone = (AdvancedDroneEntity) event.getEntity();
        Random random = new Random();

        Biome biome = event.getWorld().getBiome(drone.blockPosition());
        Biome.Category category = biome.getBiomeCategory();
        DroneType selectedType;

        if (category == Biome.Category.NETHER) {
            int roll = random.nextInt(100);
            if (roll < 40) selectedType = DroneType.FIRE;
            else if (roll < 60) selectedType = DroneType.HEAVY;
            else selectedType = DroneType.getRandomType(random);
        } else if (category == Biome.Category.THEEND) {
            int roll = random.nextInt(100);
            if (roll < 15) selectedType = DroneType.NUCLEAR;
            else if (roll < 40) selectedType = DroneType.HEAVY;
            else selectedType = DroneType.getRandomType(random);
        } else if (drone.level.isNight()) {
            int roll = random.nextInt(100);
            if (roll < 35) selectedType = DroneType.NIGHT;
            else selectedType = DroneType.getRandomType(random);
        } else {
            selectedType = DroneType.getRandomType(random);
        }
        drone.setDroneType(selectedType);

        BlockPos pos = drone.blockPosition();
        int groundY = event.getWorld().getHeight(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());

        if (drone.getY() < groundY + 5) {
            double spawnHeight = groundY + 12.0 + random.nextInt(8);
            spawnHeight = Math.min(spawnHeight, event.getWorld().getMaxBuildHeight() - 5);

            drone.setPos(drone.getX(), spawnHeight, drone.getZ());
            drone.setDeltaMovement(0, 0.5, 0);
        }
    }
}