package org.entervalov.shaxedchicken.client;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.entervalov.shaxedchicken.Main;
import org.entervalov.shaxedchicken.ModContainers;
import org.entervalov.shaxedchicken.ModEntityTypes;
import org.entervalov.shaxedchicken.ModTileEntities;
import org.entervalov.shaxedchicken.client.gui.JammerScreen;
import org.entervalov.shaxedchicken.client.renderer.DroneRenderer;
import org.entervalov.shaxedchicken.client.renderer.JammerTileEntityRenderer;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(
                ModEntityTypes.DRONE_CHICKEN.get(),
                DroneRenderer::new
        );

        ClientRegistry.bindTileEntityRenderer(
                ModTileEntities.JAMMER.get(),
                JammerTileEntityRenderer::new
        );

        ScreenManager.register(ModContainers.JAMMER.get(), JammerScreen::new);
    }
}
