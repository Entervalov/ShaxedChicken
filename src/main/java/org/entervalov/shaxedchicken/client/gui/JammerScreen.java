package org.entervalov.shaxedchicken.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.entervalov.shaxedchicken.container.JammerContainer;
import org.entervalov.shaxedchicken.block.JammerTileEntity;
import org.entervalov.shaxedchicken.item.JammerModuleItem;
import org.entervalov.shaxedchicken.item.JammerModuleType;

import java.util.ArrayList;
import java.util.List;

public class JammerScreen extends ContainerScreen<JammerContainer> {

    private static final ResourceLocation BG = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private final List<ModuleHover> moduleHovers = new ArrayList<>();

    public JammerScreen(JammerContainer container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        super.render(ms, mouseX, mouseY, partialTicks);
        for (ModuleHover hover : moduleHovers) {
            if (hover.isInside(mouseX, mouseY)) {
                this.renderTooltip(ms, toReordered(hover.lines), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void renderBg(MatrixStack ms, float partialTicks, int mouseX, int mouseY) {
        assert this.minecraft != null;
        this.minecraft.getTextureManager().bind(BG);
        blit(ms, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        JammerTileEntity te = this.menu.getJammer();
        int slotY = this.topPos + 20;
        int slotX = this.leftPos + 62;
        for (int i = 0; i < 3; i++) {
            int x = slotX + i * 18;
            int color = 0xFF444444;
            if (te != null && !te.getItem(i).isEmpty() && te.getItem(i).getItem() instanceof JammerModuleItem) {
                JammerModuleType type = ((JammerModuleItem) te.getItem(i).getItem()).getType();
                color = 0xFF000000 | colorFor(type);
            }
            fill(ms, x - 1, slotY - 1, x + 17, slotY + 17, color);
            fill(ms, x, slotY, x + 16, slotY + 16, 0xFF101010);
        }
    }

    @Override
    protected void renderLabels(MatrixStack ms, int mouseX, int mouseY) {
        JammerTileEntity te = this.menu.getJammer();
        moduleHovers.clear();

        this.font.draw(ms, "EW JAMMER // MODULES", 6, 6, 0xE0E0E0);
        this.font.draw(ms, this.inventory.getDisplayName().getString(), 6, this.inventoryLabelY, 0xA0A0A0);

        int y = 20;
        this.font.draw(ms, "Installed:", 6, y, 0xAAAAAA);
        y += 10;

        boolean any = false;
        for (int i = 0; i < 3; i++) {
            if (te == null) break;
            if (!te.getItem(i).isEmpty() && te.getItem(i).getItem() instanceof JammerModuleItem) {
                JammerModuleItem item = (JammerModuleItem) te.getItem(i).getItem();
                JammerModuleType type = item.getType();
                String line = "- " + type.getDisplayName();
                this.font.draw(ms, line, 6, y, colorFor(type));
                moduleHovers.add(new ModuleHover(
                        this.leftPos + 6,
                        this.topPos + y,
                        this.font.width(line),
                        9,
                        getModuleTooltip(type)
                ));
                y += 9;
                any = true;
            }
        }
        if (!any) {
            this.font.draw(ms, "None", 6, y, 0x777777);
            y += 9;
        }

        if (te != null) {
            y += 6;
            this.font.draw(ms, "Stats:", 6, y, 0xAAAAAA);
            y += 10;
            this.font.draw(ms, String.format("Jammer: %.1fm", te.getJammerRadius()), 6, y, 0x66CCFF);
            y += 9;
            this.font.draw(ms, String.format("Laser: %.1fm", te.getLaserRange()), 6, y, 0xFF6666);
            y += 9;
            this.font.draw(ms, "Warmup: " + te.getWarmupTime() + "t", 6, y, 0xDDDD88);
            y += 9;
            this.font.draw(ms, "Targets: " + te.getMaxTargets(), 6, y, 0x88DD88);
            y += 9;
            this.font.draw(ms, "Jam Dur: " + te.getJammerDurationTicks() + "t", 6, y, 0x66DDFF);
            y += 9;
            String weather = te.isWeatherBad() ? "Bad" : "OK";
            this.font.draw(ms, "Weather: " + weather, 6, y, 0xAAAAAA);
            if (te.hasModuleType(JammerModuleType.LEGENDARY)) {
                y += 9;
                this.font.draw(ms, "Weather Penalty: OFF", 6, y, 0xFFCC33);
            }
        }
    }

    private int colorFor(JammerModuleType type) {
        switch (type) {
            case BASIC: return 0xAAAAAA;
            case UNCOMMON: return 0x55FF55;
            case EPIC: return 0xCC66FF;
            case LEGENDARY: return 0xFFCC33;
            default: return 0xFFFFFF;
        }
    }

    private List<ITextComponent> getModuleTooltip(JammerModuleType type) {
        List<ITextComponent> lines = new ArrayList<>();
        lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.header", type.getDisplayName()));
        lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.install_hint"));
        switch (type) {
            case BASIC:
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.basic.1"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.basic.2"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.basic.3"));
                break;
            case UNCOMMON:
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.uncommon.1"));
                break;
            case EPIC:
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.epic.1"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.epic.2"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.epic.3"));
                break;
            case LEGENDARY:
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.1"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.2"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.3"));
                lines.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.4"));
                break;
        }
        return lines;
    }

    private static class ModuleHover {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final List<ITextComponent> lines;

        private ModuleHover(int x, int y, int w, int h, List<ITextComponent> lines) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.lines = lines;
        }

        private boolean isInside(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private List<net.minecraft.util.IReorderingProcessor> toReordered(List<ITextComponent> lines) {
        List<net.minecraft.util.IReorderingProcessor> out = new ArrayList<>();
        for (ITextComponent line : lines) {
            out.add(line.getVisualOrderText());
        }
        return out;
    }
}
