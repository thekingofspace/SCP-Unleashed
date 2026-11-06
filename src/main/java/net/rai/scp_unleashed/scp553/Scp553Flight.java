package net.rai.scp_unleashed.scp553;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class Scp553Flight extends MoveControl {
    public static final double Accel = 0.035;
    public static final double DiveAccel = 0.09;
    public static final double GlideAccel = 0.012;
    public static final double Brake = 0.3;
    public static final double Creep = 0.015;
    public static final double Arrive = 0.08;
    public static final float Turn = 18.0f;
    public static final float DiveTurn = 40.0f;
    private final Scp553Butterfly Butterfly;

    public Scp553Flight(Scp553Butterfly Butterfly) {
        super(Butterfly);
        this.Butterfly = Butterfly;
    }

    @Override
    public void tick() {
        if (operation != Operation.MOVE_TO) {
            mob.setZza(0.0f);
            mob.setYya(0.0f);
            Butterfly.flutter(false);
            return;
        }
        operation = Operation.WAIT;
        Vec3 To = new Vec3(wantedX - mob.getX(), wantedY - mob.getY(), wantedZ - mob.getZ());
        double Distance = To.length();
        if (Distance < Arrive) {
            Butterfly.flutter(false);
            return;
        }
        boolean Diving = Butterfly.isDiving();
        double Top = Butterfly.cruise() * speedModifier;
        double Want = Math.min(Top, Distance * Brake + Creep);
        Vec3 Desired = To.scale(Want / Distance);
        Vec3 Current = mob.getDeltaMovement();
        Vec3 Steer = Desired.subtract(Current);
        double Limit = Diving ? DiveAccel : Butterfly.isGliding() ? GlideAccel : Accel;
        double Length = Steer.length();
        if (Length > Limit) {
            Steer = Steer.scale(Limit / Length);
        }
        mob.setDeltaMovement(Current.add(Steer));
        if (!Butterfly.facesQuarry() && To.horizontalDistanceSqr() > 1.0e-4) {
            float Yaw = (float) (Mth.atan2(To.z, To.x) * Mth.RAD_TO_DEG) - 90.0f;
            mob.setYRot(rotlerp(mob.getYRot(), Yaw, Diving ? DiveTurn : Turn));
            mob.yBodyRot = mob.getYRot();
            mob.yHeadRot = mob.getYRot();
        }
        Butterfly.flutter(true);
    }
}
