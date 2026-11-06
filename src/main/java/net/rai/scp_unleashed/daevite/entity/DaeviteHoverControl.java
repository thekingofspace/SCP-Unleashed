package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class DaeviteHoverControl extends MoveControl {
    public static final double Response = 0.35;
    public static final double Settle = 0.5;

    public DaeviteHoverControl(Mob Owner) {
        super(Owner);
    }

    @Override
    public void tick() {
        Vec3 Motion = mob.getDeltaMovement();
        if (operation != Operation.MOVE_TO) {
            mob.setZza(0.0f);
            mob.setXxa(0.0f);
            return;
        }
        operation = Operation.WAIT;
        double Dx = wantedX - mob.getX();
        double Dz = wantedZ - mob.getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Flat < 0.12) {
            mob.setDeltaMovement(Motion.x * Settle, Motion.y, Motion.z * Settle);
            return;
        }
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        mob.setYRot(rotlerp(mob.getYRot(), Yaw, 14.0f));
        mob.yBodyRot = mob.getYRot();
        double Pace = speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double Tx = Dx / Flat * Pace;
        double Tz = Dz / Flat * Pace;
        mob.setDeltaMovement(Motion.x + (Tx - Motion.x) * Response, Motion.y, Motion.z + (Tz - Motion.z) * Response);
        mob.setZza(0.0f);
    }
}
