package net.rai.scp_unleashed.scp268.client;

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
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp268.Scp268Registry;

public final class Scp268CapLayer {
    public static final ModelLayerLocation Location = new ModelLayerLocation(Scp268Registry.location(Scp268Registry.CapId), "main");
    private static HumanoidModel<LivingEntity> Baked;

    private Scp268CapLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -6.8F, -4.5F, 9.0F, 2.0F, 9.0F, new CubeDeformation(0.12F, -0.2F, 0.12F))
                .texOffs(0, 11).addBox(-5.0F, -8.625F, -2.05F, 10.0F, 2.0F, 7.0F, new CubeDeformation(-0.1F, 0.225F, -0.05F))
                .texOffs(36, 0).addBox(-5.0F, -8.575F, -5.925F, 10.0F, 3.0F, 4.0F, new CubeDeformation(-0.22F, 0.025F, 0.025F))
                .texOffs(34, 11).addBox(-4.0F, -5.925F, -6.65F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F, -0.275F, 0.25F))
                .texOffs(54, 11).addBox(-0.5F, -9.45F, 0.05F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F, -0.3F, 0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
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
