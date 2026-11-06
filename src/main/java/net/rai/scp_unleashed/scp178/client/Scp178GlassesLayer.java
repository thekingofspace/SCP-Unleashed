package net.rai.scp_unleashed.scp178.client;

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

public final class Scp178GlassesLayer {
    public static final ModelLayerLocation Location = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_glasses"), "main");
    private static HumanoidModel<LivingEntity> Baked;

    private Scp178GlassesLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(4, 9).addBox(-5.0F, -5.7F, -5.05F, 10.0F, 1.0F, 1.0F, new CubeDeformation(0.1F, -0.1F, -0.27F))
                .texOffs(26, 9).addBox(1.05F, -3.15F, -5.05F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.05F, -0.15F, -0.27F))
                .texOffs(36, 9).addBox(-5.05F, -3.15F, -5.05F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.05F, -0.15F, -0.27F))
                .texOffs(60, 0).addBox(4.0F, -4.9F, -5.05F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.1F, -0.35F))
                .texOffs(0, 9).addBox(-5.0F, -4.9F, -5.05F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.1F, -0.35F))
                .texOffs(46, 9).addBox(-1.0F, -5.0F, -5.05F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.35F))
                .texOffs(36, 0).addBox(1.0F, -4.9F, -5.05F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.1F, 0.0F, -0.43F))
                .texOffs(44, 0).addBox(-4.0F, -4.9F, -5.05F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.1F, 0.0F, -0.43F))
                .texOffs(0, 0).addBox(4.26F, -5.8F, -4.45F, 1.0F, 1.0F, 8.0F, new CubeDeformation(-0.34F, -0.3F, 0.05F))
                .texOffs(52, 0).addBox(4.26F, -5.45F, 2.7F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.42F, -0.25F, -0.2F))
                .texOffs(18, 0).addBox(-5.26F, -5.8F, -4.45F, 1.0F, 1.0F, 8.0F, new CubeDeformation(-0.34F, -0.3F, 0.05F))
                .texOffs(56, 0).addBox(-5.26F, -5.45F, 2.7F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.42F, -0.25F, -0.2F)), PartPose.offset(0.0F, 0.0F, 0.0F));
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
