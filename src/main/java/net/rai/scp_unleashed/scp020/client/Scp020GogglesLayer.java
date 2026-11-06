package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp020GogglesLayer {
    public static final ModelLayerLocation Location = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020_hyperspectral_goggles"), "main");
    private static HumanoidModel<LivingEntity> Baked;

    private Scp020GogglesLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.6F, 0.1F, 0.6F))
                .texOffs(0, 10).addBox(-5.0F, -5.5F, -5.0F, 10.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(44, 0).addBox(-4.5F, -6.0F, -6.5F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(32, 0).addBox(0.5F, -6.0F, -6.5F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(34, 10).addBox(-3.5F, -5.0F, -7.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.25F))
                .texOffs(28, 10).addBox(1.5F, -5.0F, -7.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.25F))
                .texOffs(40, 10).addBox(-0.5F, -5.0F, -5.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.1F, 0.0F, 0.0F))
                .texOffs(44, 10).addBox(1.5F, -6.75F, -6.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(22, 10).addBox(-5.75F, -5.0F, -5.25F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(4.75F, -5.0F, -5.25F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
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
