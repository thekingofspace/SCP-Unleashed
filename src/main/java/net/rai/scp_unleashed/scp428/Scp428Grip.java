package net.rai.scp_unleashed.scp428;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Scp428Grip {
    public static final ResourceLocation SpeedLock = Scp428Registry.location("scp428_grip_speed");
    public static final ResourceLocation JumpLock = Scp428Registry.location("scp428_grip_jump");
    public static final double Pull = 0.5;
    public static final double Blend = 0.6;
    public static final double MaxPull = 0.35;
    public static final double Gravity = 0.08;
    public static final double FallSpeed = -0.3;
    public static final float TurnRate = 14.0f;

    private Scp428Grip() {
    }

    public static void lock(LivingEntity Victim) {
        Victim.stopRiding();
        Victim.setSprinting(false);
        Victim.stopUsingItem();
        add(Victim, Attributes.MOVEMENT_SPEED, SpeedLock);
        add(Victim, Attributes.JUMP_STRENGTH, JumpLock);
        if (Victim instanceof Mob Body) {
            Body.getNavigation().stop();
            Body.setTarget(null);
        }
    }

    public static void unlock(LivingEntity Victim) {
        remove(Victim, Attributes.MOVEMENT_SPEED, SpeedLock);
        remove(Victim, Attributes.JUMP_STRENGTH, JumpLock);
    }

    public static boolean isLocked(LivingEntity Victim) {
        AttributeInstance Instance = Victim.getAttribute(Attributes.MOVEMENT_SPEED);
        return Instance != null && Instance.hasModifier(SpeedLock);
    }

    public static void hold(LivingEntity Victim, Vec3 Point, Vec3 Face) {
        if (Victim instanceof Mob Body) {
            Body.getNavigation().stop();
        }
        Vec3 Here = Victim.position();
        double Dx = Point.x - Here.x;
        double Dz = Point.z - Here.z;
        double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
        double Want = Math.min(MaxPull, Gap * Pull);
        double Wx = Gap > 1.0e-4 ? Dx / Gap * Want : 0.0;
        double Wz = Gap > 1.0e-4 ? Dz / Gap * Want : 0.0;
        Vec3 Old = Victim.getDeltaMovement();
        double Vx = Old.x + (Wx - Old.x) * Blend;
        double Vz = Old.z + (Wz - Old.z) * Blend;
        if (Victim instanceof Player) {
            Victim.setDeltaMovement(Vx, Victim.onGround() ? 0.0 : FallSpeed, Vz);
            Victim.hurtMarked = true;
            return;
        }
        double Vy = Victim.onGround() ? -0.0784 : Math.max(-3.0, (Old.y - Gravity) * 0.98);
        Vec3 Motion = new Vec3(Vx, Vy, Vz);
        Victim.setDeltaMovement(Motion);
        Victim.move(MoverType.SELF, Motion);
        float Yaw = (float) (Mth.atan2(Face.z - Victim.getZ(), Face.x - Victim.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.approachDegrees(Victim.getYRot(), Yaw, TurnRate);
        Victim.setYRot(Turned);
        Victim.setYBodyRot(Turned);
        Victim.setYHeadRot(Turned);
    }

    private static void add(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null && !Instance.hasModifier(Id)) {
            Instance.addTransientModifier(new AttributeModifier(Id, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void remove(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null) {
            Instance.removeModifier(Id);
        }
    }
}
