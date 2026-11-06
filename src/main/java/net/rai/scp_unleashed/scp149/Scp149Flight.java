package net.rai.scp_unleashed.scp149;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class Scp149Flight extends MoveControl {
    public static final double Accel = 0.045;
    public static final double Brake = 0.35;
    public static final double Creep = 0.02;
    public static final double Arrive = 0.08;
    public static final float Turn = 28.0f;
    private final Scp149Fly Fly;

    public Scp149Flight(Scp149Fly Fly) {
        super(Fly);
        this.Fly = Fly;
    }

    @Override
    public void tick() {
        if (operation != Operation.MOVE_TO) {
            mob.setZza(0.0f);
            mob.setYya(0.0f);
            Fly.wobble();
            Fly.dryGuard();
            return;
        }
        operation = Operation.WAIT;
        Vec3 To = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
        double Distance = To.length();
        if (Distance < Arrive) {
            Fly.wobble();
            Fly.dryGuard();
            return;
        }
        double Top = Fly.cruise() * speedModifier;
        double Want = Math.min(Top, Distance * Brake + Creep);
        Vec3 Desired = To.scale(Want / Distance);
        Vec3 Current = mob.getDeltaMovement();
        Vec3 Steer = Desired.subtract(Current);
        double Length = Steer.length();
        if (Length > Accel) {
            Steer = Steer.scale(Accel / Length);
        }
        mob.setDeltaMovement(Current.add(Steer));
        if (!Fly.facesQuarry() && To.horizontalDistanceSqr() > 1.0e-4) {
            float Yaw = (float) (Mth.atan2(To.z, To.x) * Mth.RAD_TO_DEG) - 90.0f;
            mob.setYRot(rotlerp(mob.getYRot(), Yaw, Turn));
            mob.yBodyRot = mob.getYRot();
            mob.yHeadRot = mob.getYRot();
        }
        Fly.wobble();
        Fly.dryGuard();
    }
}
