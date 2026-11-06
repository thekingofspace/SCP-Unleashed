package net.rai.scp_unleashed.space.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.rai.scp_unleashed.gear.client.GearArmorModel;
import net.rai.scp_unleashed.space.SpaceSuitPiece;

public final class SpaceSuitLayers {
    private SpaceSuitLayers() {
    }

    public static LayerDefinition create(SpaceSuitPiece Piece) {
        return switch (Piece) {
            case Helmet -> spaceSuitHelmet();
            case Chestplate -> spaceSuitChestplate();
            case Leggings -> spaceSuitLeggings();
            case Boots -> spaceSuitBoots();
        };
    }

    public static LayerDefinition spaceSuitHelmet() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(1.0F))
                .texOffs(32, 0).addBox(-4.5F, -8.0F, -5.3F, 9.0F, 7.0F, 1.0F, new CubeDeformation(0.15F, 0.15F, -0.2F))
                .texOffs(24, 16).addBox(-4.0F, -7.5F, -5.5F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.3F, 0.3F, -0.1F))
                .texOffs(42, 16).addBox(-5.6F, -6.0F, -2.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(52, 0).addBox(4.6F, -6.0F, -2.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-3.0F, -9.0F, -3.0F, 6.0F, 1.0F, 6.0F, new CubeDeformation(0.0F, 0.4F, 0.0F))
                .texOffs(50, 16).addBox(1.5F, -9.9F, 0.5F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition spaceSuitChestplate() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.65F))
                .texOffs(20, 16).addBox(-4.5F, -1.0F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(0.3F, 0.0F, 0.3F))
                .texOffs(48, 28).addBox(-2.5F, 2.0F, -3.15F, 5.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-3.5F, 0.5F, 2.4F, 7.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(-1.0F, -0.4F, 3.4F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.6F))
                .texOffs(0, 28).addBox(-3.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.72F))
                .texOffs(32, 28).addBox(-3.0F, 6.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.85F, 0.2F, 0.85F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.6F))
                .texOffs(48, 16).addBox(-1.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.72F))
                .texOffs(16, 28).addBox(-1.0F, 6.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.85F, 0.2F, 0.85F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition spaceSuitLeggings() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.78F, 0.25F, 0.78F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.45F, 0.2F, 0.45F))
                .texOffs(10, 16).addBox(-2.0F, 3.5F, -2.9F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.1F, 0.0F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.45F, 0.2F, 0.45F))
                .texOffs(0, 16).addBox(-2.0F, 3.5F, -2.9F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.1F, 0.0F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition spaceSuitBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.8F, 0.55F, 0.8F))
                .texOffs(48, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.95F, 0.1F, 0.95F))
                .texOffs(10, 10).addBox(-2.0F, 10.0F, -3.7F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.66F, 0.45F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.8F, 0.55F, 0.8F))
                .texOffs(32, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.95F, 0.1F, 0.95F))
                .texOffs(0, 10).addBox(-2.0F, 10.0F, -3.7F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.66F, 0.45F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }
}
