package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;

public class Scp5001MimicModel extends PlayerModel<Scp5001Mimic> {
    public Scp5001MimicModel(ModelPart Root, boolean Slim) {
        super(Root, Slim);
    }

    @Override
    public void setupAnim(Scp5001Mimic Mimic, float Swing, float Amount, float Age, float HeadYaw, float HeadPitch) {
        super.setupAnim(Mimic, Swing, Amount, Age, HeadYaw, HeadPitch);
        float Time = Age + Mimic.getId() * 13.0f;
        boolean Snap = Mth.sin(Time * 0.31f) > 0.965f;
        head.zRot = 0.32f + Mth.sin(Time * 0.045f) * 0.1f + (Snap ? 0.85f : 0.0f);
        head.yRot += Mth.sin(Time * 0.13f) * 0.12f;
        if (Mimic.frozen() || Mimic.Phantom) {
            head.zRot += 0.25f;
            rightArm.xRot = 0.0f;
            leftArm.xRot = 0.0f;
            rightLeg.xRot = 0.0f;
            leftLeg.xRot = 0.0f;
            rightArm.zRot = 0.06f;
            leftArm.zRot = -0.06f;
        } else if (Mimic.isAggressive()) {
            rightArm.xRot = -1.42f + Mth.sin(Time * 0.6f) * 0.08f;
            leftArm.xRot = -1.28f + Mth.cos(Time * 0.53f) * 0.1f;
            rightArm.zRot = -0.12f;
            leftArm.zRot = 0.18f;
            body.xRot = 0.18f;
        } else {
            rightArm.xRot = Mth.cos(Swing * 0.3f) * 0.15f * Amount;
            leftArm.xRot = -Mth.cos(Swing * 0.3f) * 0.15f * Amount;
            rightArm.zRot = 0.08f + Mth.sin(Time * 0.07f) * 0.03f;
            leftArm.zRot = -0.05f;
            rightLeg.xRot *= 0.7f;
            leftLeg.xRot *= 1.25f;
        }
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }
}
