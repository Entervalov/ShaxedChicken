package org.entervalov.shaxedchicken.client.gui.changelog;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.list.ExtendedList;

import javax.annotation.Nonnull;

import static org.entervalov.shaxedchicken.utils.RenderUtils.*;

public class ChangelogList extends ExtendedList<ChangelogEntry> {

    private static final int SR_PURPLE = 0x7B2FBE;
    private static final int SR_PURPLE_DARK = 0x4A1A72;
    private static final int SR_PANEL_BG = 0x0D0D0F;
    private static final int SR_GRAY_DARK = 0x1A1A1E;

    private float alpha = 1f;

    public ChangelogList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
        this.setRenderBackground(false);
        this.setRenderHeader(false, 0);
        this.setRenderTopAndBottom(false);
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void addRow(ChangelogEntry entry) {
        super.addEntry(entry);
    }

    @Override
    public int getRowWidth() {
        return this.width - 35;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.width - 6;
    }

    @Override
    public void renderBackground(@Nonnull MatrixStack ms) {
    }

    @Override
    public void render(@Nonnull MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < getItemCount(); i++) {
            ChangelogEntry entry = getEntry(i);
            entry.setAlpha(alpha);
        }

        super.render(ms, mouseX, mouseY, partialTicks);

        renderScrollbar(ms);
        renderFade(ms);
    }

    private void renderScrollbar(MatrixStack ms) {
        int scrollbarX = this.getScrollbarPosition();
        int scrollbarWidth = 2;

        fill(ms, scrollbarX, y0, scrollbarX + scrollbarWidth, y1,
                withAlpha(SR_GRAY_DARK, (int) (30 * alpha)));

        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            int visibleHeight = y1 - y0;
            int thumbHeight = Math.max(20, (visibleHeight * visibleHeight) / (visibleHeight + maxScroll));
            int thumbY = y0 + (int) ((this.getScrollAmount() * (visibleHeight - thumbHeight)) / maxScroll);

            fill(ms, scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight,
                    withAlpha(SR_PURPLE, (int) (200 * alpha)));
        }
    }

    private void renderFade(MatrixStack ms) {
        int fadeHeight = 12;

        fillGradient(ms, x0, y0, x0 + width - 10, y0 + fadeHeight,
                withAlpha(SR_PANEL_BG, (int) (255 * alpha)),
                withAlpha(SR_PANEL_BG, 0));

        fillGradient(ms, x0, y1 - fadeHeight, x0 + width - 10, y1,
                withAlpha(SR_PANEL_BG, 0),
                withAlpha(SR_PANEL_BG, (int) (255 * alpha)));
    }
}