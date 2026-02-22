package org.entervalov.shaxedchicken.world;

import com.google.common.collect.ImmutableMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.gen.feature.IFeatureConfig;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.settings.DimensionStructuresSettings;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.entervalov.shaxedchicken.Main;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ModConfiguredStructures {

    private static final Logger LOGGER = LogManager.getLogger();

    public static StructureFeature<NoFeatureConfig, ?> CONFIGURED_RADAR_TOWER;
    public static StructureFeature<NoFeatureConfig, ?> CONFIGURED_ABANDONED_LAB;

    public static final int RADAR_TOWER_SPACING_CHUNKS = 24;
    public static final int RADAR_TOWER_SEPARATION_CHUNKS = 8;
    public static final int ABANDONED_LAB_SPACING_CHUNKS = 40;
    public static final int ABANDONED_LAB_SEPARATION_CHUNKS = 16;
    public static final int RADAR_TOWER_SALT = 19472837;
    public static final int ABANDONED_LAB_SALT = 84927361;

    public static void registerConfiguredStructures() {
        registerStructureSeparation(
                ModStructures.RADAR_TOWER.get(),
                new StructureSeparationSettings(
                        RADAR_TOWER_SPACING_CHUNKS,
                        RADAR_TOWER_SEPARATION_CHUNKS,
                        RADAR_TOWER_SALT
                )
        );
        registerStructureSeparation(
                ModStructures.ABANDONED_LAB.get(),
                new StructureSeparationSettings(
                        ABANDONED_LAB_SPACING_CHUNKS,
                        ABANDONED_LAB_SEPARATION_CHUNKS,
                        ABANDONED_LAB_SALT
                )
        );

        CONFIGURED_RADAR_TOWER = registerConfigured(
                "radar_tower",
                ModStructures.RADAR_TOWER.get().configured(NoFeatureConfig.INSTANCE)
        );

        CONFIGURED_ABANDONED_LAB = registerConfigured(
                "abandoned_lab",
                ModStructures.ABANDONED_LAB.get().configured(NoFeatureConfig.INSTANCE)
        );

        LOGGER.info("[ShaxedChicken] Configured structures registered: radar_tower, abandoned_lab");
    }

    private static <FC extends IFeatureConfig, F extends Structure<FC>>
    StructureFeature<FC, F> registerConfigured(String name, StructureFeature<FC, F> configured) {
        Registry<StructureFeature<?, ?>> registry = WorldGenRegistries.CONFIGURED_STRUCTURE_FEATURE;
        return Registry.register(registry, new ResourceLocation(Main.MOD_ID, name), configured);
    }

    private static <F extends Structure<?>> void registerStructureSeparation(
            F structure, StructureSeparationSettings settings) {

        ResourceLocation key = Registry.STRUCTURE_FEATURE.getKey(structure);
        if (key != null) {
            Structure.STRUCTURES_REGISTRY.put(key.toString(), structure);
            LOGGER.info("[ShaxedChicken] Registered structure in STRUCTURES_REGISTRY: {}", key);
        } else {
            LOGGER.error("[ShaxedChicken] Structure registry key is null for: {}", structure);
        }

        patchDefaults(structure, settings);
    }

    private static void patchDefaults(Structure<?> structure, StructureSeparationSettings settings) {
        try {
            Field defaultsField = null;

            for (Field f : DimensionStructuresSettings.class.getDeclaredFields()) {
                if (f.getType() == Map.class || f.getType().getName().contains("ImmutableMap")) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())
                            && java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                        f.setAccessible(true);
                        Object val = f.get(null);
                        if (val instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) val;
                            if (!map.isEmpty() && map.keySet().iterator().next() instanceof Structure) {
                                defaultsField = f;
                                break;
                            }
                        }
                    }
                }
            }

            if (defaultsField == null) {
                String[] fieldNames = {"DEFAULTS", "field_236191_b_"};
                for (String name : fieldNames) {
                    try {
                        defaultsField = DimensionStructuresSettings.class.getDeclaredField(name);
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
            }

            if (defaultsField == null) {
                LOGGER.error("[ShaxedChicken] Could not find DEFAULTS field in DimensionStructuresSettings!");
                return;
            }

            defaultsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<Structure<?>, StructureSeparationSettings> currentMap =
                    (Map<Structure<?>, StructureSeparationSettings>) defaultsField.get(null);
            Map<Structure<?>, StructureSeparationSettings> newMap = new HashMap<>(currentMap);
            newMap.put(structure, settings);

            if (trySetStaticFinalWithUnsafe(defaultsField, ImmutableMap.copyOf(newMap))) {
                LOGGER.info("[ShaxedChicken] Patched DEFAULTS via Unsafe for: {}", structure.getRegistryName());
                return;
            }

            try {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(defaultsField, defaultsField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                defaultsField.set(null, ImmutableMap.copyOf(newMap));
                LOGGER.info("[ShaxedChicken] Patched DEFAULTS via reflection for: {}", structure.getRegistryName());
                return;
            } catch (Exception ex) {
                LOGGER.error("[ShaxedChicken] Reflection fallback failed", ex);
            }

        } catch (Exception e) {
            LOGGER.error("[ShaxedChicken] Failed to patch DimensionStructuresSettings.DEFAULTS", e);
        }
    }

    private static boolean trySetStaticFinalWithUnsafe(Field field, Object value) {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            long offset = unsafe.staticFieldOffset(field);
            Object base = unsafe.staticFieldBase(field);
            unsafe.putObject(base, offset, value);
            return true;
        } catch (Exception ex) {
            LOGGER.error("[ShaxedChicken] Unsafe update failed", ex);
            return false;
        }
    }
}

