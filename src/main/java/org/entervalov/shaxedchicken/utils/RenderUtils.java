package org.entervalov.shaxedchicken.utils;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class RenderUtils {

    // Обычная заливка
    public static void fill(MatrixStack ms, int minX, int minY, int maxX, int maxY, int color) {
        int temp;
        if (minX < maxX) { temp = minX; minX = maxX; maxX = temp; }
        if (minY < maxY) { temp = minY; minY = maxY; maxY = temp; }

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.vertex(ms.last().pose(), (float)minX, (float)maxY, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(ms.last().pose(), (float)maxX, (float)maxY, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(ms.last().pose(), (float)maxX, (float)minY, 0.0F).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(ms.last().pose(), (float)minX, (float)minY, 0.0F).color(r, g, b, a).endVertex();
        tessellator.end();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void fillGradient(MatrixStack ms, int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425); // GL_SMOOTH

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        // Разбираем цвета
        float a1 = (float)(colorTop >> 24 & 255) / 255.0F;
        float r1 = (float)(colorTop >> 16 & 255) / 255.0F;
        float g1 = (float)(colorTop >> 8 & 255) / 255.0F;
        float b1 = (float)(colorTop & 255) / 255.0F;

        float a2 = (float)(colorBottom >> 24 & 255) / 255.0F;
        float r2 = (float)(colorBottom >> 16 & 255) / 255.0F;
        float g2 = (float)(colorBottom >> 8 & 255) / 255.0F;
        float b2 = (float)(colorBottom & 255) / 255.0F;

        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // Вершины для вертикального градиента (верх один цвет, низ другой)
        buffer.vertex(ms.last().pose(), (float)x2, (float)y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(ms.last().pose(), (float)x1, (float)y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(ms.last().pose(), (float)x1, (float)y2, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(ms.last().pose(), (float)x2, (float)y2, 0).color(r2, g2, b2, a2).endVertex();
        tessellator.end();

        RenderSystem.shadeModel(7424); // GL_FLAT
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public static void fillGradientH(MatrixStack ms, int x1, int y1, int x2, int y2, int colorLeft, int colorRight) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        float a1 = (float)(colorLeft >> 24 & 255) / 255.0F;
        float r1 = (float)(colorLeft >> 16 & 255) / 255.0F;
        float g1 = (float)(colorLeft >> 8 & 255) / 255.0F;
        float b1 = (float)(colorLeft & 255) / 255.0F;

        float a2 = (float)(colorRight >> 24 & 255) / 255.0F;
        float r2 = (float)(colorRight >> 16 & 255) / 255.0F;
        float g2 = (float)(colorRight >> 8 & 255) / 255.0F;
        float b2 = (float)(colorRight & 255) / 255.0F;

        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // Левые вершины - colorLeft, Правые - colorRight
        buffer.vertex(ms.last().pose(), (float)x1, (float)y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(ms.last().pose(), (float)x1, (float)y2, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(ms.last().pose(), (float)x2, (float)y2, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(ms.last().pose(), (float)x2, (float)y1, 0).color(r2, g2, b2, a2).endVertex();
        tessellator.end();

        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    public static int blendColors(int color1, int color2, float ratio) {
        float iRatio = 1.0f - ratio;
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a = (int)((a1 * iRatio) + (a2 * ratio));
        int r = (int)((r1 * iRatio) + (r2 * ratio));
        int g = (int)((g1 * iRatio) + (g2 * ratio));
        int b = (int)((b1 * iRatio) + (b2 * ratio));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
