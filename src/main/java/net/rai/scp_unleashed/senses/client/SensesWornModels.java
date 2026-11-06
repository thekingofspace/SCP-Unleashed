package net.rai.scp_unleashed.senses.client;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.senses.SensesRegistry;

public final class SensesWornModels {
    public static final ModelLayerLocation BlindfoldLayer = new ModelLayerLocation(SensesRegistry.location(SensesRegistry.BlindfoldId), "worn");
    public static final ModelLayerLocation EarplugsLayer = new ModelLayerLocation(SensesRegistry.location(SensesRegistry.EarplugsId), "worn");
    public static final ResourceLocation BlindfoldTexture = SensesRegistry.location("textures/entity/senses/blindfold.png");
    public static final ResourceLocation EarplugsTexture = SensesRegistry.location("textures/entity/senses/earplugs.png");

    private SensesWornModels() {
    }

    public static LayerDefinition blindfold() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("band", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -5.0f, -4.0f, 8.0f, 2.0f, 8.0f, new CubeDeformation(0.35f, 0.05f, 0.35f))
                .texOffs(0, 10).addBox(-1.0f, -5.0f, 4.35f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)), PartPose.ZERO);
        Root.addOrReplaceChild("left_tail", CubeListBuilder.create()
                .texOffs(8, 10).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(-0.1f, 0.0f, -0.3f)),
                PartPose.offsetAndRotation(0.6f, -3.1f, 4.95f, 0.18f, 0.0f, -0.22f));
        Root.addOrReplaceChild("right_tail", CubeListBuilder.create()
                .texOffs(12, 10).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(-0.1f, 0.0f, -0.3f)),
                PartPose.offsetAndRotation(-0.6f, -3.1f, 4.95f, 0.12f, 0.0f, 0.3f));
        return LayerDefinition.create(Mesh, 32, 16);
    }

    public static LayerDefinition earplugs() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("plugs", CubeListBuilder.create()
                .texOffs(0, 0).addBox(4.0f, -2.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.12f))
                .texOffs(0, 2).addBox(-5.0f, -2.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.12f)), PartPose.ZERO);
        return LayerDefinition.create(Mesh, 16, 16);
    }

    public static ModelPart bake(EntityModelSet Models, ModelLayerLocation Layer) {
        return Models.bakeLayer(Layer);
    }
}
