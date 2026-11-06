package net.rai.scp_unleashed.scp049.client;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.rai.scp_unleashed.scp049.Scp049Cured;

public class Scp049CuredModel extends PlayerModel<Scp049Cured> {
    public Scp049CuredModel(ModelPart Root, boolean Slim) {
        super(Root, Slim);
    }

    @Override
    public void setupAnim(Scp049Cured Risen, float LimbSwing, float LimbSwingAmount, float AgeInTicks, float HeadYaw, float HeadPitch) {
        super.setupAnim(Risen, LimbSwing, LimbSwingAmount, AgeInTicks, HeadYaw, HeadPitch);
        AnimationUtils.animateZombieArms(leftArm, rightArm, Risen.isAggressive(), attackTime, AgeInTicks);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
