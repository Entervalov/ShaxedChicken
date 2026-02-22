package org.entervalov.shaxedchicken;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MOD_ID);

    public static final RegistryObject<SoundEvent> CHICKEN_PRIME =
            SOUNDS.register("chicken_prime",
                    () -> new SoundEvent(new ResourceLocation(Main.MOD_ID, "chicken_prime")));

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}