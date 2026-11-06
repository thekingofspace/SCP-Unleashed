package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp035.Scp035Host;

public class Scp035HostModel extends PlayerModel<Scp035Host> {
    public static final float TiltAmount = 0.16f;
    public static final float TiltSpeed = 0.035f;
    public static final float ReachAngle = 0.35f;

    public Scp035HostModel(ModelPart Root, boolean Slim) {
        super(Root, Slim);
    }

    @Override
    public void setupAnim(Scp035Host Host, float LimbSwing, float LimbSwingAmount, float AgeInTicks, float HeadYaw, float HeadPitch) {
        super.setupAnim(Host, LimbSwing, LimbSwingAmount, AgeInTicks, HeadYaw, HeadPitch);
        head.zRot = TiltAmount * (0.6f + 0.4f * Mth.sin(AgeInTicks * TiltSpeed + Host.getId()));
        hat.copyFrom(head);
        if (Host.isAggressive() && attackTime <= 0.0f) {
            rightArm.xRot -= ReachAngle;
            leftArm.xRot -= ReachAngle * 0.7f;
        }
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
