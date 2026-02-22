package org.entervalov.shaxedchicken.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class JammerModuleItem extends Item {

    private final JammerModuleType type;

    public JammerModuleItem(JammerModuleType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public JammerModuleType getType() {
        return type;
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return type == JammerModuleType.EPIC || type == JammerModuleType.LEGENDARY;
    }

    @Override
    @Nonnull
    public Rarity getRarity(@Nonnull ItemStack stack) {
        switch (type) {
            case UNCOMMON: return Rarity.UNCOMMON;
            case EPIC: return Rarity.EPIC;
            case LEGENDARY: return Rarity.EPIC;
            default: return Rarity.COMMON;
        }
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world,
                                @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.header", type.getDisplayName())
                .withStyle(type.getColor()));
        tooltip.add(new StringTextComponent("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"));
        tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.install_hint").withStyle(TextFormatting.GRAY));
        tooltip.add(new StringTextComponent(""));
        String prefix = type.getColor().toString();
        switch (type) {
            case BASIC:
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.basic.1").withStyle(TextFormatting.AQUA));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.basic.2").withStyle(TextFormatting.AQUA));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.basic.3").withStyle(TextFormatting.AQUA));
                break;
            case UNCOMMON:
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.uncommon.1").withStyle(TextFormatting.GREEN));
                break;
            case EPIC:
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.epic.1").withStyle(TextFormatting.LIGHT_PURPLE));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.epic.2").withStyle(TextFormatting.LIGHT_PURPLE));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.epic.3").withStyle(TextFormatting.LIGHT_PURPLE));
                break;
            case LEGENDARY:
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.1").withStyle(TextFormatting.GOLD));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.2").withStyle(TextFormatting.GOLD));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.3").withStyle(TextFormatting.GOLD));
                tooltip.add(new TranslationTextComponent("item.shaxedchicken.jammer_module.legendary.4").withStyle(TextFormatting.GOLD));
                break;
        }
    }
}
