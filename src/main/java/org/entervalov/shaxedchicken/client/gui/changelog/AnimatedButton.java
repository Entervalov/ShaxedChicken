package org.entervalov.shaxedchicken.client.gui.changelog;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;

import static org.entervalov.shaxedchicken.utils.RenderUtils.*;

/**
 * Saints Row IV style button.
 * Dark background, purple highlight on hover, sharp rectangular shape,
 * uppercase text, clean aggressive look.
 */
public class AnimatedButton extends Button {

    private float hoverAnimation = 0f;
    private float clickAnimation = 0f;
    private float alpha = 1f;

    // SR4 palette
    private static final int SR_BG_NORMAL = 0x111114;
    private static final int SR_BG_HOVER = 0x7B2FBE;
    private static final int SR_PURPLE = 0x7B2FBE;
    private static final int SR_PURPLE_BRIGHT = 0x9B4FDE;
    private static final int SR_BORDER = 0x333338;
    private static final int SR_TEXT_NORMAL = 0xAAAAAA;
    private static final int SR_TEXT_HOVER = 0xFFFFFF;

    public AnimatedButton(int x, int y, int width, int height, ITextComponent text, IPressable onPress) {
        super(x, y, width, height, text, onPress);
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    @Override
    public void renderButton(@Nonnull MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();

        float targetHover = isHovered ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * partialTicks * 0.35f;
        clickAnimation = Math.max(0, clickAnimation - partialTicks * 0.12f);

        int a = (int) (255 * alpha);
        if (a <= 0) return;

        float scale = 1f - clickAnimation * 0.03f;
        int offsetY = (int) (clickAnimation * 1);

        ms.pushPose();
        ms.translate(x + width / 2f, y + height / 2f, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-(x + width / 2f), -(y + height / 2f), 0);

        // Фон кнопки — при наведении заливается фиолетовым (SR4 фирменный стиль)
        int bgColor = blendColors(withAlpha(SR_BG_NORMAL, 255), withAlpha(SR_BG_HOVER, 255), hoverAnimation);
        fill(ms, x, y + offsetY, x + width, y + height + offsetY, withAlpha(bgColor, a));

        // Верхняя фиолетовая линия-акцент
        int topLineAlpha = (int) ((80 + 175 * hoverAnimation) * alpha);
        fill(ms, x, y + offsetY, x + width, y + 1 + offsetY, withAlpha(SR_PURPLE, topLineAlpha));

        // Боковые границы — тонкие, серые, ярче при наведении
        int borderAlpha = (int) ((40 + 60 * hoverAnimation) * alpha);
        int borderColor = blendColors(withAlpha(SR_BORDER, 255), withAlpha(SR_PURPLE_BRIGHT, 255), hoverAnimation * 0.5f);
        fill(ms, x, y + offsetY, x + 1, y + height + offsetY, withAlpha(borderColor, borderAlpha));
        fill(ms, x + width - 1, y + offsetY, x + width, y + height + offsetY, withAlpha(borderColor, borderAlpha));

        // Нижняя граница
        fill(ms, x, y + height - 1 + offsetY, x + width, y + height + offsetY,
                withAlpha(SR_BORDER, (int) (50 * alpha)));

        // Текст — белый при наведении, серый в обычном состоянии
        String text = getMessage().getString();
        int textColor = blendColors(withAlpha(SR_TEXT_NORMAL, 255), withAlpha(SR_TEXT_HOVER, 255), hoverAnimation);

        // Тень текста
        mc.font.draw(ms, text,
                x + (width - mc.font.width(text)) / 2f + 1,
                y + (height - 8) / 2f + 1 + offsetY,
                withAlpha(0x000000, (int) (80 * alpha)));

        // Основной текст
        mc.font.draw(ms, text,
                x + (width - mc.font.width(text)) / 2f,
                y + (height - 8) / 2f + offsetY,
                withAlpha(textColor & 0x00FFFFFF, (int) (255 * alpha)));

        ms.popPose();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        clickAnimation = 1f;
        super.onClick(mouseX, mouseY);
    }
}
