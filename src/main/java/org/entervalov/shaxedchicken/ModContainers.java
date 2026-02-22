package org.entervalov.shaxedchicken;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.entervalov.shaxedchicken.container.JammerContainer;

public class ModContainers {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, Main.MOD_ID);

    public static final RegistryObject<ContainerType<JammerContainer>> JAMMER =
            CONTAINERS.register("jammer", () -> IForgeContainerType.create((windowId, inv, data) -> new JammerContainer(windowId, inv, data)));

    public static void register(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }
}
