package net.rai.scp_unleashed.scp420j.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp420j.Scp420jFloat;

public final class Scp420jSteer {
    public static final double Damp = 0.84;
    public static final double DescentDamp = 0.92;
    public static final double DriftSpeed = 0.012;
    public static final double DriftTurn = 0.021;

    private Scp420jSteer() {
    }

    public static void steer(LocalPlayer User) {
        if (User.getAbilities().flying || User.isPassenger() || User.isFallFlying() || User.isSpectator() || User.isInWater() || User.isInLava()) {
            return;
        }
        Vec3 Motion = User.getDeltaMovement();
        if (Scp420jClientState.floating()) {
            int Elapsed = Scp420jClientState.elapsed();
            double Rise = Scp420jFloat.steer(User.getY(), Scp420jClientState.baseY(), Elapsed);
            double Settle = Math.min(1.0, Elapsed / (double) Scp420jFloat.RiseTicks);
            double Heading = Elapsed * DriftTurn + (Scp420jClientState.session() & 255) * 0.0245;
            double DriftX = Math.cos(Heading) * DriftSpeed * Settle;
            double DriftZ = Math.sin(Heading) * DriftSpeed * Settle;
            User.setDeltaMovement(Motion.x * Damp + DriftX, Rise, Motion.z * Damp + DriftZ);
            User.resetFallDistance();
        } else if (Scp420jClientState.descending() && Motion.y < -Scp420jFloat.DescendSpeed) {
            User.setDeltaMovement(Motion.x * DescentDamp, -Scp420jFloat.DescendSpeed, Motion.z * DescentDamp);
            User.resetFallDistance();
        }
    }
}
