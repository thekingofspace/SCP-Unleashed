package net.rai.scp_unleashed.scp093;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class Scp093TorsoMove extends MoveControl {
    public static final float StallAngle = 80.0f;
    public static final float DriveBias = 0.25f;
    public static final double Arrived = 2.5000003E-7;

    private final Scp093Torso Torso;

    public Scp093TorsoMove(Scp093Torso Torso) {
        super(Torso);
        this.Torso = Torso;
    }

    public static float drive(float Error) {
        float Left = Math.abs(Error);
        if (Left >= StallAngle) {
            return 0.0f;
        }
        return Mth.clamp(Mth.cos(Left * Mth.DEG_TO_RAD) * (1.0f + DriveBias) - DriveBias, 0.0f, 1.0f);
    }

    @Override
    public void tick() {
        if (operation != Operation.MOVE_TO) {
            operation = Operation.WAIT;
            Torso.setZza(0.0f);
            return;
        }
        operation = Operation.WAIT;
        double Dx = wantedX - Torso.getX();
        double Dz = wantedZ - Torso.getZ();
        double Dy = wantedY - Torso.getY();
        if (Dx * Dx + Dy * Dy + Dz * Dz < Arrived) {
            Torso.setZza(0.0f);
            return;
        }
        float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        Torso.setYRot(Torso.getYRot() + Scp093Torso.turnStep(Mth.wrapDegrees(Wanted - Torso.getYRot()), Scp093Torso.BodyTurn));
        float Drive = drive(Mth.wrapDegrees(Wanted - Torso.getYRot()));
        Torso.setSpeed((float) (speedModifier * Torso.getAttributeValue(Attributes.MOVEMENT_SPEED)) * Drive);
    }
}
