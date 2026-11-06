package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.rai.scp_unleashed.scp902.Scp902Copy;

public class Scp902CopyBodyRenderer extends LivingEntityRenderer<Scp902Copy, PlayerModel<Scp902Copy>> {
    public Scp902CopyBodyRenderer(EntityRendererProvider.Context Context, boolean Thin) {
        super(Context, new PlayerModel<>(Context.bakeLayer(Thin ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), Thin), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(Context.bakeLayer(Thin ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(Context.bakeLayer(Thin ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)), Context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, Context.getItemInHandRenderer()));
        addLayer(new CustomHeadLayer<>(this, Context.getModelSet(), Context.getItemInHandRenderer()));
        addLayer(new ElytraLayer<>(this, Context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(Scp902Copy Copy) {
        return Scp902CopyRenderer.skinOf(Copy).texture();
    }

    @Override
    public void render(Scp902Copy Copy, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        PlayerModel<Scp902Copy> Body = getModel();
        Body.setAllVisible(true);
        Body.crouching = false;
        HumanoidModel.ArmPose Main = Copy.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose Off = Copy.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        if (Copy.getMainArm() == HumanoidArm.RIGHT) {
            Body.rightArmPose = Main;
            Body.leftArmPose = Off;
        } else {
            Body.rightArmPose = Off;
            Body.leftArmPose = Main;
        }
        super.render(Copy, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected boolean shouldShowName(Scp902Copy Copy) {
        return false;
    }

    @Override
    protected void scale(Scp902Copy Copy, PoseStack Pose, float PartialTick) {
        Pose.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
