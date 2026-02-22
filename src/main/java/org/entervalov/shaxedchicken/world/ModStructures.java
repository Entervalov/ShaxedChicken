package org.entervalov.shaxedchicken.world;

import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.entervalov.shaxedchicken.Main;

public class ModStructures {

    public static final DeferredRegister<Structure<?>> STRUCTURES =
            DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES, Main.MOD_ID);

    public static final RegistryObject<Structure<NoFeatureConfig>> RADAR_TOWER =
            STRUCTURES.register("radar_tower", RadarTowerStructure::new);

    public static final RegistryObject<Structure<NoFeatureConfig>> ABANDONED_LAB =
            STRUCTURES.register("abandoned_lab", AbandonedLabStructure::new);

    public static void register(IEventBus eventBus) {
        STRUCTURES.register(eventBus);
    }
}
