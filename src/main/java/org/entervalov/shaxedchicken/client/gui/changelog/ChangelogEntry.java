package org.entervalov.shaxedchicken.client.gui.changelog;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nonnull;

import static org.entervalov.shaxedchicken.utils.RenderUtils.*;

/**
 * Saints Row IV style changelog entry.
 * Clean lines, purple accent on hover, minimal decoration.
 */
public class ChangelogEntry extends ExtendedList.AbstractListEntry<ChangelogEntry> {

    // SR4 palette
    private static final int SR_PURPLE = 0x7B2FBE;
    private static final int SR_PURPLE_BRIGHT = 0x9B4FDE;
    private static final int SR_PURPLE_DARK = 0x4A1A72;
    private static final int SR_GRAY_DARK = 0x1A1A1E;

    private final ITextComponent text;
    private float hoverAnimation = 0f;
    private float appearAnimation = 0f;
    private float alpha = 1f;
    private final EntryType type;

    public enum EntryType { CATEGORY, DIVIDER, EMPTY, NORMAL }

    public ChangelogEntry(ITextComponent textComponent) {
        String raw = textComponent.getString();

        if (raw.trim().isEmpty()) {
            this.type = EntryType.EMPTY;
            this.text = textComponent;
        } else if (raw.startsWith("\u0000DIVIDER\u0000")) {
            this.type = EntryType.DIVIDER;
            this.text = new StringTextComponent("");
        } else if (raw.startsWith("\u0000CATEGORY\u0000")) {
            this.type = EntryType.CATEGORY;
            String cleanText = raw.replace("\u0000CATEGORY\u0000", "");
            this.text = new StringTextComponent(cleanText).setStyle(textComponent.getStyle());
        } else {
            this.type = EntryType.NORMAL;
            this.text = textComponent;
        }
    }

    public void setAlpha(float alpha) { this.alpha = alpha; }

    @Override
    public void render(@Nonnull MatrixStack ms, int index, int top, int left, int width, int height,
                       int mouseX, int mouseY, boolean isHovered, float partialTicks) {
        if (alpha <= 0.01f) return;

        if (appearAnimation < 1f) appearAnimation = Math.min(1f, appearAnimation + 0.1f);
        float smoothedAppear = easeOutCubic(Math.max(0, appearAnimation));
        float entryAlpha = alpha * smoothedAppear;
        if (entryAlpha <= 0.05f) return;

        float targetHover = (isHovered && type == EntryType.NORMAL) ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.15f;

        // SR4 — минимальный сдвиг при появлении, без бокового смещения при наведении
        int totalOffsetX = (int) ((1f - smoothedAppear) * 10);

        RenderSystem.disableTexture();
        switch (type) {
            case CATEGORY: renderCategory(ms, top, left + totalOffsetX, width, height, entryAlpha); break;
            case DIVIDER: renderDivider(ms, top, left, width, entryAlpha); break;
            case NORMAL: renderNormalBg(ms, top, left + totalOffsetX, width, height, entryAlpha); break;
            case EMPTY: break;
        }
        RenderSystem.enableTexture();

        // Рендер текста
        int textColor = 0xFFFFFF | ((int) (255 * entryAlpha) << 24);
        Minecraft mc = Minecraft.getInstance();

        if (type == EntryType.CATEGORY) {
            // Заголовок категории — слева с отступом, как в SR4
            mc.font.drawShadow(ms, this.text, left + 8 + totalOffsetX, top + 2, textColor);
        } else if (type == EntryType.NORMAL) {
            mc.font.drawShadow(ms, this.text, left + totalOffsetX, top + 2, textColor);
        }
    }

    private void renderNormalBg(MatrixStack ms, int top, int left, int width, int height, float alpha) {
        if (hoverAnimation > 0.01f) {
            float hoverAlpha = hoverAnimation * alpha;

            // SR4 стиль: при наведении — тёмная подсветка с фиолетовой полосой слева
            int bgAlpha = (int) (30 * hoverAlpha);
            fill(ms, left - 10, top - 1, left + width + 10, top + height + 1,
                    withAlpha(SR_PURPLE_DARK, bgAlpha));

            // Фиолетовая вертикальная полоса слева (фирменный SR4 элемент)
            int barAlpha = (int) (255 * hoverAlpha);
            fill(ms, left - 10, top, left - 7, top + height,
                    withAlpha(SR_PURPLE, barAlpha));
        }
    }

    private void renderCategory(MatrixStack ms, int top, int left, int width, int height, float alpha) {
        // SR4 стиль: горизонтальная линия под заголовком + фиолетовый акцент
        int bgAlpha = (int) (40 * alpha);
        fill(ms, left, top - 2, left + width, top + height + 2, withAlpha(SR_GRAY_DARK, bgAlpha));

        // Фиолетовая полоса слева от заголовка категории
        int accentAlpha = (int) (220 * alpha);
        fill(ms, left, top - 2, left + 3, top + height + 2, withAlpha(SR_PURPLE, accentAlpha));

        // Тонкая линия под заголовком
        int lineAlpha = (int) (120 * alpha);
        fill(ms, left + 8, top + height + 2, left + width - 8, top + height + 3,
                withAlpha(SR_PURPLE_DARK, lineAlpha));
    }

    private void renderDivider(MatrixStack ms, int top, int left, int width, float alpha) {
        int lineY = top + 5;
        int lineAlpha = (int) (40 * alpha);

        // Простая горизонтальная линия
        fill(ms, left + 8, lineY, left + width - 8, lineY + 1,
                withAlpha(SR_GRAY_DARK, lineAlpha));

        // Маленький фиолетовый акцент слева
        int accentAlpha = (int) (160 * alpha);
        fill(ms, left + 8, lineY - 1, left + 30, lineY + 2,
                withAlpha(SR_PURPLE_DARK, accentAlpha));
    }

    private float easeOutCubic(float t) {
        t = Math.max(0, Math.min(1, t));
        return 1f - (float) Math.pow(1 - t, 3);
    }
}
