package org.entervalov.shaxedchicken;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ModDamageSource extends DamageSource {

    private final Entity damageSourceEntity;

    public ModDamageSource(String name, @Nullable Entity source) {
        super(name);
        this.damageSourceEntity = source;
        this.setExplosion();
    }

    @Override
    @Nullable
    public Entity getEntity() {
        return this.damageSourceEntity;
    }

    @Override
    public ITextComponent getLocalizedDeathMessage(@Nonnull LivingEntity victim) {
        String key = "death.attack." + this.msgId;

        if (this.damageSourceEntity != null) {
            return new TranslationTextComponent(key + ".player",
                    victim.getDisplayName(),
                    this.damageSourceEntity.getDisplayName());
        }

        return new TranslationTextComponent(key, victim.getDisplayName());
    }

    // Статический метод для создания
    public static DamageSource droneExplosion(Entity drone) {
        return new ModDamageSource("drone", drone);
    }
}