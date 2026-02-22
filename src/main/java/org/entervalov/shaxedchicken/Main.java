package org.entervalov.shaxedchicken;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.entervalov.shaxedchicken.network.ModNetwork;
import org.entervalov.shaxedchicken.utils.DebugSystem;
import org.entervalov.shaxedchicken.world.ModConfiguredStructures;
import org.entervalov.shaxedchicken.world.ModStructurePieceTypes;
import org.entervalov.shaxedchicken.world.ModStructures;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "shaxedchicken";

    public Main() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModSounds.register(modBus);
        ModEntityTypes.register(modBus);
        ModTileEntities.register(modBus);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModContainers.register(modBus);
        ModStructures.register(modBus);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DebugSystem.registerCommand(event.getDispatcher());
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(DebugSystem::registerPackets);
        event.enqueueWork(ModNetwork::registerPackets);
        event.enqueueWork(ModSpawnEvents::registerSpawnPlacements);
        event.enqueueWork(() -> {
            ModStructurePieceTypes.register();
            ModConfiguredStructures.registerConfiguredStructures();
        });
    }
}
