package org.entervalov.shaxedchicken.client.renderer.layer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.ChickenModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import org.entervalov.shaxedchicken.entity.AdvancedDroneEntity;
import org.entervalov.shaxedchicken.entity.DroneType;

import javax.annotation.Nonnull;

public class DroneAuraLayer extends LayerRenderer<AdvancedDroneEntity, ChickenModel<AdvancedDroneEntity>> {

    // Используем ванильную текстуру щита крипера
    private static final ResourceLocation POWER_LOCATION = new ResourceLocation("textures/entity/creeper/creeper_armor.png");
    private final ChickenModel<AdvancedDroneEntity> model = new ChickenModel<>();

    public DroneAuraLayer(IEntityRenderer<AdvancedDroneEntity, ChickenModel<AdvancedDroneEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(@Nonnull MatrixStack ms, @Nonnull IRenderTypeBuffer buffer, int light, AdvancedDroneEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {

        DroneType type = entity.getDroneType();

        // Рисуем ауру только для Ядерных (зеленая) и Танков (синяя защита)
        if (!type.isNuclear() && type != DroneType.HEAVY) return;

        float f = (float)entity.tickCount + partialTicks;

        // Модель ауры чуть больше основной
        this.getParentModel().copyPropertiesTo(this.model);
        this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        IVertexBuilder builder = buffer.getBuffer(RenderType.energySwirl(POWER_LOCATION, f * 0.01F, f * 0.01F));

        float r = 0.5F, g = 0.5F, b = 0.5F; // По умолчанию серый
        if (type.isNuclear()) { r = 0.1F; g = 1.0F; b = 0.1F; } // Зеленая радиация
        if (type == DroneType.HEAVY) { r = 0.2F; g = 0.5F; b = 1.0F; } // Синий силовой щит

        ms.pushPose();
        // Масштабируем ауру, чтобы она была вокруг дрона
        float scale = 1.1F;
        ms.scale(scale, scale, scale);

        this.model.renderToBuffer(ms, builder, light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
        ms.popPose();
    }
}