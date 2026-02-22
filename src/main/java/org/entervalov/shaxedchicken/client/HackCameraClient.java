package org.entervalov.shaxedchicken.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.entervalov.shaxedchicken.Main;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.network.DroneInputPacket;
import org.entervalov.shaxedchicken.network.ModNetwork;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class HackCameraClient {

    private static final ResourceLocation VIGNETTE = new ResourceLocation("minecraft", "textures/misc/vignette.png");
    private static boolean isActive = false;

    private static float droneYaw = 0;
    private static float dronePitch = 0;
    private static float prevPlayerYaw = 0;
    private static float prevPlayerPitch = 0;

    public static void setDroneCamera(int entityId, boolean enable) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (enable) {
            Entity target = mc.level.getEntity(entityId);
            if (target != null) {
                mc.setCameraEntity(target);
                isActive = true;

                droneYaw = target.yRot;
                dronePitch = target.xRot;
                if (mc.player != null) {
                    prevPlayerYaw = mc.player.yRot;
                    prevPlayerPitch = mc.player.xRot;
                }
            }
        } else {
            if (isActive) {
                assert mc.player != null;
                mc.setCameraEntity(mc.player);
                isActive = false;
            }
        }
    }

    public static boolean isControllingDrone() {
        return isActive;
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        if (isActive && event.getGui() instanceof IngameMenuScreen) {
            event.setCanceled(true);
            disconnectDrone();
        }
    }

    private static void disconnectDrone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity cam = mc.getCameraEntity();
        if (cam instanceof AdvancedDroneEntity) {
            AdvancedDroneEntity drone = (AdvancedDroneEntity) cam;
            ModNetwork.CHANNEL.sendToServer(new DroneInputPacket(
                    drone.getId(), false, true, droneYaw, dronePitch, 0, 0, false, false
            ));
        }
        mc.setCameraEntity(mc.player);
        isActive = false;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (isActive && mc.getCameraEntity() instanceof AdvancedDroneEntity) {
            AdvancedDroneEntity drone = (AdvancedDroneEntity) mc.getCameraEntity();

            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            MatrixStack ms = event.getMatrixStack();

            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.defaultBlendFunc();

            mc.getTextureManager().bind(VIGNETTE);
            RenderSystem.color4f(0.0F, 1.0F, 0.0F, 0.4F);
            AbstractGui.blit(ms, 0, 0, 0, 0, width, height, width, height);

            fillScanlines(ms, width, height);

            if (drone.isJammed()) {
                renderJammedNoise(ms, width, height, drone);
            }

            ms.pushPose();
            ms.scale(1.5f, 1.5f, 1.5f);
            if (drone.isJammed()) {
                mc.font.draw(ms, "!! SIGNAL INTERFERENCE !!", 6, 6, 0xFF0000);
            } else {
                mc.font.draw(ms, "UAV LINK ESTABLISHED", 6, 6, 0x00FF00);
            }
            ms.popPose();

            int infoY = 24;
            mc.font.draw(ms, "ALT: " + String.format("%.1f", drone.getY()), 10, infoY, 0x00FF00);
            infoY += 10;
            double speed = drone.getDeltaMovement().length() * 20;
            mc.font.draw(ms, "SPD: " + String.format("%.1f m/s", speed), 10, infoY, 0x00FF00);
            infoY += 10;

            String status = drone.getAIState().name();
            mc.font.draw(ms, "MODE: " + status, 10, infoY, 0x00FF00);
            infoY += 10;

            if (drone.isJammed()) {
                double dist = drone.getDistanceToJammer();
                int distColor = dist < 15 ? 0xFF0000 : 0xFFFF00;
                mc.font.draw(ms, "JAMMER DIST: " + String.format("%.0f", dist) + "m", 10, infoY, distColor);
                infoY += 10;
            }

            String chunkText = drone.isChunkLockActive() ? "CHUNK LOCK: ACTIVE" : "CHUNK LOCK: OFF";
            int chunkColor = drone.isChunkLockActive() ? 0x00FF55 : 0xFF5555;
            mc.font.draw(ms, chunkText, 10, infoY, chunkColor);

            String help = "[LMB] KAMIKAZE   [ESC] DISCONNECT";
            int helpWidth = mc.font.width(help);
            mc.font.draw(ms, help, (width - helpWidth) / 2f, height - 20, 0xFFFF00);

            mc.font.draw(ms, "[ + ]", width / 2f - 10, height / 2f - 4, 0xFF0000);
            RenderSystem.enableDepthTest();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void fillScanlines(MatrixStack ms, int w, int h) {
        int color = 0x40000000;
        for (int y = 0; y < h; y += 4) {
            AbstractGui.fill(ms, 0, y, w, y + 1, color);
        }
    }

    private static void renderJammedNoise(MatrixStack ms, int w, int h, AdvancedDroneEntity drone) {
        double dist = drone.getDistanceToJammer();
        float intensity = (float) Math.max(0.1, 1.0 - (dist / 30.0));

        int stripeCount = (int) (10 + intensity * 30);
        long seed = (long) (drone.tickCount * 7 + drone.getId());
        java.util.Random noiseRng = new java.util.Random(seed);

        for (int i = 0; i < stripeCount; i++) {
            int y = noiseRng.nextInt(h);
            int stripeH = 1 + noiseRng.nextInt(3);
            int alpha = (int) (intensity * (80 + noiseRng.nextInt(100)));
            alpha = Math.min(alpha, 200);
            int color = (alpha << 24) | (noiseRng.nextBoolean() ? 0xFF0000 : 0xFFFFFF);
            AbstractGui.fill(ms, 0, y, w, y + stripeH, color);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (isActive && (mc.getCameraEntity() == null || !mc.getCameraEntity().isAlive())) {
            if (mc.player != null) mc.setCameraEntity(mc.player);
            isActive = false;
            return;
        }

        if (isActive && mc.getCameraEntity() instanceof AdvancedDroneEntity && mc.player != null) {
            AdvancedDroneEntity drone = (AdvancedDroneEntity) mc.getCameraEntity();

            float deltaYaw = mc.player.yRot - prevPlayerYaw;
            float deltaPitch = mc.player.xRot - prevPlayerPitch;
            prevPlayerYaw = mc.player.yRot;
            prevPlayerPitch = mc.player.xRot;

            droneYaw += deltaYaw;
            dronePitch += deltaPitch;
            dronePitch = net.minecraft.util.math.MathHelper.clamp(dronePitch, -90.0F, 90.0F);

            drone.yRot = droneYaw;
            drone.yRotO = droneYaw;
            drone.xRot = dronePitch;
            drone.xRotO = dronePitch;
            drone.yHeadRot = droneYaw;
            drone.yBodyRot = droneYaw;

            boolean attack = mc.mouseHandler.isLeftPressed();
            boolean sneak = mc.options.keyShift.isDown();
            boolean jump = mc.options.keyJump.isDown();

            float forward = 0;
            if (mc.options.keyUp.isDown()) forward += 1;
            if (mc.options.keyDown.isDown()) forward -= 1;

            float strafe = 0;
            if (mc.options.keyLeft.isDown()) strafe += 1;
            if (mc.options.keyRight.isDown()) strafe -= 1;

            ModNetwork.CHANNEL.sendToServer(new DroneInputPacket(
                    drone.getId(),
                    attack,
                    false,
                    droneYaw,
                    dronePitch,
                    forward,
                    strafe,
                    jump,
                    sneak
            ));

            if (attack) mc.mouseHandler.grabMouse();
        }
    }
}
