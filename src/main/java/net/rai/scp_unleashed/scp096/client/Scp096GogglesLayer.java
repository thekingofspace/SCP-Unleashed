package net.rai.scp_unleashed.scp096.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp096GogglesLayer {
    public static final ModelLayerLocation Location = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scramble_goggles"), "main");
    private static HumanoidModel<LivingEntity> Baked;

    private Scp096GogglesLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.6F, 0.1F, 0.6F))
                .texOffs(32, 0).addBox(-6.0F, -6.0F, -5.0F, 12.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 10).addBox(4.0F, -5.5F, -4.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.2F, -0.1F, 0.0F))
                .texOffs(8, 10).addBox(-6.0F, -5.5F, -4.5F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.2F, -0.1F, 0.0F))
                .texOffs(16, 10).addBox(-5.0F, -5.0F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.08F, -0.08F, -0.25F))
                .texOffs(58, 0).addBox(-1.0F, -5.0F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.08F, -0.08F, -0.25F))
                .texOffs(22, 10).addBox(3.0F, -5.0F, -5.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(-0.08F, -0.08F, -0.25F))
                .texOffs(36, 10).addBox(-2.5F, -5.0F, -5.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.12F, -0.12F, -0.3F))
                .texOffs(28, 10).addBox(-2.5F, -4.0F, -5.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.12F, -0.12F, -0.3F))
                .texOffs(40, 10).addBox(1.5F, -5.0F, -5.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.12F, -0.12F, -0.3F))
                .texOffs(32, 10).addBox(1.5F, -4.0F, -5.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(-0.12F, -0.12F, -0.3F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static void bake(EntityModelSet Models) {
        Baked = new HumanoidModel<>(Models.bakeLayer(Location));
    }

    public static HumanoidModel<LivingEntity> model() {
        if (Baked == null) {
            Baked = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(Location));
        }
        return Baked;
    }
}
