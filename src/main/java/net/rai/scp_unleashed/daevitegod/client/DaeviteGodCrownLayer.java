package net.rai.scp_unleashed.daevitegod.client;

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

public final class DaeviteGodCrownLayer {
    public static final ModelLayerLocation Location = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_god_crown"), "main");
    private static HumanoidModel<LivingEntity> Baked;

    private DaeviteGodCrownLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(7, 11).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(30, 11).addBox(-5.0F, -9.0F, 4.0F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-5.0F, -9.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(19, 0).addBox(4.0F, -9.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(38, 0).addBox(-1.0F, -13.0F, -5.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(53, 11).addBox(-4.0F, -11.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(58, 11).addBox(3.0F, -11.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(45, 0).addBox(-5.0F, -12.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(52, 0).addBox(4.0F, -12.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 11).addBox(-1.0F, -12.0F, 4.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-5.0F, -10.5F, 3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(5, 16).addBox(4.0F, -10.5F, 3.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.05F))
                .texOffs(10, 16).addBox(-0.5F, -11.5F, -5.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.05F)), PartPose.ZERO);
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
