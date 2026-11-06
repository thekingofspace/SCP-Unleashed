package net.rai.scp_unleashed.scp163;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

public class Scp163MoveControl extends MoveControl {
    public static final double HoldRadius = 0.15;
    public static final float TurnDeadZone = 3.0f;
    public static final double SidestepStop = 1.0e-3;
    public static final double SidestepEase = 0.3;
    private boolean Sidestep;
    private boolean Sidestepped;
    private double SideX;
    private double SideZ;
    private double SideSpeed;

    public Scp163MoveControl(Mob Owner) {
        super(Owner);
    }

    public void sidestep(double X, double Z, double Speed) {
        SideX = X;
        SideZ = Z;
        SideSpeed = Speed;
        Sidestep = true;
        operation = Operation.WAIT;
    }

    @Override
    public void setWantedPosition(double X, double Y, double Z, double Speed) {
        Sidestep = false;
        super.setWantedPosition(X, Y, Z, Speed);
    }

    @Override
    public void tick() {
        if (Sidestep) {
            Sidestep = false;
            Sidestepped = true;
            shuffle();
            return;
        }
        if (Sidestepped) {
            Sidestepped = false;
            mob.setXxa(0.0f);
        }
        if (operation == Operation.MOVE_TO) {
            double Dx = wantedX - mob.getX();
            double Dz = wantedZ - mob.getZ();
            double Dy = wantedY - mob.getY();
            if (Dx * Dx + Dz * Dz < HoldRadius * HoldRadius && Dy <= mob.maxUpStep()) {
                operation = Operation.WAIT;
                mob.setZza(0.0f);
                return;
            }
            float Heading = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
            if (Math.abs(Mth.wrapDegrees(Heading - mob.getYRot())) <= TurnDeadZone) {
                float Kept = mob.getYRot();
                super.tick();
                mob.setYRot(Kept);
                return;
            }
        }
        super.tick();
    }

    private void shuffle() {
        double Dx = SideX - mob.getX();
        double Dz = SideZ - mob.getZ();
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance < SidestepStop) {
            mob.setZza(0.0f);
            mob.setXxa(0.0f);
            return;
        }
        float Pace = (float) (SideSpeed * mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * Math.min(1.0, Distance / SidestepEase));
        float Yaw = mob.getYRot() * Mth.DEG_TO_RAD;
        float Sin = Mth.sin(Yaw);
        float Cos = Mth.cos(Yaw);
        mob.setSpeed(Pace);
        mob.setZza((float) ((Dz * Cos - Dx * Sin) / Distance * Pace));
        mob.setXxa((float) ((Dx * Cos + Dz * Sin) / Distance * Pace));
    }
}
