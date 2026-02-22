package org.entervalov.shaxedchicken;

import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAttributes {

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.DRONE_CHICKEN.get(), AdvancedDroneEntity.createAttributes().build());
    }
}
