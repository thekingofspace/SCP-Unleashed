package net.rai.scp_unleashed.gear.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

public class GearArmorModel extends HumanoidModel<LivingEntity> {
    public GearArmorModel(ModelPart Root) {
        super(Root);
    }

    public static MeshDefinition createEmptyMesh() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return Mesh;
    }
}
