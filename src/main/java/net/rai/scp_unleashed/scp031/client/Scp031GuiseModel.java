package net.rai.scp_unleashed.scp031.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp031.Scp031Mimic;

public class Scp031GuiseModel extends PlayerModel<Scp031Mimic> {
    public static final float HugPitch = -1.32f;
    public static final float HugInward = 0.62f;
    public static final float HugSway = 0.06f;
    public static final float HugTilt = 0.22f;

    public Scp031GuiseModel(ModelPart Root, boolean Slim) {
        super(Root, Slim);
    }

    @Override
    public void setupAnim(Scp031Mimic Mimic, float LimbSwing, float LimbSwingAmount, float AgeInTicks, float HeadYaw, float HeadPitch) {
        super.setupAnim(Mimic, LimbSwing, LimbSwingAmount, AgeInTicks, HeadYaw, HeadPitch);
        head.zRot = 0.0f;
        if (!Mimic.isEmbracing()) {
            hat.copyFrom(head);
            return;
        }
        float Sway = Mth.sin(AgeInTicks * 0.18f) * HugSway;
        rightArm.xRot = HugPitch + Sway;
        leftArm.xRot = HugPitch - Sway;
        rightArm.yRot = -HugInward;
        leftArm.yRot = HugInward;
        rightArm.zRot = 0.08f;
        leftArm.zRot = -0.08f;
        head.zRot = HugTilt;
        hat.copyFrom(head);
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
    }
}
