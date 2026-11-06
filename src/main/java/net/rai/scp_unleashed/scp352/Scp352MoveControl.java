package net.rai.scp_unleashed.scp352;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;

public class Scp352MoveControl extends MoveControl {
    public static final float FastTurn = 14.0f;
    public static final float SlowTurn = 40.0f;
    public static final float SharpAngle = 80.0f;
    public static final double SlowestShare = 0.25;
    public static final double FastModifier = 0.8;

    private final Scp352Crone Owner;
    private boolean Fast;

    public Scp352MoveControl(Scp352Crone Owner) {
        super(Owner);
        this.Owner = Owner;
    }

    public boolean fast() {
        return Fast;
    }

    @Override
    public void tick() {
        Fast = speedModifier >= FastModifier && Owner.phase().tall();
        if (operation == Operation.MOVE_TO && Fast) {
            double Dx = wantedX - Owner.getX();
            double Dz = wantedZ - Owner.getZ();
            if (Dx * Dx + Dz * Dz > 1.0e-4) {
                float Wanted = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
                float Turn = Math.abs(Mth.wrapDegrees(Wanted - Owner.getYRot()));
                speedModifier *= Math.sqrt(Mth.clamp(1.0 - Turn / SharpAngle, SlowestShare, 1.0));
            }
        }
        super.tick();
    }

    @Override
    protected float rotlerp(float Source, float Target, float Limit) {
        return super.rotlerp(Source, Target, Math.min(Limit, Fast ? FastTurn : SlowTurn));
    }
}
