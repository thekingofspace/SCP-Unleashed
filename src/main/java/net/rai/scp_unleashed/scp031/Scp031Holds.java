package net.rai.scp_unleashed.scp031;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp031Holds {
    public static final ResourceLocation ModifierId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp031_hold");
    public static final int StaleTicks = 4;
    public static final int SlownessLevel = 6;
    public static final int SlownessTicks = 12;
    public static final double Leash = 0.25;
    public static final double PullLimit = 0.18;
    private static final Map<UUID, Hold> Held = new ConcurrentHashMap<>();

    private Scp031Holds() {
    }

    public record Hold(UUID Holder, long Seen) {
    }

    public static boolean isHeld(Entity Target) {
        return Held.containsKey(Target.getUUID());
    }

    @Nullable
    public static UUID holderOf(Entity Target) {
        Hold Found = Held.get(Target.getUUID());
        return Found == null ? null : Found.Holder();
    }

    public static boolean isHeldBy(Entity Target, Entity Holder) {
        Hold Found = Held.get(Target.getUUID());
        return Found != null && Found.Holder().equals(Holder.getUUID());
    }

    public static int count() {
        return Held.size();
    }

    public static void hold(LivingEntity Target, Scp031Mimic Holder, Vec3 Anchor, long Now) {
        Held.put(Target.getUUID(), new Hold(Holder.getUUID(), Now));
        lock(Target);
        clamp(Target, Anchor);
    }

    public static void release(LivingEntity Target) {
        Held.remove(Target.getUUID());
        unlock(Target);
    }

    public static void forget(UUID Target) {
        Held.remove(Target);
    }

    private static void lock(LivingEntity Target) {
        modify(Target.getAttribute(Attributes.MOVEMENT_SPEED));
        modify(Target.getAttribute(Attributes.JUMP_STRENGTH));
        MobEffectInstance Current = Target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (Current == null || Current.getAmplifier() < SlownessLevel || Current.getDuration() < SlownessTicks / 2) {
            Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SlownessTicks, SlownessLevel, false, false, true));
        }
        Target.setSprinting(false);
        if (Target instanceof Mob Captive) {
            Captive.getNavigation().stop();
            Captive.setJumping(false);
        }
        if (Target.isSleeping()) {
            Target.stopSleeping();
        }
    }

    private static void modify(@Nullable AttributeInstance Instance) {
        if (Instance != null && !Instance.hasModifier(ModifierId)) {
            Instance.addTransientModifier(new AttributeModifier(ModifierId, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void unlock(LivingEntity Target) {
        AttributeInstance Speed = Target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.removeModifier(ModifierId);
        }
        AttributeInstance Jump = Target.getAttribute(Attributes.JUMP_STRENGTH);
        if (Jump != null) {
            Jump.removeModifier(ModifierId);
        }
        MobEffectInstance Current = Target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (Current != null && Current.getAmplifier() == SlownessLevel && Current.getDuration() <= SlownessTicks) {
            Target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private static void clamp(LivingEntity Target, Vec3 Anchor) {
        Vec3 Motion = Target.getDeltaMovement();
        double Dx = Anchor.x - Target.getX();
        double Dz = Anchor.z - Target.getZ();
        double Offset = Math.sqrt(Dx * Dx + Dz * Dz);
        Vec3 Next;
        if (Offset > Leash) {
            double Pull = Math.min(PullLimit, (Offset - Leash) * 0.5 + 0.02);
            Next = new Vec3(Dx / Offset * Pull, Math.min(Motion.y, 0.0), Dz / Offset * Pull);
        } else {
            Next = new Vec3(0.0, Math.min(Motion.y, 0.0), 0.0);
        }
        boolean Changed = Math.abs(Next.x - Motion.x) > 1.0e-3 || Math.abs(Next.z - Motion.z) > 1.0e-3 || Motion.y > 1.0e-3;
        Target.setDeltaMovement(Next);
        if (Target instanceof ServerPlayer && Changed && (Offset > Leash || Motion.horizontalDistanceSqr() > 4.0e-4 || Motion.y > 1.0e-3)) {
            Target.hurtMarked = true;
        }
    }

    public static void sweep(MinecraftServer Server) {
        if (Held.isEmpty()) {
            return;
        }
        long Now = Server.getTickCount();
        for (Map.Entry<UUID, Hold> Entry : List.copyOf(Held.entrySet())) {
            if (Now - Entry.getValue().Seen() <= StaleTicks) {
                continue;
            }
            Held.remove(Entry.getKey());
            for (ServerLevel World : Server.getAllLevels()) {
                if (World.getEntity(Entry.getKey()) instanceof LivingEntity Target) {
                    unlock(Target);
                    break;
                }
            }
        }
    }

    public static int releaseAll(MinecraftServer Server) {
        int Freed = 0;
        for (UUID Target : List.copyOf(Held.keySet())) {
            for (ServerLevel World : Server.getAllLevels()) {
                if (World.getEntity(Target) instanceof LivingEntity Found) {
                    UUID Holder = holderOf(Found);
                    if (Holder != null && World.getEntity(Holder) instanceof Scp031Mimic Mimic) {
                        Mimic.releaseHold(false, Scp031Mimic.BreakCooldown);
                    }
                    release(Found);
                    Freed++;
                    break;
                }
            }
            Held.remove(Target);
        }
        return Freed;
    }

    public static void clear() {
        Held.clear();
    }
}
