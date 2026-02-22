package org.entervalov.shaxedchicken.entity;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public enum DroneType {
    BASIC(
            "Шахед-Курица",
            TextFormatting.WHITE,
            25.0, 1.0, 5.0F, 1.0F,
            false, false, false, false,
            10
    ),
    SPEED(
            "Скоростная Курица",
            TextFormatting.AQUA,
            15.0, 2.5, 2.5F, 0.7F,
            false, false, false, false,
            8
    ),
    HEAVY(
            "Тяжёлая Курица",
            TextFormatting.DARK_GRAY,
            60.0, 0.5, 8.0F, 1.8F,
            false, false, false, false,
            5
    ),
    SCOUT(
            "Курица-Разведчик",
            TextFormatting.YELLOW,
            20.0, 1.8, 0.0F, 0.6F,
            true, false, false, false,
            4
    ),
    FIRE(
            "Огненная Курица",
            TextFormatting.RED,
            25.0, 1.0, 4.0F, 1.0F,
            false, true, false, false,
            5
    ),
    NIGHT(
            "Ночной Охотник",
            TextFormatting.DARK_PURPLE,
            30.0, 1.3, 6.0F, 1.0F,
            false, false, true, false,
            4
    ),
    NUCLEAR(
            "Термоядерная Курица",
            TextFormatting.LIGHT_PURPLE,
            100.0, 0.7, 15.0F, 2.5F,
            false, false, false, true,
            1  // очень редкий
    );

    private final String displayName;
    private final TextFormatting color;
    private final double health;
    private final double speedMultiplier;
    private final float explosionPower;
    private final float sizeMultiplier;
    private final boolean isScout;
    private final boolean isFire;
    private final boolean isNight;
    private final boolean isNuclear;
    private final int spawnWeight;

    DroneType(String displayName, TextFormatting color,
              double health, double speedMultiplier, float explosionPower, float sizeMultiplier,
              boolean isScout, boolean isFire, boolean isNight, boolean isNuclear,
              int spawnWeight) {
        this.displayName = displayName;
        this.color = color;
        this.health = health;
        this.speedMultiplier = speedMultiplier;
        this.explosionPower = explosionPower;
        this.sizeMultiplier = sizeMultiplier;
        this.isScout = isScout;
        this.isFire = isFire;
        this.isNight = isNight;
        this.isNuclear = isNuclear;
        this.spawnWeight = spawnWeight;
    }

    public String getDisplayName() { return displayName; }
    public TextFormatting getColor() { return color; }
    public double getHealth() { return health; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public float getExplosionPower() { return explosionPower; }
    public float getSizeMultiplier() { return sizeMultiplier; }
    public boolean isScout() { return isScout; }
    public boolean isFire() { return isFire; }
    public boolean isNight() { return isNight; }
    public boolean isNuclear() { return isNuclear; }
    public int getSpawnWeight() { return spawnWeight; }

    public ITextComponent getFormattedName() {
        return new StringTextComponent(displayName).withStyle(color);
    }

    public static DroneType getRandomType(java.util.Random random) {
        int totalWeight = 0;
        for (DroneType type : values()) {
            totalWeight += type.spawnWeight;
        }

        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (DroneType type : values()) {
            currentWeight += type.spawnWeight;
            if (roll < currentWeight) {
                return type;
            }
        }

        return BASIC;
    }

    public static DroneType fromId(int id) {
        if (id >= 0 && id < values().length) {
            return values()[id];
        }
        return BASIC;
    }
}
