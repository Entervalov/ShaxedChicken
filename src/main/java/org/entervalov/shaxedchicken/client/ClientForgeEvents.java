package org.entervalov.shaxedchicken.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.entervalov.shaxedchicken.Main;
import org.entervalov.shaxedchicken.client.gui.changelog.ChangelogPanel;
import org.entervalov.shaxedchicken.utils.UpdateChecker;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public class ClientForgeEvents {

    private static ChangelogPanel panel;

    private static boolean sessionClosed = false;

    @SubscribeEvent
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof MainMenuScreen) {

            if (UpdateChecker.hasNewVersion() && !sessionClosed) {

                int width = event.getGui().width;
                int height = event.getGui().height;

                if (panel == null || width != Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
                    panel = new ChangelogPanel(Minecraft.getInstance(), width, height);
                } else {
                    panel.init(width, height);
                }

                panel.setVisible(true);

            } else {
                panel = null;
            }
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.getGui() instanceof MainMenuScreen && panel != null && panel.isVisible()) {
            panel.render(event.getMatrixStack(), event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
        }
    }

    @SubscribeEvent
    public static void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (event.getGui() instanceof MainMenuScreen && panel != null && panel.isVisible()) {

            if (panel.mouseClicked(event.getMouseX(), event.getMouseY())) {

                if (!panel.isVisible()) {
                    sessionClosed = true;
                }
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(GuiScreenEvent.MouseScrollEvent.Pre event) {
        if (event.getGui() instanceof MainMenuScreen && panel != null && panel.isVisible()) {
            if (panel.isMouseOver(event.getMouseX(), event.getMouseY())) {
                panel.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta());
                event.setCanceled(true);
            }
        }
    }
}