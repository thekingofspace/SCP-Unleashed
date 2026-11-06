package net.rai.scp_unleashed.scp323.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp323.Scp323Registry;

public final class Scp323HornShape {
    public static final ModelLayerLocation LayerLocation = new ModelLayerLocation(Scp323Registry.location("scp323_horns"), "main");
    public static final ResourceLocation Texture = Scp323Registry.location("textures/entity/scp323/horns.png");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 32;

    private Scp323HornShape() {
    }

    public static LayerDefinition create() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("burr_a_left", CubeListBuilder.create().texOffs(10, 0).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(-2.6f, -8.5f, 0.4f, 0.0f, -0.7854f, 0.0f));
        Root.addOrReplaceChild("burr_b_left", CubeListBuilder.create().texOffs(23, 0).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 1.0f, 3.0f, new CubeDeformation(-0.1f)),
                PartPose.offsetAndRotation(-2.6f, -8.3f, 0.4f, 0.0f, 0.0f, 0.0f));
        Root.addOrReplaceChild("beam_1_left", CubeListBuilder.create().texOffs(38, 6).addBox(-3.4f, -0.5f, -0.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.25f)),
                PartPose.offsetAndRotation(-2.6f, -8.8f, 0.4f, 0.0f, 0.0f, 0.3927f));
        Root.addOrReplaceChild("beam_2_left", CubeListBuilder.create().texOffs(0, 11).addBox(-2.4f, -0.5f, -0.5f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.25f)),
                PartPose.offsetAndRotation(-5.5102f, -10.0055f, 0.4f, 0.0f, 0.0f, 0.7854f));
        Root.addOrReplaceChild("beam_3_left", CubeListBuilder.create().texOffs(18, 6).addBox(-0.5f, -2.2f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)),
                PartPose.offsetAndRotation(-6.9951f, -11.4904f, 0.4f, 0.0f, 0.0f, 0.0f));
        Root.addOrReplaceChild("tine_brow_left", CubeListBuilder.create().texOffs(0, 6).addBox(-0.5f, -0.5f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.2f)),
                PartPose.offsetAndRotation(-3.3276f, -9.1014f, 0.4f, 0.0f, -0.3927f, 0.0f));
        Root.addOrReplaceChild("tine_mid_left", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5f, -3.4f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)),
                PartPose.offsetAndRotation(-4.7827f, -9.7041f, 0.4f, 0.3927f, 0.0f, 0.0f));
        Root.addOrReplaceChild("tine_crown_left", CubeListBuilder.create().texOffs(28, 6).addBox(-0.5f, -1.5f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.15f)),
                PartPose.offsetAndRotation(-6.9951f, -12.8904f, 0.4f, 0.0f, 0.0f, -0.7854f));
        Root.addOrReplaceChild("burr_a_right", CubeListBuilder.create().texOffs(36, 0).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)),
                PartPose.offsetAndRotation(2.6f, -8.5f, 0.4f, 0.0f, -0.7854f, 0.0f));
        Root.addOrReplaceChild("burr_b_right", CubeListBuilder.create().texOffs(49, 0).addBox(-1.5f, -0.5f, -1.5f, 3.0f, 1.0f, 3.0f, new CubeDeformation(-0.1f)),
                PartPose.offsetAndRotation(2.6f, -8.3f, 0.4f, 0.0f, 0.0f, 0.0f));
        Root.addOrReplaceChild("beam_1_right", CubeListBuilder.create().texOffs(49, 6).addBox(-0.6f, -0.5f, -0.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.25f)),
                PartPose.offsetAndRotation(2.6f, -8.8f, 0.4f, 0.0f, 0.0f, -0.3927f));
        Root.addOrReplaceChild("beam_2_right", CubeListBuilder.create().texOffs(9, 11).addBox(-0.6f, -0.5f, -0.5f, 3.0f, 1.0f, 1.0f, new CubeDeformation(0.25f)),
                PartPose.offsetAndRotation(5.5102f, -10.0055f, 0.4f, 0.0f, 0.0f, -0.7854f));
        Root.addOrReplaceChild("beam_3_right", CubeListBuilder.create().texOffs(23, 6).addBox(-0.5f, -2.2f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)),
                PartPose.offsetAndRotation(6.9951f, -11.4904f, 0.4f, 0.0f, 0.0f, 0.0f));
        Root.addOrReplaceChild("tine_brow_right", CubeListBuilder.create().texOffs(9, 6).addBox(-0.5f, -0.5f, -2.5f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.2f)),
                PartPose.offsetAndRotation(3.3276f, -9.1014f, 0.4f, 0.0f, 0.3927f, 0.0f));
        Root.addOrReplaceChild("tine_mid_right", CubeListBuilder.create().texOffs(5, 0).addBox(-0.5f, -3.4f, -0.5f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)),
                PartPose.offsetAndRotation(4.7827f, -9.7041f, 0.4f, 0.3927f, 0.0f, 0.0f));
        Root.addOrReplaceChild("tine_crown_right", CubeListBuilder.create().texOffs(33, 6).addBox(-0.5f, -1.5f, -0.5f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.15f)),
                PartPose.offsetAndRotation(6.9951f, -12.8904f, 0.4f, 0.0f, 0.0f, 0.7854f));
        return LayerDefinition.create(Mesh, TextureWidth, TextureHeight);
    }
}
