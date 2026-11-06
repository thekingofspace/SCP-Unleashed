package net.rai.scp_unleashed.scp093;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.BodyRotationControl;

public class Scp093TorsoTurning extends BodyRotationControl {
    public static final float IdleFollow = 30.0f;
    public static final int IdleDelay = 20;

    private final Scp093Torso Torso;
    private int Held;

    public Scp093TorsoTurning(Scp093Torso Torso) {
        super(Torso);
        this.Torso = Torso;
    }

    @Override
    public void clientTick() {
        if (!Torso.level().isClientSide) {
            settle();
        }
        float Gap = Mth.wrapDegrees(Torso.getYRot() - Torso.yBodyRot);
        Torso.yBodyRot += Mth.clamp(Gap * Scp093Torso.BodyEase, -Scp093Torso.BodyFollow, Scp093Torso.BodyFollow);
        Torso.yHeadRot = Mth.rotateIfNecessary(Torso.yHeadRot, Torso.yBodyRot, Torso.getMaxHeadYRot());
    }

    private void settle() {
        boolean Resting = Torso.isAlive() && !Torso.locksMovement() && Torso.getTarget() == null && Torso.getNavigation().isDone()
                && !(Torso.getNavigation() instanceof Scp093TorsoNavigation Route && Route.heading() != null);
        float Aside = Mth.wrapDegrees(Torso.yHeadRot - Torso.getYRot());
        if (!Resting || Math.abs(Aside) < IdleFollow) {
            Held = 0;
            return;
        }
        if (++Held > IdleDelay) {
            Torso.setYRot(Torso.getYRot() + Scp093Torso.turnStep(Aside, Scp093Torso.IdleTurn));
        }
    }
}
