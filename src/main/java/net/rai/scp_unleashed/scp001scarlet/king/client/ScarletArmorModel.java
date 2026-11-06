package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public class ScarletArmorModel extends HumanoidModel<LivingEntity> {
    public ScarletArmorModel(ModelPart Root) {
        super(Root);
    }

    public static LayerDefinition outer() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Map<String, PartDefinition> Parts = new HashMap<>();
        CubeDeformation Base = new CubeDeformation(1.0F);
        Parts.put("head", Root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, Base), PartPose.offset(0.0F, 0.0F, 0.0F)));
        Parts.put("body", Root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, Base), PartPose.offset(0.0F, 0.0F, 0.0F)));
        Parts.put("right_arm", Root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(-5.0F, 2.0F, 0.0F)));
        Parts.put("left_arm", Root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(5.0F, 2.0F, 0.0F)));
        Parts.put("right_leg", Root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(-1.9F, 12.0F, 0.0F)));
        Parts.put("left_leg", Root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(1.9F, 12.0F, 0.0F)));
        Root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        Parts.put("horn_right", Parts.get("head").addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(92, 56).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(-3.0F, -8.4F, 0.5F, -0.611F, 0.0F, -0.384F)));
        Parts.put("horn_right_b", Parts.get("horn_right").addOrReplaceChild("horn_right_b", CubeListBuilder.create().texOffs(104, 56).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -2.6F, 0.0F, -0.698F, 0.0F, 0.105F)));
        Parts.put("horn_right_c", Parts.get("horn_right_b").addOrReplaceChild("horn_right_c", CubeListBuilder.create().texOffs(112, 56).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, 0.873F, 0.0F, 0.14F)));
        Parts.put("horn_right_d", Parts.get("horn_right_c").addOrReplaceChild("horn_right_d", CubeListBuilder.create().texOffs(16, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, 0.524F, 0.0F, 0.0F)));
        Parts.put("horn_right_tine", Parts.get("horn_right_b").addOrReplaceChild("horn_right_tine", CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.175F, 0.0F, -0.873F)));
        Parts.put("cheek_right", Parts.get("head").addOrReplaceChild("cheek_right", CubeListBuilder.create().texOffs(20, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.6F, -2.5F, -2.0F, -0.436F, 0.0F, -0.349F)));
        Parts.put("cheek_right_b", Parts.get("head").addOrReplaceChild("cheek_right_b", CubeListBuilder.create().texOffs(4, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-4.6F, -4.5F, -0.5F, -0.611F, 0.0F, -0.524F)));
        Parts.put("jaw_right", Parts.get("head").addOrReplaceChild("jaw_right", CubeListBuilder.create().texOffs(8, 44).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, -0.6F, -4.6F, 0.0F, -0.349F, 0.0F)));
        Parts.put("horn_left", Parts.get("head").addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(3.0F, -8.4F, 0.5F, -0.611F, 0.0F, 0.384F)));
        Parts.put("horn_left_b", Parts.get("horn_left").addOrReplaceChild("horn_left_b", CubeListBuilder.create().texOffs(12, 32).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -2.6F, 0.0F, -0.698F, 0.0F, -0.105F)));
        Parts.put("horn_left_c", Parts.get("horn_left_b").addOrReplaceChild("horn_left_c", CubeListBuilder.create().texOffs(20, 32).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, 0.873F, 0.0F, -0.14F)));
        Parts.put("horn_left_d", Parts.get("horn_left_c").addOrReplaceChild("horn_left_d", CubeListBuilder.create().texOffs(24, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -3.6F, 0.0F, 0.524F, 0.0F, 0.0F)));
        Parts.put("horn_left_tine", Parts.get("horn_left_b").addOrReplaceChild("horn_left_tine", CubeListBuilder.create().texOffs(16, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.175F, 0.0F, 0.873F)));
        Parts.put("cheek_left", Parts.get("head").addOrReplaceChild("cheek_left", CubeListBuilder.create().texOffs(28, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(4.6F, -2.5F, -2.0F, -0.436F, 0.0F, 0.349F)));
        Parts.put("cheek_left_b", Parts.get("head").addOrReplaceChild("cheek_left_b", CubeListBuilder.create().texOffs(20, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(4.6F, -4.5F, -0.5F, -0.611F, 0.0F, 0.524F)));
        Parts.put("jaw_left", Parts.get("head").addOrReplaceChild("jaw_left", CubeListBuilder.create().texOffs(24, 44).addBox(-1.5F, -2.0F, -0.5F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(3.0F, -0.6F, -4.6F, 0.0F, 0.349F, 0.0F)));
        Parts.put("crest", Parts.get("head").addOrReplaceChild("crest", CubeListBuilder.create().texOffs(72, 0).addBox(-0.5F, -2.0F, -4.2F, 1.0F, 2.0F, 9.0F), PartPose.offsetAndRotation(0.0F, -8.6F, -1.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("crest_front", Parts.get("head").addOrReplaceChild("crest_front", CubeListBuilder.create().texOffs(32, 38).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -7.5F, -5.0F, 0.436F, 0.0F, 0.0F)));
        Parts.put("visor", Parts.get("head").addOrReplaceChild("visor", CubeListBuilder.create().texOffs(32, 44).addBox(-3.0F, -1.5F, -0.5F, 6.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -4.0F, -5.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("snout", Parts.get("head").addOrReplaceChild("snout", CubeListBuilder.create().texOffs(46, 44).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -1.0F, -5.2F, -0.349F, 0.0F, 0.0F)));
        Parts.put("ridge", Parts.get("body").addOrReplaceChild("ridge", CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, 0.2F, -4.6F, 2.0F, 10.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("collar", Parts.get("body").addOrReplaceChild("collar", CubeListBuilder.create().texOffs(88, 41).addBox(-4.0F, -6.0F, -0.5F, 8.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -1.0F, 2.4F, -0.244F, 0.0F, 0.0F)));
        Parts.put("collar_spikes", Parts.get("collar").addOrReplaceChild("collar_spikes", CubeListBuilder.create().texOffs(28, 52).addBox(-3.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(52, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(32, 52).addBox(2.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, -0.175F, 0.0F, 0.0F)));
        Parts.put("breast", Parts.get("body").addOrReplaceChild("breast", CubeListBuilder.create().texOffs(28, 32).addBox(-4.3F, 0.4F, -3.5F, 4.0F, 5.0F, 1.0F).texOffs(38, 32).addBox(0.3F, 0.4F, -3.5F, 4.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("gorget", Parts.get("body").addOrReplaceChild("gorget", CubeListBuilder.create().texOffs(64, 23).addBox(-4.6F, -1.8F, -3.4F, 9.0F, 2.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("gorget_flare", Parts.get("body").addOrReplaceChild("gorget_flare", CubeListBuilder.create().texOffs(36, 52).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -1.6F, -3.4F, 0.436F, 0.0F, 0.0F)));
        Parts.put("back_spine", Parts.get("body").addOrReplaceChild("back_spine", CubeListBuilder.create().texOffs(92, 0).addBox(-0.5F, 0.5F, -0.4F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("abdomen_0", Parts.get("body").addOrReplaceChild("abdomen_0", CubeListBuilder.create().texOffs(12, 56).addBox(-3.0F, 0.0F, -0.4F, 6.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 6.2F, -3.3F, 0.0F, 0.0F, 0.0F)));
        Parts.put("abdomen_1", Parts.get("body").addOrReplaceChild("abdomen_1", CubeListBuilder.create().texOffs(26, 56).addBox(-2.7F, 0.0F, -0.4F, 6.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 7.7F, -3.3F, 0.0F, 0.0F, 0.0F)));
        Parts.put("abdomen_2", Parts.get("body").addOrReplaceChild("abdomen_2", CubeListBuilder.create().texOffs(40, 56).addBox(-2.4F, 0.0F, -0.4F, 6.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 9.2F, -3.3F, 0.0F, 0.0F, 0.0F)));
        Parts.put("abdomen_3", Parts.get("body").addOrReplaceChild("abdomen_3", CubeListBuilder.create().texOffs(0, 59).addBox(-2.1F, 0.0F, -0.4F, 6.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.7F, -3.3F, 0.0F, 0.0F, 0.0F)));
        Parts.put("spine_spike0", Parts.get("body").addOrReplaceChild("spine_spike0", CubeListBuilder.create().texOffs(56, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 1.5F, 3.4F, 0.698F, 0.0F, 0.0F)));
        Parts.put("spine_spike1", Parts.get("body").addOrReplaceChild("spine_spike1", CubeListBuilder.create().texOffs(60, 44).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.5F, 3.4F, 0.698F, 0.0F, 0.0F)));
        Parts.put("spine_spike2", Parts.get("body").addOrReplaceChild("spine_spike2", CubeListBuilder.create().texOffs(0, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 7.5F, 3.4F, 0.698F, 0.0F, 0.0F)));
        Parts.put("pauldron_right", Parts.get("right_arm").addOrReplaceChild("pauldron_right", CubeListBuilder.create().texOffs(96, 0).addBox(-4.6F, -3.8F, -3.6F, 6.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("pauldron_right_low", Parts.get("right_arm").addOrReplaceChild("pauldron_right_low", CubeListBuilder.create().texOffs(96, 23).addBox(-2.0F, -1.0F, -3.2F, 4.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(-1.0F, 0.6F, 0.0F, 0.0F, 0.0F, -0.314F)));
        Parts.put("pauldron_right_rim", Parts.get("right_arm").addOrReplaceChild("pauldron_right_rim", CubeListBuilder.create().texOffs(90, 32).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(-1.6F, 2.4F, 0.0F, 0.0F, 0.0F, -0.489F)));
        Parts.put("pauldron_right_blade0", Parts.get("right_arm").addOrReplaceChild("pauldron_right_blade0", CubeListBuilder.create().texOffs(108, 32).addBox(-1.0F, -7.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-0.8F, -3.6F, -2.6F, -0.314F, 0.0F, -0.209F)));
        Parts.put("pauldron_right_blade0_tip", Parts.get("pauldron_right_blade0").addOrReplaceChild("pauldron_right_blade0_tip", CubeListBuilder.create().texOffs(4, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.6F, 0.0F, 0.0F, 0.0F, 0.314F)));
        Parts.put("pauldron_right_blade1", Parts.get("right_arm").addOrReplaceChild("pauldron_right_blade1", CubeListBuilder.create().texOffs(96, 12).addBox(-1.0F, -9.0F, -0.5F, 2.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(-0.8F, -3.6F, -0.9F, -0.105F, 0.0F, -0.384F)));
        Parts.put("pauldron_right_blade1_tip", Parts.get("pauldron_right_blade1").addOrReplaceChild("pauldron_right_blade1_tip", CubeListBuilder.create().texOffs(8, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, 0.0F, 0.0F, 0.314F)));
        Parts.put("pauldron_right_blade2", Parts.get("right_arm").addOrReplaceChild("pauldron_right_blade2", CubeListBuilder.create().texOffs(122, 0).addBox(-1.0F, -10.0F, -0.5F, 2.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-0.8F, -3.6F, 0.9F, 0.105F, 0.0F, -0.559F)));
        Parts.put("pauldron_right_blade2_tip", Parts.get("pauldron_right_blade2").addOrReplaceChild("pauldron_right_blade2_tip", CubeListBuilder.create().texOffs(12, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -9.6F, 0.0F, 0.0F, 0.0F, 0.314F)));
        Parts.put("pauldron_right_blade3", Parts.get("right_arm").addOrReplaceChild("pauldron_right_blade3", CubeListBuilder.create().texOffs(116, 23).addBox(-1.0F, -8.0F, -0.5F, 2.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-0.8F, -3.6F, 2.6F, 0.314F, 0.0F, -0.768F)));
        Parts.put("pauldron_right_blade3_tip", Parts.get("pauldron_right_blade3").addOrReplaceChild("pauldron_right_blade3_tip", CubeListBuilder.create().texOffs(16, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -7.6F, 0.0F, 0.0F, 0.0F, 0.314F)));
        Parts.put("elbow_right", Parts.get("right_arm").addOrReplaceChild("elbow_right", CubeListBuilder.create().texOffs(20, 48).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F).texOffs(28, 48).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.5F, 2.4F, 1.047F, 0.0F, 0.0F)));
        Parts.put("vambrace_right", Parts.get("right_arm").addOrReplaceChild("vambrace_right", CubeListBuilder.create().texOffs(106, 41).addBox(-3.6F, 5.0F, -2.6F, 5.0F, 2.0F, 5.0F).texOffs(64, 49).addBox(-3.6F, 7.5F, -2.6F, 5.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("vambrace_right_fin", Parts.get("right_arm").addOrReplaceChild("vambrace_right_fin", CubeListBuilder.create().texOffs(84, 49).addBox(-0.5F, -3.0F, -1.5F, 1.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(-2.6F, 6.5F, 0.0F, 0.0F, 0.0F, 0.349F)));
        Parts.put("claws_right", Parts.get("right_arm").addOrReplaceChild("claws_right", CubeListBuilder.create().texOffs(32, 48).addBox(-1.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(36, 48).addBox(0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, 10.2F, -2.2F, -0.349F, 0.0F, 0.0F)));
        Parts.put("pauldron_left", Parts.get("left_arm").addOrReplaceChild("pauldron_left", CubeListBuilder.create().texOffs(64, 12).addBox(-1.4F, -3.8F, -3.6F, 6.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("pauldron_left_low", Parts.get("left_arm").addOrReplaceChild("pauldron_left_low", CubeListBuilder.create().texOffs(64, 32).addBox(-2.0F, -1.0F, -3.2F, 4.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(1.0F, 0.6F, 0.0F, 0.0F, 0.0F, 0.314F)));
        Parts.put("pauldron_left_rim", Parts.get("left_arm").addOrReplaceChild("pauldron_left_rim", CubeListBuilder.create().texOffs(64, 41).addBox(-1.5F, -0.5F, -3.0F, 3.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(1.6F, 2.4F, 0.0F, 0.0F, 0.0F, 0.489F)));
        Parts.put("pauldron_left_blade0", Parts.get("left_arm").addOrReplaceChild("pauldron_left_blade0", CubeListBuilder.create().texOffs(82, 41).addBox(-1.0F, -7.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.8F, -3.6F, -2.6F, -0.314F, 0.0F, 0.209F)));
        Parts.put("pauldron_left_blade0_tip", Parts.get("pauldron_left_blade0").addOrReplaceChild("pauldron_left_blade0_tip", CubeListBuilder.create().texOffs(40, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.6F, 0.0F, 0.0F, 0.0F, -0.314F)));
        Parts.put("pauldron_left_blade1", Parts.get("left_arm").addOrReplaceChild("pauldron_left_blade1", CubeListBuilder.create().texOffs(102, 12).addBox(-1.0F, -9.0F, -0.5F, 2.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.8F, -3.6F, -0.9F, -0.105F, 0.0F, 0.384F)));
        Parts.put("pauldron_left_blade1_tip", Parts.get("pauldron_left_blade1").addOrReplaceChild("pauldron_left_blade1_tip", CubeListBuilder.create().texOffs(44, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, 0.0F, 0.0F, -0.314F)));
        Parts.put("pauldron_left_blade2", Parts.get("left_arm").addOrReplaceChild("pauldron_left_blade2", CubeListBuilder.create().texOffs(90, 12).addBox(-1.0F, -10.0F, -0.5F, 2.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(0.8F, -3.6F, 0.9F, 0.105F, 0.0F, 0.559F)));
        Parts.put("pauldron_left_blade2_tip", Parts.get("pauldron_left_blade2").addOrReplaceChild("pauldron_left_blade2_tip", CubeListBuilder.create().texOffs(48, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -9.6F, 0.0F, 0.0F, 0.0F, -0.314F)));
        Parts.put("pauldron_left_blade3", Parts.get("left_arm").addOrReplaceChild("pauldron_left_blade3", CubeListBuilder.create().texOffs(84, 32).addBox(-1.0F, -8.0F, -0.5F, 2.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.8F, -3.6F, 2.6F, 0.314F, 0.0F, 0.768F)));
        Parts.put("pauldron_left_blade3_tip", Parts.get("pauldron_left_blade3").addOrReplaceChild("pauldron_left_blade3_tip", CubeListBuilder.create().texOffs(52, 48).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -7.6F, 0.0F, 0.0F, 0.0F, -0.314F)));
        Parts.put("elbow_left", Parts.get("left_arm").addOrReplaceChild("elbow_left", CubeListBuilder.create().texOffs(56, 48).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F).texOffs(0, 52).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.5F, 2.4F, 1.047F, 0.0F, 0.0F)));
        Parts.put("vambrace_left", Parts.get("left_arm").addOrReplaceChild("vambrace_left", CubeListBuilder.create().texOffs(92, 49).addBox(-1.4F, 5.0F, -2.6F, 5.0F, 2.0F, 5.0F).texOffs(64, 56).addBox(-1.4F, 7.5F, -2.6F, 5.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("vambrace_left_fin", Parts.get("left_arm").addOrReplaceChild("vambrace_left_fin", CubeListBuilder.create().texOffs(84, 56).addBox(-0.5F, -3.0F, -1.5F, 1.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(2.6F, 6.5F, 0.0F, 0.0F, 0.0F, -0.349F)));
        Parts.put("claws_left", Parts.get("left_arm").addOrReplaceChild("claws_left", CubeListBuilder.create().texOffs(4, 52).addBox(-1.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(8, 52).addBox(0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(1.0F, 10.2F, -2.2F, -0.349F, 0.0F, 0.0F)));
        Parts.put("sabaton_right", Parts.get("right_leg").addOrReplaceChild("sabaton_right", CubeListBuilder.create().texOffs(48, 32).addBox(-2.6F, 9.6F, -4.4F, 5.0F, 3.0F, 3.0F).texOffs(50, 52).addBox(-2.3F, 7.6F, -3.2F, 5.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("toe_right", Parts.get("right_leg").addOrReplaceChild("toe_right", CubeListBuilder.create().texOffs(36, 38).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 11.6F, -4.4F, 0.209F, 0.0F, 0.0F)));
        Parts.put("shin_right", Parts.get("right_leg").addOrReplaceChild("shin_right", CubeListBuilder.create().texOffs(46, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 7.6F, -3.4F, -0.524F, 0.0F, 0.0F)));
        Parts.put("heel_right", Parts.get("right_leg").addOrReplaceChild("heel_right", CubeListBuilder.create().texOffs(12, 52).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.4F, 2.6F, 0.96F, 0.0F, 0.0F)));
        Parts.put("ankle_right", Parts.get("right_leg").addOrReplaceChild("ankle_right", CubeListBuilder.create().texOffs(16, 52).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.7F, 9.0F, 0.0F, 0.0F, 0.0F, 0.436F)));
        Parts.put("sabaton_left", Parts.get("left_leg").addOrReplaceChild("sabaton_left", CubeListBuilder.create().texOffs(0, 38).addBox(-2.6F, 9.6F, -4.4F, 5.0F, 3.0F, 3.0F).texOffs(0, 56).addBox(-2.3F, 7.6F, -3.2F, 5.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("toe_left", Parts.get("left_leg").addOrReplaceChild("toe_left", CubeListBuilder.create().texOffs(50, 38).addBox(-1.0F, -1.0F, -2.5F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 11.6F, -4.4F, 0.209F, 0.0F, 0.0F)));
        Parts.put("shin_left", Parts.get("left_leg").addOrReplaceChild("shin_left", CubeListBuilder.create().texOffs(60, 38).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 7.6F, -3.4F, -0.524F, 0.0F, 0.0F)));
        Parts.put("heel_left", Parts.get("left_leg").addOrReplaceChild("heel_left", CubeListBuilder.create().texOffs(20, 52).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.4F, 2.6F, 0.96F, 0.0F, 0.0F)));
        Parts.put("ankle_left", Parts.get("left_leg").addOrReplaceChild("ankle_left", CubeListBuilder.create().texOffs(24, 52).addBox(-0.5F, -2.5F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.7F, 9.0F, 0.0F, 0.0F, 0.0F, -0.436F)));
        return LayerDefinition.create(Mesh, 128, 64);
    }

    public static LayerDefinition inner() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = Mesh.getRoot();
        Map<String, PartDefinition> Parts = new HashMap<>();
        CubeDeformation Base = new CubeDeformation(0.5F);
        Parts.put("head", Root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, Base), PartPose.offset(0.0F, 0.0F, 0.0F)));
        Parts.put("body", Root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, Base), PartPose.offset(0.0F, 0.0F, 0.0F)));
        Parts.put("right_arm", Root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(-5.0F, 2.0F, 0.0F)));
        Parts.put("left_arm", Root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror().texOffs(40, 16).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(5.0F, 2.0F, 0.0F)));
        Parts.put("right_leg", Root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(-1.9F, 12.0F, 0.0F)));
        Parts.put("left_leg", Root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, Base), PartPose.offset(1.9F, 12.0F, 0.0F)));
        Root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        Parts.put("fauld", Parts.get("body").addOrReplaceChild("fauld", CubeListBuilder.create().texOffs(64, 10).addBox(-4.6F, 8.6F, -2.7F, 9.0F, 2.0F, 5.0F).texOffs(92, 10).addBox(-4.3F, 10.4F, -2.9F, 9.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)));
        Parts.put("front_tasset", Parts.get("body").addOrReplaceChild("front_tasset", CubeListBuilder.create().texOffs(64, 0).addBox(-2.0F, 0.0F, -0.5F, 4.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.0F, -2.9F, -0.105F, 0.0F, 0.0F)));
        Parts.put("front_tasset_tip", Parts.get("front_tasset").addOrReplaceChild("front_tasset_tip", CubeListBuilder.create().texOffs(112, 17).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, -0.105F, 0.0F, 0.0F)));
        Parts.put("back_tasset", Parts.get("body").addOrReplaceChild("back_tasset", CubeListBuilder.create().texOffs(94, 0).addBox(-2.5F, 0.0F, -0.5F, 5.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 2.9F, 0.14F, 0.0F, 0.0F)));
        Parts.put("tasset_right", Parts.get("right_leg").addOrReplaceChild("tasset_right", CubeListBuilder.create().texOffs(64, 17).addBox(-2.5F, -0.5F, -0.6F, 5.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.6F, -0.209F, 0.0F, -0.105F)));
        Parts.put("thigh_right", Parts.get("right_leg").addOrReplaceChild("thigh_right", CubeListBuilder.create().texOffs(74, 0).addBox(-0.5F, -1.0F, -2.0F, 1.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(-2.6F, 0.5F, 0.0F, 0.0F, 0.0F, 0.14F)));
        Parts.put("knee_right", Parts.get("right_leg").addOrReplaceChild("knee_right", CubeListBuilder.create().texOffs(88, 17).addBox(-1.5F, -1.0F, -0.6F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 5.5F, -2.7F, 0.0F, 0.0F, 0.0F)));
        Parts.put("knee_right_spike", Parts.get("right_leg").addOrReplaceChild("knee_right_spike", CubeListBuilder.create().texOffs(96, 17).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 5.0F, -3.2F, -0.873F, 0.0F, 0.0F)));
        Parts.put("tasset_left", Parts.get("left_leg").addOrReplaceChild("tasset_left", CubeListBuilder.create().texOffs(76, 17).addBox(-2.5F, -0.5F, -0.6F, 5.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.6F, -0.209F, 0.0F, 0.105F)));
        Parts.put("thigh_left", Parts.get("left_leg").addOrReplaceChild("thigh_left", CubeListBuilder.create().texOffs(84, 0).addBox(-0.5F, -1.0F, -2.0F, 1.0F, 6.0F, 4.0F), PartPose.offsetAndRotation(2.6F, 0.5F, 0.0F, 0.0F, 0.0F, -0.14F)));
        Parts.put("knee_left", Parts.get("left_leg").addOrReplaceChild("knee_left", CubeListBuilder.create().texOffs(100, 17).addBox(-1.5F, -1.0F, -0.6F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 5.5F, -2.7F, 0.0F, 0.0F, 0.0F)));
        Parts.put("knee_left_spike", Parts.get("left_leg").addOrReplaceChild("knee_left_spike", CubeListBuilder.create().texOffs(108, 17).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 5.0F, -3.2F, -0.873F, 0.0F, 0.0F)));
        return LayerDefinition.create(Mesh, 128, 64);
    }
}
