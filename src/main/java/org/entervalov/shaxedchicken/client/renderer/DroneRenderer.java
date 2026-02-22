package org.entervalov.shaxedchicken.client.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.ChickenModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import org.entervalov.shaxedchicken.client.renderer.layer.DroneAuraLayer;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;
import org.entervalov.shaxedchicken.utils.DebugSystem;

import javax.annotation.Nonnull;

public class DroneRenderer extends MobRenderer<AdvancedDroneEntity, ChickenModel<AdvancedDroneEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/chicken.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/block/white_concrete.png");

    public DroneRenderer(EntityRendererManager manager) {
        super(manager, new ChickenModel<>(), 0.3F);
        this.addLayer(new DroneAuraLayer(this));
    }

    @Override
    public void render(@Nonnull AdvancedDroneEntity entity, float entityYaw, float partialTicks,
                       @Nonnull MatrixStack matrixStack, @Nonnull IRenderTypeBuffer buffer, int packedLight) {
        DroneType type = entity.getDroneType();
        float scale = type.getSizeMultiplier();

        matrixStack.pushPose();

        if (type.isNuclear()) {
            float pulse = (float) Math.sin((entity.tickCount + partialTicks) * 0.2F) * 0.05F + 1.0F;
            matrixStack.scale(scale * pulse, scale * pulse, scale * pulse);
        } else {
            matrixStack.scale(scale, scale, scale);
        }

        Vector3d velocity = entity.getDeltaMovement();
        double speedSq = velocity.x * velocity.x + velocity.z * velocity.z;
        if (speedSq > 0.002) {
            float tilt = (float) Math.min(Math.sqrt(speedSq) * 80, 45);
            matrixStack.mulPose(Vector3f.XP.rotationDegrees(tilt));
        }

        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
        matrixStack.popPose();

        if (DebugSystem.SHOW_DRONE_DEBUG) {
            renderDebugInfo(entity, matrixStack, buffer);

            if (DebugSystem.SHOW_PATH_LINES) {
                renderPathLine(entity, matrixStack, buffer, partialTicks);
            }
        }

        renderEmergencyTimer(entity, matrixStack, buffer);
    }


    private void renderPathLine(AdvancedDroneEntity entity, MatrixStack ms, IRenderTypeBuffer buffer, float partialTicks) {
        BlockPos targetPos = entity.getDebugTargetPos();
        if (targetPos == null) return;


        IVertexBuilder lineBuilder = buffer.getBuffer(RenderType.lines());

        ms.pushPose();

        double lerpX = MathHelper.lerp(partialTicks, entity.xo, entity.getX());
        double lerpY = MathHelper.lerp(partialTicks, entity.yo, entity.getY());
        double lerpZ = MathHelper.lerp(partialTicks, entity.zo, entity.getZ());

        double dx = targetPos.getX() + 0.5 - lerpX;
        double dy = targetPos.getY() + 0.5 - lerpY;
        double dz = targetPos.getZ() + 0.5 - lerpZ;

        Matrix4f matrix = ms.last().pose();

        float r = 0, g = 1, b = 0;
        switch (entity.getAIState()) {
            case ATTACK: r=1; g=0; b=0; break;
            case DIVE_BOMB: r=1; g=0; b=0; break;
            case CHASE: r=1; g=1; b=0; break;
            case SEARCH: r=0; g=1; b=1; break;
            case RETREAT: r=1; g=0; b=1; break;
            case EVADE: r=1; g=0.5f; b=0; break;
        }

        lineBuilder.vertex(matrix, 0, 0.5f, 0).color(r, g, b, 1.0F).endVertex();
        lineBuilder.vertex(matrix, (float)dx, (float)dy, (float)dz).color(r, g, b, 1.0F).endVertex();

        ms.popPose();
    }

    private void renderDebugInfo(AdvancedDroneEntity entity, MatrixStack ms, IRenderTypeBuffer buffer) {
        ms.pushPose();

        float height = entity.getBbHeight() + 0.75F;
        ms.translate(0, height, 0);

        ms.mulPose(this.entityRenderDispatcher.cameraOrientation());

        ms.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = ms.last().pose();
        FontRenderer font = this.getFont();

        DroneType type = entity.getDroneType();
        String hpColor = entity.getHealth() < 10 ? TextFormatting.RED.toString() : TextFormatting.GREEN.toString();

        String jammedInfo = entity.isJammed()
                ? TextFormatting.RED + "YES (" + entity.getDebugJammedTicks() + "t)"
                : TextFormatting.GREEN + "NO";

        double dist = entity.getDistanceToJammer();
        String jammerDist = dist > 900 ? "N/A" : String.format("%.1f", dist);
        if (dist < 10) jammerDist = TextFormatting.DARK_RED + jammerDist + " (CRIT)";
        else if (dist < 30) jammerDist = TextFormatting.YELLOW + jammerDist + " (NOISE)";

        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        double speedVal = Math.sqrt(dx*dx + dy*dy + dz*dz) * 20.0;
        String speedStr;
        if (speedVal < 0.01) speedStr = "0.00 m/s (Idle)";
        else if (speedVal > 15) speedStr = TextFormatting.RED + String.format("%.2f m/s", speedVal);
        else speedStr = String.format("%.2f m/s", speedVal);

        String[] lines = new String[] {
                TextFormatting.GOLD + "=== DRONE DEBUG ===",
                type.getColor() + type.name() + TextFormatting.RESET + " [ID:" + entity.getId() + "]",
                "HP: " + hpColor + String.format("%.1f", entity.getHealth()) + "/" + entity.getMaxHealth(),
                "AI: " + TextFormatting.AQUA + entity.getAIState().name(),
                "TGT: " + (entity.getDebugTargetId() == -1 ? "None" : entity.getDebugTargetId()),
                "Spd: " + speedStr,
                "----------------",
                "Jammed: " + jammedInfo,
                "J.Dist: " + jammerDist
        };

        int maxWidth = 0;
        for (String line : lines) maxWidth = Math.max(maxWidth, font.width(line));

        int halfWidth = maxWidth / 2;
        int backgroundHeight = lines.length * 10;

        IVertexBuilder bgBuilder = buffer.getBuffer(RenderType.text(WHITE_TEXTURE));

        float x1 = -halfWidth - 2;
        float x2 = halfWidth + 2;
        float y1 = -2;
        float y2 = backgroundHeight + 2;
        float a = 0.65F;

        bgBuilder.vertex(matrix4f, x1, y2, -0.01F).color(0f, 0f, 0f, a).uv(0, 1).uv2(240).endVertex();
        bgBuilder.vertex(matrix4f, x2, y2, -0.01F).color(0f, 0f, 0f, a).uv(1, 1).uv2(240).endVertex();
        bgBuilder.vertex(matrix4f, x2, y1, -0.01F).color(0f, 0f, 0f, a).uv(1, 0).uv2(240).endVertex();
        bgBuilder.vertex(matrix4f, x1, y1, -0.01F).color(0f, 0f, 0f, a).uv(0, 0).uv2(240).endVertex();

        int yOffset = 0;
        for (String line : lines) {
            float xOffset = (float)(-font.width(line) / 2);
            font.drawInBatch(line, xOffset, yOffset, 0xFFFFFFFF, true, matrix4f, buffer,
                    false, 0, 0xF000F0);
            yOffset += 10;
        }

        ms.popPose();
    }

    private void renderEmergencyTimer(AdvancedDroneEntity entity, MatrixStack ms, IRenderTypeBuffer buffer) {
        int ticks = entity.getEmergencyTicks();
        if (ticks <= 0) return;

        ms.pushPose();

        float height = entity.getBbHeight() + 0.75F;
        if (DebugSystem.SHOW_DRONE_DEBUG) {
            height += 0.9F;
        }
        ms.translate(0, height, 0);
        ms.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ms.scale(-0.025F, -0.025F, 0.025F);

        FontRenderer font = this.getFont();
        float seconds = ticks / 20.0F;
        String text = "T-" + String.format("%.1f", seconds) + "s";
        int color = ticks <= 20 ? 0xFFFF5555 : 0xFFFFFF55;

        int textWidth = font.width(text);
        float x1 = -textWidth / 2f - 3;
        float x2 = textWidth / 2f + 3;
        float y1 = -3;
        float y2 = 9;

        Matrix4f matrix4f = ms.last().pose();
        IVertexBuilder bgBuilder = buffer.getBuffer(RenderType.text(WHITE_TEXTURE));
        bgBuilder.vertex(matrix4f, x1, y2, -0.01F).color(0f, 0f, 0f, 0.6f).uv(0, 1).uv2(240).endVertex();
        bgBuilder.vertex(matrix4f, x2, y2, -0.01F).color(0f, 0f, 0f, 0.6f).uv(1, 1).uv2(240).endVertex();
        bgBuilder.vertex(matrix4f, x2, y1, -0.01F).color(0f, 0f, 0f, 0.6f).uv(1, 0).uv2(240).endVertex();
        bgBuilder.vertex(matrix4f, x1, y1, -0.01F).color(0f, 0f, 0f, 0.6f).uv(0, 0).uv2(240).endVertex();

        font.drawInBatch(text, -textWidth / 2f, 0, color, true, matrix4f, buffer, false, 0, 0xF000F0);

        ms.popPose();
    }

    @Override
    protected int getBlockLightLevel(@Nonnull AdvancedDroneEntity entity, @Nonnull net.minecraft.util.math.BlockPos pos) {
        if (entity.getDroneType().isNuclear()) return 15;
        return super.getBlockLightLevel(entity, pos);
    }

    @Nonnull
    @Override
    public ResourceLocation getTextureLocation(@Nonnull AdvancedDroneEntity entity) {
        return TEXTURE;
    }
}
