package org.entervalov.shaxedchicken.client.gui.changelog;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.entervalov.shaxedchicken.utils.UpdateChecker;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static org.entervalov.shaxedchicken.utils.RenderUtils.*;

/**
 * Saints Row IV style changelog panel.
 * Black background, purple/white color scheme, sharp geometric lines,
 * aggressive rectangular shapes, clean minimalist design.
 */
public class ChangelogPanel extends AbstractGui {

    private final Minecraft mc;
    private ChangelogList list;
    private AnimatedButton closeButton;

    private int x, y, width, height;
    private boolean visible = true;
    private boolean closing = false;

    // === Анимации ===
    private float openAnimation = 0f;
    private float targetAnimation = 1f;
    private float glowPhase = 0f;
    private float scanlineOffset = 0f;
    private long lastTime = System.currentTimeMillis();

    // Saints Row IV color palette
    private static final int SR_BLACK = 0x000000;
    private static final int SR_DARK = 0x0A0A0A;
    private static final int SR_PANEL_BG = 0x0D0D0F;
    private static final int SR_PURPLE = 0x7B2FBE;
    private static final int SR_PURPLE_BRIGHT = 0x9B4FDE;
    private static final int SR_PURPLE_DARK = 0x4A1A72;
    private static final int SR_WHITE = 0xFFFFFF;
    private static final int SR_GRAY_LIGHT = 0xCCCCCC;
    private static final int SR_GRAY = 0x888888;
    private static final int SR_GRAY_DARK = 0x222222;
    private static final int SR_STRIPE = 0x1A1A1E;

    public ChangelogPanel(Minecraft mc, int screenWidth, int screenHeight) {
        this.mc = mc;
        init(screenWidth, screenHeight);
    }

    public void init(int screenWidth, int screenHeight) {
        this.width = 400;
        this.height = Math.min(460, screenHeight - 40);
        this.x = (screenWidth - width) / 2;
        this.y = (screenHeight - height) / 2;

        int listTop = y + 62;
        int listBottom = y + height - 48;

        List<ITextComponent> lines = ChangelogParser.loadChangelog(width - 50);

        this.list = new ChangelogList(mc, width, height - 110, listTop, listBottom, 14);
        this.list.setLeftPos(this.x);

        for (ITextComponent line : lines) {
            this.list.addRow(new ChangelogEntry(line));
        }

        int btnWidth = 160;
        this.closeButton = new AnimatedButton(
                x + (width - btnWidth) / 2,
                y + height - 40,
                btnWidth, 26,
                new StringTextComponent("CONTINUE"),
                b -> startClosing()
        );
    }

    private void startClosing() {
        this.closing = true;
        this.targetAnimation = 0f;
    }

    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        updateAnimations();

        if (closing && openAnimation <= 0.01f) {
            visible = false;
            closing = false;
            UpdateChecker.markAsRead();
            return;
        }

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        ms.pushPose();
        ms.translate(0, 0, 500);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Затемнение фона — чёрное, как в SR4
        RenderSystem.disableTexture();
        float alpha = openAnimation;
        int fullScreenAlpha = (int) (230 * alpha);
        fill(ms, 0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                withAlpha(SR_BLACK, fullScreenAlpha));
        RenderSystem.enableTexture();

        // Анимация — резкое появление, без bouncing (SR4 стиль — чёткие линии)
        float scale = easeOutQuart(openAnimation);

        ms.pushPose();
        int centerX = x + width / 2;
        int centerY = y + height / 2;

        ms.translate(centerX, centerY, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-centerX, -centerY, 0);

        RenderSystem.disableTexture();

        // Основной фон панели
        renderBackground(ms, alpha);

        // Фиолетовые акцентные полосы (SR4 signature)
        renderPurpleAccents(ms, alpha);

        // Горизонтальные сканлайны (едва видимые, как в SR4 меню)
        renderScanlines(ms, alpha);

        RenderSystem.enableTexture();

        // Заголовок
        renderHeader(ms, alpha);

        // Верхний разделитель
        RenderSystem.disableTexture();
        renderDivider(ms, y + 54, alpha);
        RenderSystem.enableTexture();

        // Список изменений
        if (alpha > 0.1f) {
            list.setAlpha(alpha);
            enableScissor(x, y + 62, x + width, y + height - 48);
            list.render(ms, mouseX, mouseY, partialTicks);
            disableScissor();
        }

        // Нижний разделитель
        RenderSystem.disableTexture();
        renderDivider(ms, y + height - 50, alpha);
        RenderSystem.enableTexture();

        // Кнопка
        closeButton.setAlpha(alpha);
        closeButton.render(ms, mouseX, mouseY, partialTicks);

        // Угловые акценты (SR4 — резкие L-образные уголки)
        RenderSystem.disableTexture();
        renderCornerAccents(ms, alpha);
        RenderSystem.enableTexture();

        ms.popPose(); // Scale pop
        ms.popPose(); // Z-offset pop

        RenderSystem.disableBlend();

        RenderSystem.enableTexture();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void enableScissor(int x1, int y1, int x2, int y2) {
        double scale = mc.getWindow().getGuiScale();
        int screenHeight = mc.getWindow().getHeight();
        int scX = (int) (x1 * scale);
        int scW = Math.max(0, (int) ((x2 - x1) * scale));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scX, Math.max(0, screenHeight - (int) (y2 * scale)), scW, (int) ((y2 - y1) * scale));
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void renderHeader(MatrixStack ms, float alpha) {
        String title = "UPDATE // LOG";
        String version = "v" + UpdateChecker.getCurrentVersion();

        // Подложка заголовка — тёмная полоса
        RenderSystem.disableTexture();
        int headerBg = (int) (180 * alpha);
        fill(ms, x, y, x + width, y + 54, withAlpha(SR_DARK, headerBg));

        // Фиолетовая полоса слева от заголовка (SR4 accent bar)
        int accentAlpha = (int) (255 * alpha);
        fill(ms, x, y + 8, x + 4, y + 46, withAlpha(SR_PURPLE, accentAlpha));
        RenderSystem.enableTexture();

        // Заголовок — белый, жирный, слева
        int titleColor = withAlpha(SR_WHITE, (int) (255 * alpha));
        mc.font.drawShadow(ms, title, x + 14, y + 12, titleColor);

        // Версия — серая, ниже
        int versionColor = withAlpha(SR_GRAY, (int) (200 * alpha));
        mc.font.drawShadow(ms, version, x + 14, y + 28, versionColor);

        // Декоративная фиолетовая точка справа
        RenderSystem.disableTexture();
        int dotAlpha = (int) (255 * alpha * (0.6f + 0.4f * (float) Math.sin(glowPhase)));
        fill(ms, x + width - 18, y + 14, x + width - 10, y + 22, withAlpha(SR_PURPLE_BRIGHT, dotAlpha));
        RenderSystem.enableTexture();
    }

    private void renderDivider(MatrixStack ms, int yPos, float alpha) {
        // Простая горизонтальная линия — SR4 стиль (никаких градиентов к центру)
        int lineAlpha = (int) (100 * alpha);
        fill(ms, x + 4, yPos, x + width - 4, yPos + 1, withAlpha(SR_GRAY_DARK, lineAlpha));

        // Фиолетовый акцент на левой стороне линии
        int accentAlpha = (int) (200 * alpha);
        fill(ms, x + 4, yPos, x + 60, yPos + 1, withAlpha(SR_PURPLE, accentAlpha));
    }

    private void renderCornerAccents(MatrixStack ms, float alpha) {
        // SR4 стиль — жёсткие L-образные уголки фиолетового цвета
        int a = (int) (220 * alpha);
        int size = 20;
        int thickness = 2;

        // Верхний левый
        fill(ms, x, y, x + size, y + thickness, withAlpha(SR_PURPLE, a));
        fill(ms, x, y, x + thickness, y + size, withAlpha(SR_PURPLE, a));

        // Верхний правый
        fill(ms, x + width - size, y, x + width, y + thickness, withAlpha(SR_PURPLE, a));
        fill(ms, x + width - thickness, y, x + width, y + size, withAlpha(SR_PURPLE, a));

        // Нижний левый
        fill(ms, x, y + height - thickness, x + size, y + height, withAlpha(SR_PURPLE, a));
        fill(ms, x, y + height - size, x + thickness, y + height, withAlpha(SR_PURPLE, a));

        // Нижний правый
        fill(ms, x + width - size, y + height - thickness, x + width, y + height, withAlpha(SR_PURPLE, a));
        fill(ms, x + width - thickness, y + height - size, x + width, y + height, withAlpha(SR_PURPLE, a));
    }

    private void renderBackground(MatrixStack ms, float alpha) {
        int a = (int) (245 * alpha);
        // Основной чёрный фон панели
        fill(ms, x, y, x + width, y + height, withAlpha(SR_PANEL_BG, a));

        // Тонкая рамка серого цвета по периметру
        int borderAlpha = (int) (60 * alpha);
        fill(ms, x, y, x + width, y + 1, withAlpha(SR_GRAY_DARK, borderAlpha));
        fill(ms, x, y + height - 1, x + width, y + height, withAlpha(SR_GRAY_DARK, borderAlpha));
        fill(ms, x, y, x + 1, y + height, withAlpha(SR_GRAY_DARK, borderAlpha));
        fill(ms, x + width - 1, y, x + width, y + height, withAlpha(SR_GRAY_DARK, borderAlpha));
    }

    private void renderPurpleAccents(MatrixStack ms, float alpha) {
        // Вертикальная фиолетовая полоса слева (фирменный SR4 элемент)
        int accentAlpha = (int) (40 * alpha);
        fill(ms, x + 1, y + 1, x + 3, y + height - 1, withAlpha(SR_PURPLE_DARK, accentAlpha));

        // Горизонтальная тонкая фиолетовая линия сверху
        int topLineAlpha = (int) (180 * alpha);
        fill(ms, x, y, x + width, y + 2, withAlpha(SR_PURPLE, topLineAlpha));

        // Фиолетовый градиент внизу — едва заметный
        int bottomGlow = (int) (20 * alpha);
        fillGradient(ms, x, y + height - 30, x + width, y + height,
                withAlpha(SR_PURPLE_DARK, 0), withAlpha(SR_PURPLE_DARK, bottomGlow));
    }

    private void renderScanlines(MatrixStack ms, float alpha) {
        // Горизонтальные сканлайны — едва видимые полосы как в SR4 интерфейсе
        int scanAlpha = (int) (8 * alpha);
        if (scanAlpha <= 0) return;
        int color = withAlpha(SR_STRIPE, scanAlpha);
        int offset = (int) scanlineOffset % 4;
        for (int sy = y + offset; sy < y + height; sy += 4) {
            fill(ms, x + 1, sy, x + width - 1, sy + 1, color);
        }
    }

    private void updateAnimations() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 1000f;
        lastTime = currentTime;

        float speed = closing ? 6f : 4f;
        if (openAnimation < targetAnimation) openAnimation = Math.min(targetAnimation, openAnimation + deltaTime * speed);
        else if (openAnimation > targetAnimation) openAnimation = Math.max(targetAnimation, openAnimation - deltaTime * speed);

        glowPhase += deltaTime * 2.5f;
        if (glowPhase > Math.PI * 2) glowPhase -= (float) (Math.PI * 2);

        scanlineOffset += deltaTime * 8f;
        if (scanlineOffset > 400f) scanlineOffset -= 400f;
    }

    private float easeOutQuart(float t) {
        return 1f - (float) Math.pow(1 - t, 4);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!visible || closing) return false;
        if (closeButton.isMouseOver(mouseX, mouseY)) {
            closeButton.onClick(mouseX, mouseY);
            return true;
        }
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible) list.mouseScrolled(mouseX, mouseY, delta);
    }

    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            this.closing = false;
            this.openAnimation = 0f;
            this.targetAnimation = 1f;
        } else {
            this.openAnimation = 0f;
            this.closing = false;
        }
    }
}

