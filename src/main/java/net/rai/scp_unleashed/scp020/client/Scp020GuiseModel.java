package net.rai.scp_unleashed.scp020.client;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

public class Scp020GuiseModel extends HumanoidModel<AbstractClientPlayer> {
    public Scp020GuiseModel(ModelPart Root) {
        super(Root);
    }

    @Override
    public void setupAnim(AbstractClientPlayer Subject, float LimbSwing, float LimbSwingAmount, float AgeInTicks, float NetHeadYaw, float HeadPitch) {
        super.setupAnim(Subject, LimbSwing, LimbSwingAmount, AgeInTicks, NetHeadYaw, HeadPitch);
        AnimationUtils.animateZombieArms(leftArm, rightArm, false, attackTime, AgeInTicks);
    }
}
