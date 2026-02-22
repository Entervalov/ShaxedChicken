package org.entervalov.shaxedchicken.client.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import org.entervalov.shaxedchicken.block.JammerTileEntity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class JammerTileEntityRenderer extends TileEntityRenderer<JammerTileEntity> {

    private static final int WAVE_COUNT = 3;
    private static final float WAVE_RADIUS = 15.0F;

    public JammerTileEntityRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(JammerTileEntity te, float partialTicks, @Nonnull MatrixStack matrixStack,
                       @Nonnull IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {

        if (!te.isActive()) return;

        if (te.isJammerEnabled()) {
            renderWaves(te, partialTicks, matrixStack, buffer);
        }

        if (te.isLaserEnabled()) {
            renderLasers(te, partialTicks, matrixStack, buffer);
        }
    }

    private void renderLasers(JammerTileEntity te, float partialTicks, MatrixStack ms, IRenderTypeBuffer buffer) {
        float charge = te.getChargeProgress();
        List<Integer> ids = te.getTargetIds();

        List<Entity> targets = new ArrayList<>();
        if (te.getLevel() != null) {
            for (int id : ids) {
                Entity e = te.getLevel().getEntity(id);
                if (e != null && e.isAlive()) targets.add(e);
            }
        }

        ms.pushPose();
        ms.translate(0.5, 2.0, 0.5);

        IVertexBuilder beamBuilder = buffer.getBuffer(RenderType.lightning());
        Matrix4f matrix = ms.last().pose();

        float time = te.getTickCounter() + partialTicks;
        float laserRange = (float) te.getLaserRange();
        float[] scanColor = getModuleColor(te);

        if (targets.isEmpty()) {
            renderScanningMode(matrix, beamBuilder, time, laserRange, scanColor[0], scanColor[1], scanColor[2]);
        } else {
            for (Entity target : targets) {
                renderAttackLaser(matrix, beamBuilder, ms, buffer, target, charge, partialTicks, te, time);
            }
        }

        ms.popPose();
    }

    private void renderScanningMode(Matrix4f matrix, IVertexBuilder builder, float time, float range, float r, float g, float b) {
        float speed = time * 0.02F;
        float pulse = 1.0F + MathHelper.sin(time * 0.1F) * 0.1F;

        Vector3d end1 = new Vector3d(Math.sin(speed) * range, 2.0, Math.cos(speed) * range);
        drawCylindricalBeam(builder, matrix, end1, 0.1F * pulse, r, g, b, 0.4F);

        float angle2 = speed * 1.3F + 2.0F;
        Vector3d end2 = new Vector3d(Math.sin(angle2) * range * 0.5, Math.abs(Math.cos(angle2)) * range * 0.8 + 2.0, Math.cos(angle2) * range * 0.5);
        drawCylindricalBeam(builder, matrix, end2, 0.1F * pulse, r, g, b, 0.4F);

        float angle3 = speed * 0.8F + 4.0F;
        Vector3d end3 = new Vector3d(Math.cos(angle3) * range, Math.sin(angle3 * 0.5) * (range * 0.5) + 5.0, Math.sin(angle3) * range);
        drawCylindricalBeam(builder, matrix, end3, 0.1F * pulse, r, g, b, 0.4F);
    }

    private void renderAttackLaser(Matrix4f matrix, IVertexBuilder builder, MatrixStack ms, IRenderTypeBuffer buffer,
                                   Entity target, float charge, float pt, JammerTileEntity te, float time) {

        Vector3d targetVec = getTargetVec(target, pt, te);

        float pulseSpeed = 0.2F + (charge * 0.8F);
        float pulseAmp = 0.05F + (charge * 0.1F);
        float pulse = 1.0F + MathHelper.sin(time * pulseSpeed) * pulseAmp;

        float r, g, b, width, alpha;

        if (charge < 0.30F) {
            r = 0.0F; g = 1.0F; b = 0.2F;
            width = 0.04F * pulse;
            alpha = 0.7F;
        } else if (charge < 0.90F) {
            r = 1.0F; g = 0.8F - (charge * 0.5F); b = 0.0F;
            width = (0.06F + (charge * 0.05F)) * pulse;
            alpha = 0.9F;
            if (charge > 0.5F) {
                double shake = 0.05 * (charge - 0.5F);
                targetVec = targetVec.add((Math.random()-0.5)*shake, (Math.random()-0.5)*shake, (Math.random()-0.5)*shake);
            }
        } else {
            r = 1.0F; g = 0.0F; b = 0.0F;
            width = 0.22F * pulse;
            alpha = 1.0F;
        }

        drawGlowingBeam(builder, matrix, targetVec, width, r, g, b, alpha);

        if (charge > 0.3F) {
            renderHalo(ms, buffer, targetVec, width * 4.0F, r, g, b, alpha);
        }
    }

    private void drawGlowingBeam(IVertexBuilder builder, Matrix4f mat, Vector3d end,
                                 float width, float r, float g, float b, float a) {
        drawCylindricalBeam(builder, mat, end, width * 2.5F, r, g, b, a * 0.2F);
        drawCylindricalBeam(builder, mat, end, width, r, g, b, a * 0.8F);
        drawCylindricalBeam(builder, mat, end, width * 0.3F, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawCylindricalBeam(IVertexBuilder builder, Matrix4f mat, Vector3d end,
                                     float radius, float r, float g, float b, float a) {

        Vector3d diff = end.subtract(Vector3d.ZERO);
        Vector3d up = new Vector3d(0, 1, 0);
        if (Math.abs(diff.normalize().y) > 0.95) up = new Vector3d(1, 0, 0);

        Vector3d right = diff.cross(up).normalize();
        Vector3d forward = diff.cross(right).normalize();

        int segments = 8;
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;

            Vector3d offset1 = right.scale(Math.cos(angle1) * radius).add(forward.scale(Math.sin(angle1) * radius));
            Vector3d offset2 = right.scale(Math.cos(angle2) * radius).add(forward.scale(Math.sin(angle2) * radius));

            Vector3d v1 = Vector3d.ZERO.add(offset1);
            Vector3d v2 = end.add(offset1);
            Vector3d v3 = end.add(offset2);
            Vector3d v4 = Vector3d.ZERO.add(offset2);

            addQuad(builder, mat, v1, v2, v3, v4, r, g, b, a);
            addQuad(builder, mat, v4, v3, v2, v1, r, g, b, a);
        }
    }

    private void addQuad(IVertexBuilder builder, Matrix4f mat, Vector3d v1, Vector3d v2, Vector3d v3, Vector3d v4,
                         float r, float g, float b, float a) {
        builder.vertex(mat, (float)v1.x, (float)v1.y, (float)v1.z).color(r,g,b,a).endVertex();
        builder.vertex(mat, (float)v2.x, (float)v2.y, (float)v2.z).color(r,g,b,a).endVertex();
        builder.vertex(mat, (float)v3.x, (float)v3.y, (float)v3.z).color(r,g,b,a).endVertex();
        builder.vertex(mat, (float)v4.x, (float)v4.y, (float)v4.z).color(r,g,b,a).endVertex();
    }

    private void renderHalo(MatrixStack ms, IRenderTypeBuffer buffer, Vector3d pos, float size, float r, float g, float b, float a) {
        ms.pushPose();
        ms.translate(pos.x, pos.y, pos.z);
        ms.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        IVertexBuilder builder = buffer.getBuffer(RenderType.lightning());
        Matrix4f m = ms.last().pose();
        float h = size / 2.0F;

        builder.vertex(m, -h, -h, 0).color(r, g, b, a).endVertex();
        builder.vertex(m, h, -h, 0).color(r, g, b, a).endVertex();
        builder.vertex(m, h, h, 0).color(r, g, b, a).endVertex();
        builder.vertex(m, -h, h, 0).color(r, g, b, a).endVertex();

        ms.popPose();
    }

    private Vector3d getTargetVec(Entity target, float pt, JammerTileEntity te) {
        double tx = MathHelper.lerp(pt, target.xo, target.getX());
        double ty = MathHelper.lerp(pt, target.yo, target.getY()) + target.getBbHeight() * 0.5;
        double tz = MathHelper.lerp(pt, target.zo, target.getZ());

        return new Vector3d(tx, ty, tz).subtract(
                te.getBlockPos().getX() + 0.5,
                te.getBlockPos().getY() + 2.0,
                te.getBlockPos().getZ() + 0.5
        );
    }

    private void renderWaves(JammerTileEntity te, float pt, MatrixStack ms, IRenderTypeBuffer buffer) {
        IVertexBuilder builder = buffer.getBuffer(RenderType.lightning());
        ms.pushPose();
        ms.translate(0.5, 1.5, 0.5);

        float baseTime = (te.getTickCounter() + pt) / 120.0F;
        Matrix4f m = ms.last().pose();

        float[] waveColor = getModuleColor(te);
        float r = waveColor[0];
        float g = waveColor[1];
        float b = waveColor[2];
        int waveCount = WAVE_COUNT + getExtraWaves(te);

        for (int i = 0; i < waveCount; i++) {
            float progress = (baseTime + (float)i / waveCount) % 1.0F;
            if (progress < 0.05F || progress > 0.95F) continue;

            float maxRadius = (float) Math.max(WAVE_RADIUS, Math.min(te.getJammerRadius(), 45.0));
            float radius = progress * maxRadius;
            float alpha = (1.0F - progress) * 0.4F;

            int segments = 64;
            for (int j = 0; j < segments; j++) {
                double a1 = 2 * Math.PI * j / segments;
                double a2 = 2 * Math.PI * (j + 1) / segments;

                float x1 = (float)Math.cos(a1) * radius; float z1 = (float)Math.sin(a1) * radius;
                float x2 = (float)Math.cos(a2) * radius; float z2 = (float)Math.sin(a2) * radius;

                builder.vertex(m, x1, 0, z1).color(r, g, b, alpha).endVertex();
                builder.vertex(m, x2, 0, z2).color(r, g, b, alpha).endVertex();
                builder.vertex(m, x2, 0.2f, z2).color(r, g, b, alpha).endVertex();
                builder.vertex(m, x1, 0.2f, z1).color(r, g, b, alpha).endVertex();

                float rOut = radius + 0.1F;
                float x1o = (float)Math.cos(a1) * rOut; float z1o = (float)Math.sin(a1) * rOut;
                float x2o = (float)Math.cos(a2) * rOut; float z2o = (float)Math.sin(a2) * rOut;

                builder.vertex(m, x1o, 0, z1o).color(r, g, b, alpha * 0.5F).endVertex();
                builder.vertex(m, x2o, 0, z2o).color(r, g, b, alpha * 0.5F).endVertex();
                builder.vertex(m, x2o, 0.05f, z2o).color(r, g, b, alpha * 0.5F).endVertex();
                builder.vertex(m, x1o, 0.05f, z1o).color(r, g, b, alpha * 0.5F).endVertex();
            }
        }
        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@Nonnull JammerTileEntity te) { return true; }

    private float[] getModuleColor(JammerTileEntity te) {
        if (te == null || te.getHighestModuleType() == null) return new float[] {0.1F, 0.5F, 1.0F};
        switch (te.getHighestModuleType()) {
            case BASIC: return new float[] {0.2F, 0.7F, 1.0F};
            case UNCOMMON: return new float[] {0.2F, 1.0F, 0.4F};
            case EPIC: return new float[] {0.7F, 0.3F, 1.0F};
            case LEGENDARY: return new float[] {1.0F, 0.7F, 0.2F};
            default: return new float[] {0.1F, 0.5F, 1.0F};
        }
    }

    private int getExtraWaves(JammerTileEntity te) {
        if (te == null || te.getHighestModuleType() == null) return 0;
        switch (te.getHighestModuleType()) {
            case EPIC: return 1;
            case LEGENDARY: return 2;
            default: return 0;
        }
    }
}
