package net.rai.scp_unleashed.scp008.client;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.rai.scp_unleashed.scp008.Scp008Zombie;

public class Scp008ZombieModel extends PlayerModel<Scp008Zombie> {
    public Scp008ZombieModel(ModelPart Root, boolean Slim) {
        super(Root, Slim);
    }

    @Override
    public void setupAnim(Scp008Zombie Risen, float LimbSwing, float LimbSwingAmount, float AgeInTicks, float HeadYaw, float HeadPitch) {
        super.setupAnim(Risen, LimbSwing, LimbSwingAmount, AgeInTicks, HeadYaw, HeadPitch);
        AnimationUtils.animateZombieArms(leftArm, rightArm, Risen.isAggressive(), attackTime, AgeInTicks);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
