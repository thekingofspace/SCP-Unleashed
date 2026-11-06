package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.rai.scp_unleashed.gear.client.GearArmorModel;

public final class Scp4666GearLayers {
    private Scp4666GearLayers() {
    }

    public static LayerDefinition scp4666WinterHat() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.55F, 0.45F, 0.55F))
                .texOffs(32, 0).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.9F, 0.2F, 0.9F))
                .texOffs(0, 11).addBox(-1.5F, -12.0F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.15F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scp4666WinterCoat() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 14.0F, 4.0F, new CubeDeformation(0.55F))
                .texOffs(0, 18).addBox(-4.0F, -1.0F, -2.0F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.85F, 0.35F, 0.85F))
                .texOffs(56, 0).addBox(-1.0F, 3.0F, -3.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F, 0.1F, 0.0F))
                .texOffs(0, 24).addBox(-1.0F, 6.0F, -3.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F, 0.1F, 0.0F))
                .texOffs(56, 18).addBox(-1.0F, 9.0F, -3.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F, 0.1F, 0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(40, 18).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.8F, 0.2F, 0.8F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(24, 18).addBox(-1.0F, 7.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.8F, 0.2F, 0.8F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scp4666WinterBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.55F, 0.0F, 0.55F))
                .texOffs(34, 9).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.95F, 0.15F, 0.95F))
                .texOffs(0, 9).addBox(-2.0F, 11.0F, -3.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.7F, 0.1F, 0.7F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.55F, 0.0F, 0.55F))
                .texOffs(18, 9).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.95F, 0.15F, 0.95F))
                .texOffs(32, 0).addBox(-2.0F, 11.0F, -3.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.7F, 0.1F, 0.7F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }
}
