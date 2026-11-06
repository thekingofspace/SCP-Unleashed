package net.rai.scp_unleashed.scp001scarlet.king.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletArmorItem;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class ScarletArmorGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public static final ResourceLocation OuterGlow = ScarletKingRegistry.location("textures/models/armor/scarlet_model_outer_glow.png");
    public static final ResourceLocation InnerGlow = ScarletKingRegistry.location("textures/models/armor/scarlet_model_inner_glow.png");
    public static final int Tint = 0xC0FFFFFF;
    private static final EquipmentSlot[] Slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ScarletArmorGlowLayer(RenderLayerParent<T, M> Parent) {
        super(Parent);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, T Owner, float LimbSwing, float LimbSwingAmount, float PartialTick, float AgeInTicks,
                       float NetHeadYaw, float HeadPitch) {
        if (Owner.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        HumanoidModel<T> Parent = getParentModel();
        for (EquipmentSlot Slot : Slots) {
            if (!(Owner.getItemBySlot(Slot).getItem() instanceof ScarletArmorItem)) {
                continue;
            }
            boolean Inner = Slot == EquipmentSlot.LEGS;
            ScarletArmorModel Model = Inner ? ScarletArmorModels.inner() : ScarletArmorModels.outer();
            Model.head.copyFrom(Parent.head);
            Model.hat.copyFrom(Parent.hat);
            Model.body.copyFrom(Parent.body);
            Model.rightArm.copyFrom(Parent.rightArm);
            Model.leftArm.copyFrom(Parent.leftArm);
            Model.rightLeg.copyFrom(Parent.rightLeg);
            Model.leftLeg.copyFrom(Parent.leftLeg);
            Model.setAllVisible(false);
            switch (Slot) {
                case HEAD -> Model.head.visible = true;
                case CHEST -> {
                    Model.body.visible = true;
                    Model.rightArm.visible = true;
                    Model.leftArm.visible = true;
                }
                case LEGS -> {
                    Model.body.visible = true;
                    Model.rightLeg.visible = true;
                    Model.leftLeg.visible = true;
                }
                default -> {
                    Model.rightLeg.visible = true;
                    Model.leftLeg.visible = true;
                }
            }
            Model.renderToBuffer(Pose, Buffers.getBuffer(RenderType.eyes(Inner ? InnerGlow : OuterGlow)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, Tint);
        }
    }
}
