package net.rai.scp_unleashed.scp469;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class Scp469Embrace {
    public static final ResourceLocation SpeedLock = Scp469Registry.location("scp469_embrace_speed");
    public static final ResourceLocation JumpLock = Scp469Registry.location("scp469_embrace_jump");
    public static final double Pull = 0.5;
    public static final double Blend = 0.6;
    public static final double MaxPull = 0.35;
    public static final double Gravity = 0.08;
    public static final double Snap = 2.5;

    private Scp469Embrace() {
    }

    public static void seize(Scp469Angel Holder, LivingEntity Victim) {
        CompoundTag Mark = new CompoundTag();
        Mark.putUUID("Holder", Holder.getUUID());
        CompoundTag Earlier = Victim.getExistingDataOrNull(Scp469Registry.Embraced.get());
        if (Victim instanceof Mob Body) {
            Mark.putBoolean("NoAi", Earlier != null && Earlier.contains("NoAi") ? Earlier.getBoolean("NoAi") : Body.isNoAi());
            Body.getNavigation().stop();
            Body.setTarget(null);
            Body.setNoAi(true);
        }
        Victim.setData(Scp469Registry.Embraced.get(), Mark);
        Victim.stopRiding();
        Victim.setSprinting(false);
        Victim.stopUsingItem();
        lock(Victim);
    }

    public static boolean isHeld(@Nullable Entity Found) {
        return Found instanceof LivingEntity Body && Body.getExistingDataOrNull(Scp469Registry.Embraced.get()) != null;
    }

    @Nullable
    public static UUID holder(LivingEntity Victim) {
        CompoundTag Mark = Victim.getExistingDataOrNull(Scp469Registry.Embraced.get());
        return Mark != null && Mark.hasUUID("Holder") ? Mark.getUUID("Holder") : null;
    }

    public static boolean heldBy(LivingEntity Victim, Scp469Angel Holder) {
        return Holder.getUUID().equals(holder(Victim));
    }

    public static void release(LivingEntity Victim) {
        CompoundTag Mark = Victim.getExistingDataOrNull(Scp469Registry.Embraced.get());
        if (Mark != null) {
            if (Victim instanceof Mob Body) {
                Body.setNoAi(Mark.getBoolean("NoAi"));
            }
            Victim.removeData(Scp469Registry.Embraced.get());
        }
        unlock(Victim);
    }

    public static boolean locked(LivingEntity Victim) {
        AttributeInstance Speed = Victim.getAttribute(Attributes.MOVEMENT_SPEED);
        return Speed != null && Speed.hasModifier(SpeedLock);
    }

    public static void lock(LivingEntity Victim) {
        add(Victim, Attributes.MOVEMENT_SPEED, SpeedLock, -1.0);
        add(Victim, Attributes.JUMP_STRENGTH, JumpLock, -1.0);
    }

    public static void unlock(LivingEntity Victim) {
        remove(Victim, Attributes.MOVEMENT_SPEED, SpeedLock);
        remove(Victim, Attributes.JUMP_STRENGTH, JumpLock);
    }

    private static void add(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id, double Amount) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null && !Instance.hasModifier(Id)) {
            Instance.addTransientModifier(new AttributeModifier(Id, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void remove(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null) {
            Instance.removeModifier(Id);
        }
    }

    public static void tether(LivingEntity Victim, Vec3 Point) {
        Vec3 Here = Victim.position();
        double Dx = Point.x - Here.x;
        double Dz = Point.z - Here.z;
        double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Gap > Snap && Victim instanceof Player User) {
            User.teleportTo(Point.x, Math.max(Point.y, Here.y), Point.z);
            return;
        }
        double Want = Math.min(MaxPull, Gap * Pull);
        double Wx = Gap > 1.0e-4 ? Dx / Gap * Want : 0.0;
        double Wz = Gap > 1.0e-4 ? Dz / Gap * Want : 0.0;
        Vec3 Old = Victim.getDeltaMovement();
        double Vx = Old.x + (Wx - Old.x) * Blend;
        double Vz = Old.z + (Wz - Old.z) * Blend;
        if (Victim instanceof Player) {
            Victim.setDeltaMovement(Vx, Victim.onGround() ? 0.0 : Math.min(0.0, Old.y), Vz);
            Victim.hurtMarked = true;
            return;
        }
        double Vy = Victim.onGround() ? -0.0784 : Math.max(-3.0, (Old.y - Gravity) * 0.98);
        Vec3 Motion = new Vec3(Vx, Vy, Vz);
        Victim.setDeltaMovement(Motion);
        Victim.move(MoverType.SELF, Motion);
        if (Victim.onGround() && Victim.getDeltaMovement().y < 0.0) {
            Victim.setDeltaMovement(Victim.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        }
    }
}
