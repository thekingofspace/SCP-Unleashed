package net.rai.scp_unleashed.scp031;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;

public class Scp031LookControl extends LookControl {
    private final Scp031Gaze Gaze = new Scp031Gaze();

    public Scp031LookControl(Mob Owner) {
        super(Owner);
    }

    @Override
    public void tick() {
        boolean Tracking = lookAtCooldown > 0;
        if (Tracking) {
            lookAtCooldown--;
        }
        float WantedYaw = Tracking ? getYRotD().orElse(mob.yBodyRot) : mob.yBodyRot;
        float WantedPitch = Tracking ? getXRotD().orElse(0.0f) : 0.0f;
        Scp031Gaze.Pose Next = Gaze.step(mob.yHeadRot, mob.getXRot(), WantedYaw, WantedPitch, Tracking, mob.getRandom()::nextDouble);
        mob.yHeadRot = Next.Yaw();
        mob.setXRot(Next.Pitch());
        clampHeadRotationToBody();
    }

    @Override
    protected boolean resetXRotOnTick() {
        return false;
    }
}
