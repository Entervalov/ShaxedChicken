package org.entervalov.shaxedchicken;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.world.ModConfiguredStructures;
import org.entervalov.shaxedchicken.world.ModStructures;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public class ModSpawnEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int DRONE_SPAWN_WEIGHT = 3;
    private static final int DRONE_MIN_GROUP = 1;
    private static final int DRONE_MAX_GROUP = 2;
    private static final Set<Biome.Category> ALLOWED_CATEGORIES = EnumSet.of(
            Biome.Category.PLAINS,
            Biome.Category.FOREST,
            Biome.Category.TAIGA,
            Biome.Category.SWAMP,
            Biome.Category.SAVANNA,
            Biome.Category.DESERT,
            Biome.Category.JUNGLE,
            Biome.Category.MESA,
            Biome.Category.ICY
    );

    public static void registerSpawnPlacements() {
        EntitySpawnPlacementRegistry.register(
                ModEntityTypes.DRONE_CHICKEN.get(),
                EntitySpawnPlacementRegistry.PlacementType.NO_RESTRICTIONS,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                AdvancedDroneEntity::canSpawn
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBiomeLoading(BiomeLoadingEvent event) {
        Biome.Category category = event.getCategory();
        boolean isOverworldCategory = category != Biome.Category.NETHER
                && category != Biome.Category.THEEND
                && category != Biome.Category.NONE;
        boolean canSpawnDrones = ALLOWED_CATEGORIES.contains(category);

        if (canSpawnDrones) {
            event.getSpawns().getSpawner(EntityClassification.MONSTER).add(
                    new MobSpawnInfo.Spawners(
                            ModEntityTypes.DRONE_CHICKEN.get(),
                            DRONE_SPAWN_WEIGHT,
                            DRONE_MIN_GROUP,
                            DRONE_MAX_GROUP
                    )
            );
        }

        if (isOverworldCategory) {
            if (ModConfiguredStructures.CONFIGURED_RADAR_TOWER != null) {
                event.getGeneration().getStructures().add(() -> ModConfiguredStructures.CONFIGURED_RADAR_TOWER);
            }
            if (ModConfiguredStructures.CONFIGURED_ABANDONED_LAB != null) {
                event.getGeneration().getStructures().add(() -> ModConfiguredStructures.CONFIGURED_ABANDONED_LAB);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() instanceof net.minecraft.world.server.ServerWorld) {
            net.minecraft.world.server.ServerWorld serverWorld = (net.minecraft.world.server.ServerWorld) event.getWorld();

            if (!serverWorld.dimension().equals(net.minecraft.world.World.OVERWORLD)) {
                return;
            }

            Map<Structure<?>, StructureSeparationSettings> tempMap =
                    new HashMap<>(serverWorld.getChunkSource().generator.getSettings().structureConfig());

            if (!tempMap.containsKey(ModStructures.RADAR_TOWER.get())) {
                tempMap.put(ModStructures.RADAR_TOWER.get(), new StructureSeparationSettings(
                        ModConfiguredStructures.RADAR_TOWER_SPACING_CHUNKS,
                        ModConfiguredStructures.RADAR_TOWER_SEPARATION_CHUNKS,
                        ModConfiguredStructures.RADAR_TOWER_SALT
                ));
                LOGGER.info("[ShaxedChicken] Injected radar_tower separation into world settings");
            }

            if (!tempMap.containsKey(ModStructures.ABANDONED_LAB.get())) {
                tempMap.put(ModStructures.ABANDONED_LAB.get(), new StructureSeparationSettings(
                        ModConfiguredStructures.ABANDONED_LAB_SPACING_CHUNKS,
                        ModConfiguredStructures.ABANDONED_LAB_SEPARATION_CHUNKS,
                        ModConfiguredStructures.ABANDONED_LAB_SALT
                ));
                LOGGER.info("[ShaxedChicken] Injected abandoned_lab separation into world settings");
            }

            try {
                Field configField = null;
                for (Field f : DimensionStructuresSettings.class.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        Object val = f.get(serverWorld.getChunkSource().generator.getSettings());
                        if (val instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) val;
                            if (!map.isEmpty() && map.keySet().iterator().next() instanceof Structure) {
                                configField = f;
                                break;
                            }
                        }
                    }
                }

                if (configField != null) {
                    configField.setAccessible(true);
                    configField.set(serverWorld.getChunkSource().generator.getSettings(), tempMap);
                    LOGGER.info("[ShaxedChicken] Successfully patched world structure settings");
                }
            } catch (Exception e) {
                LOGGER.error("[ShaxedChicken] Failed to patch world structure settings", e);
            }
        }
    }
}
