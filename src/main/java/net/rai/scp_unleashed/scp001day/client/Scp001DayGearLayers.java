package net.rai.scp_unleashed.scp001day.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.rai.scp_unleashed.gear.client.GearArmorModel;

public final class Scp001DayGearLayers {
    private Scp001DayGearLayers() {
    }

    public static LayerDefinition scp001DaySunHat() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(32, 17).addBox(-3.5F, -11.0F, -3.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.4F))
                .texOffs(0, 32).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.62F, 0.3F, 0.62F))
                .texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 1.0F, 16.0F, new CubeDeformation(0.0F, 0.05F, 0.0F))
                .texOffs(0, 17).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.75F, 0.2F, 0.75F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition scp001DaySunWraps() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.58F))
                .texOffs(0, 16).addBox(-4.0F, -0.5F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.95F, 0.4F, 0.95F))
                .texOffs(24, 16).addBox(-4.0F, 6.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.82F, 0.18F, 0.82F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.52F))
                .texOffs(0, 23).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.78F, 0.2F, 0.78F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.52F))
                .texOffs(48, 16).addBox(-1.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.78F, 0.2F, 0.78F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scp001DaySunLegWraps() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.38F, 0.2F, 0.38F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.36F, 0.3F, 0.36F))
                .texOffs(16, 12).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.5F, 0.12F, 0.5F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.36F, 0.3F, 0.36F))
                .texOffs(0, 12).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.5F, 0.12F, 0.5F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scp001DaySunBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.0F, 0.6F))
                .texOffs(34, 8).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.85F, 0.15F, 0.85F))
                .texOffs(0, 8).addBox(-2.0F, 11.0F, -3.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.7F, 0.1F, 0.7F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.0F, 0.6F))
                .texOffs(18, 8).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.85F, 0.15F, 0.85F))
                .texOffs(32, 0).addBox(-2.0F, 11.0F, -3.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.7F, 0.1F, 0.7F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }
}
