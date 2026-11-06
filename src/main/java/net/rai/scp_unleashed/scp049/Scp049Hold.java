package net.rai.scp_unleashed.scp049;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Scp049Hold {
    public static final ResourceLocation SpeedLock = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_hold_speed");
    public static final ResourceLocation JumpLock = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_hold_jump");
    public static final ResourceLocation StepAssist = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp049_hold_step");
    public static final double StepBoost = 0.6;
    public static final double Pull = 0.45;
    public static final double Blend = 0.55;
    public static final double Gravity = 0.08;
    public static final double Grounding = -0.0784;
    public static final double FallSpeed = -0.3;
    public static final float PersonHealth = 2.0f;
    public static final double StepUp = 1.05;
    public static final double NudgeSpeed = 0.25;
    public static final double NudgeLift = 0.45;

    private Scp049Hold() {
    }

    public static void seize(Scp049Doctor Doctor, LivingEntity Victim) {
        CompoundTag Mark = new CompoundTag();
        Mark.putUUID("Doctor", Doctor.getUUID());
        CompoundTag Earlier = Victim.getExistingDataOrNull(Scp049Registry.Held.get());
        if (Victim instanceof Mob Body) {
            Mark.putBoolean("NoAi", Earlier != null && Earlier.contains("NoAi") ? Earlier.getBoolean("NoAi") : Body.isNoAi());
            Body.getNavigation().stop();
            Body.setTarget(null);
            Body.setNoAi(true);
        }
        Victim.setData(Scp049Registry.Held.get(), Mark);
        Victim.stopRiding();
        Victim.setSprinting(false);
        Victim.stopUsingItem();
        lock(Victim);
    }

    public static void weaken(LivingEntity Victim) {
        Victim.setAbsorptionAmount(0.0f);
        if (Victim.getHealth() > PersonHealth) {
            Victim.setHealth(PersonHealth);
        }
    }

    public static boolean isHeld(@Nullable Entity Found) {
        return Found instanceof LivingEntity Body && Body.getExistingDataOrNull(Scp049Registry.Held.get()) != null;
    }

    @Nullable
    public static UUID holder(LivingEntity Victim) {
        CompoundTag Mark = Victim.getExistingDataOrNull(Scp049Registry.Held.get());
        return Mark != null && Mark.hasUUID("Doctor") ? Mark.getUUID("Doctor") : null;
    }

    public static boolean heldBy(LivingEntity Victim, Scp049Doctor Doctor) {
        return Doctor.getUUID().equals(holder(Victim));
    }

    public static void release(LivingEntity Victim) {
        CompoundTag Mark = Victim.getExistingDataOrNull(Scp049Registry.Held.get());
        if (Mark != null) {
            if (Victim instanceof Mob Body) {
                Body.setNoAi(Mark.getBoolean("NoAi"));
            }
            Victim.removeData(Scp049Registry.Held.get());
        }
        unlock(Victim);
    }

    public static void lock(LivingEntity Victim) {
        add(Victim, Attributes.MOVEMENT_SPEED, SpeedLock, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Victim, Attributes.JUMP_STRENGTH, JumpLock, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        add(Victim, Attributes.STEP_HEIGHT, StepAssist, StepBoost, AttributeModifier.Operation.ADD_VALUE);
        if (Victim instanceof Mob Body && !Body.isNoAi()) {
            Body.setNoAi(true);
        }
    }

    public static void unlock(LivingEntity Victim) {
        remove(Victim, Attributes.MOVEMENT_SPEED, SpeedLock);
        remove(Victim, Attributes.JUMP_STRENGTH, JumpLock);
        remove(Victim, Attributes.STEP_HEIGHT, StepAssist);
    }

    private static void add(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id, double Amount, AttributeModifier.Operation Operation) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null && !Instance.hasModifier(Id)) {
            Instance.addTransientModifier(new AttributeModifier(Id, Amount, Operation));
        }
    }

    private static void remove(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null) {
            Instance.removeModifier(Id);
        }
    }

    public static String unstick(LivingEntity Victim, Vec3 Point) {
        Vec3 Here = Victim.position();
        Vec3 Flat = new Vec3(Point.x - Here.x, 0.0, Point.z - Here.z);
        double Gap = Flat.length();
        if (Gap < 0.2) {
            return "close";
        }
        Vec3 Ahead = Flat.scale(1.0 / Gap);
        Vec3 Side = new Vec3(-Ahead.z, 0.0, Ahead.x);
        if (Victim instanceof Player) {
            Victim.setDeltaMovement(Ahead.x * NudgeSpeed, NudgeLift, Ahead.z * NudgeSpeed);
            Victim.hurtMarked = true;
            return "hop";
        }
        Vec3[][] Options = {
                {new Vec3(0.0, StepUp, 0.0), Ahead.scale(0.45)},
                {Side.scale(0.5), Ahead.scale(0.35)},
                {Side.scale(-0.5), Ahead.scale(0.35)},
                {Ahead.scale(-0.35), Side.scale(0.5)},
                {Ahead.scale(-0.35), Side.scale(-0.5)},
        };
        String[] Names = {"step", "side", "side", "slide", "slide"};
        for (int Index = 0; Index < Options.length; Index++) {
            AABB Box = Victim.getBoundingBox();
            boolean Clear = true;
            for (Vec3 Step : Options[Index]) {
                Box = Box.move(Step);
                if (!Victim.level().noCollision(Victim, Box)) {
                    Clear = false;
                    break;
                }
            }
            if (!Clear) {
                continue;
            }
            for (Vec3 Step : Options[Index]) {
                Victim.move(MoverType.SELF, Step);
            }
            Victim.setDeltaMovement(Vec3.ZERO);
            return Names[Index];
        }
        return "blocked";
    }

    public static void tether(LivingEntity Victim, Vec3 Point, double MaxSpeed, @Nullable Vec3 Face) {
        Vec3 Here = Victim.position();
        double Dx = Point.x - Here.x;
        double Dz = Point.z - Here.z;
        double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
        double Want = Math.min(MaxSpeed, Gap * Pull);
        double Wx = Gap > 1.0e-4 ? Dx / Gap * Want : 0.0;
        double Wz = Gap > 1.0e-4 ? Dz / Gap * Want : 0.0;
        Vec3 Old = Victim.getDeltaMovement();
        double Vx = Old.x + (Wx - Old.x) * Blend;
        double Vz = Old.z + (Wz - Old.z) * Blend;
        if (Math.abs(Vx) < 1.0e-3 && Gap < 0.05) {
            Vx = 0.0;
        }
        if (Math.abs(Vz) < 1.0e-3 && Gap < 0.05) {
            Vz = 0.0;
        }
        if (Victim instanceof Player) {
            Victim.setDeltaMovement(Vx, Victim.onGround() ? 0.0 : FallSpeed, Vz);
            Victim.hurtMarked = true;
            return;
        }
        double Vy = Victim.onGround() ? Grounding : Math.max(-3.0, (Old.y - Gravity) * 0.98);
        if (Victim.isInWater() || Victim.isInLava()) {
            Vy = Math.max(Vy, -0.05);
        }
        Vec3 Motion = new Vec3(Vx, Vy, Vz);
        Victim.setDeltaMovement(Motion);
        Victim.move(MoverType.SELF, Motion);
        if (Victim.onGround() && Victim.getDeltaMovement().y < 0.0) {
            Victim.setDeltaMovement(Victim.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
        if (Face != null) {
            float Yaw = (float) (Mth.atan2(Face.z - Victim.getZ(), Face.x - Victim.getX()) * Mth.RAD_TO_DEG) - 90.0f;
            float Turned = Mth.approachDegrees(Victim.getYRot(), Yaw, 12.0f);
            Victim.setYRot(Turned);
            Victim.setYBodyRot(Turned);
            Victim.setYHeadRot(Turned);
        }
    }
}
