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

        // 1. Выбор типа (как и было)
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

        // 2. ФИКС ВЫСОТЫ СПАВНА
        // Если дрон заспавнился на земле - поднимем его в воздух!
        // Иначе он начинает думать, что он курица, и ходит пешком
        BlockPos pos = drone.blockPosition();
        int groundY = event.getWorld().getHeight(Heightmap.Type.MOTION_BLOCKING, pos.getX(), pos.getZ());

        // Если дрон слишком близко к земле (< 5 блоков)
        if (drone.getY() < groundY + 5) {
            // Телепортируем его на 10-15 блоков вверх
            double spawnHeight = groundY + 12.0 + random.nextInt(8);
            // Проверка на потолок мира
            spawnHeight = Math.min(spawnHeight, event.getWorld().getMaxBuildHeight() - 5);

            drone.setPos(drone.getX(), spawnHeight, drone.getZ());
            // Даем начальный импульс
            drone.setDeltaMovement(0, 0.5, 0);
        }
    }
}
