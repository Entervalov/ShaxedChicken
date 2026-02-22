package org.entervalov.shaxedchicken;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, Main.MOD_ID);

    public static final RegistryObject<EntityType<AdvancedDroneEntity>> DRONE_CHICKEN =
            ENTITIES.register("drone_chicken",
                    () -> EntityType.Builder
                            .of(AdvancedDroneEntity::new, EntityClassification.MONSTER)
                            .sized(0.4F, 0.7F)
                            .clientTrackingRange(8)    // Уменьшено для оптимизации
                            .updateInterval(3)          // Реже обновления
                            .setShouldReceiveVelocityUpdates(true)
                            .build(new ResourceLocation(Main.MOD_ID, "drone_chicken").toString()));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}