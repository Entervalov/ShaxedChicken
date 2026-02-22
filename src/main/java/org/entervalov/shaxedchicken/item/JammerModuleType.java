package org.entervalov.shaxedchicken.item;

import net.minecraft.util.text.TextFormatting;

public enum JammerModuleType {
    BASIC("Basic", TextFormatting.GRAY),
    UNCOMMON("Uncommon", TextFormatting.GREEN),
    EPIC("Epic", TextFormatting.LIGHT_PURPLE),
    LEGENDARY("Legendary", TextFormatting.GOLD);

    private final String displayName;
    private final TextFormatting color;

    JammerModuleType(String displayName, TextFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextFormatting getColor() {
        return color;
    }
}
