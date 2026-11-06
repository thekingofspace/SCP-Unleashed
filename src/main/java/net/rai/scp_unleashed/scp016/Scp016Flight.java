package net.rai.scp_unleashed.scp016;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class Scp016Flight extends MoveControl {
    public static final double Accel = 0.05;
    public static final double Brake = 0.3;
    public static final double Creep = 0.02;
    public static final double Arrive = 0.15;
    public static final float Turn = 14.0f;
    public static final float Climb = 45.0f;
    private final Scp016Flyer Flyer;

    public Scp016Flight(Scp016Flyer Flyer) {
        super(Flyer);
        this.Flyer = Flyer;
    }

    @Override
    public void tick() {
        if (Flyer.isPerched()) {
            if (operation == Operation.MOVE_TO) {
                Flyer.takeOff();
            }
            mob.setZza(0.0f);
            mob.setYya(0.0f);
            mob.setSpeed(0.0f);
            return;
        }
        if (Flyer.steering()) {
            operation = Operation.WAIT;
            return;
        }
        if (operation != Operation.MOVE_TO) {
            mob.setZza(0.0f);
            mob.setYya(0.0f);
            Flyer.hover();
            return;
        }
        operation = Operation.WAIT;
        Vec3 To = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
        double Distance = To.length();
        if (Distance < Arrive) {
            Flyer.hover();
            return;
        }
        double Top = mob.getAttributeValue(Attributes.FLYING_SPEED) * speedModifier;
        double Want = Math.min(Top, Distance * Brake + Creep);
        steer(To.scale(Want / Distance), Accel);
    }

    public void steer(Vec3 Desired, double Limit) {
        Vec3 Current = mob.getDeltaMovement();
        Vec3 Change = Desired.subtract(Current);
        double Length = Change.length();
        if (Length > Limit) {
            Change = Change.scale(Limit / Length);
        }
        Vec3 Next = Current.add(Change);
        mob.setDeltaMovement(Next);
        face(Next);
    }

    public void face(Vec3 Motion) {
        if (Motion.horizontalDistanceSqr() > 1.0E-4) {
            float Yaw = (float) (Mth.atan2(Motion.z, Motion.x) * Mth.RAD_TO_DEG) - 90.0f;
            mob.setYRot(rotlerp(mob.getYRot(), Yaw, Turn));
            mob.yBodyRot = mob.getYRot();
            mob.yHeadRot = mob.getYRot();
        }
        float Pitch = (float) (-Mth.atan2(Motion.y, Math.sqrt(Motion.horizontalDistanceSqr())) * Mth.RAD_TO_DEG);
        mob.setXRot(rotlerp(mob.getXRot(), Mth.clamp(Pitch, -Climb, Climb * 1.6f), 8.0f));
    }
}
