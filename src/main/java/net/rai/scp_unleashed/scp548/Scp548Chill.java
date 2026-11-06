package net.rai.scp_unleashed.scp548;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp244.Scp244Chill;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp548Chill {
    public static final double AuraRadius = 1.5;
    public static final float AuraRate = 2.0f;
    public static final float StackShare = 0.5f;
    public static final int StackCap = 3;
    public static final int Overfreeze = 40;
    public static final int HurtGap = 20;
    public static final float HurtAmount = 1.0f;
    public static final int HissGap = 80;
    public static final int FrostbiteTicks = 160;
    public static final int FrostbiteCap = 320;
    public static final int VenomGap = 40;
    public static final float VenomDamage = 1.0f;
    private static final Map<LivingEntity, Exposure> Records = new WeakHashMap<>();

    private Scp548Chill() {
    }

    private static final class Exposure {
        private long Seen = Long.MIN_VALUE;
        private int Count;
        private float Carry;
        private int Clock;
    }

    public static boolean susceptible(LivingEntity Target) {
        return !(Target instanceof Scp548Spider) && Scp244Chill.susceptible(Target);
    }

    public static double distance(Vec3 Centre, Entity Target) {
        AABB Box = Target.getBoundingBox();
        double X = Mth.clamp(Centre.x, Box.minX, Box.maxX);
        double Y = Mth.clamp(Centre.y, Box.minY, Box.maxY);
        double Z = Mth.clamp(Centre.z, Box.minZ, Box.maxZ);
        return Centre.distanceTo(new Vec3(X, Y, Z));
    }

    public static Vec3 centre(Scp548Spider Spider) {
        return Spider.position().add(0.0, Spider.getBbHeight() * 0.5, 0.0);
    }

    public static boolean inAura(Scp548Spider Spider, Entity Target) {
        return distance(centre(Spider), Target) <= AuraRadius;
    }

    public static int radiate(ServerLevel World, Scp548Spider Spider) {
        Vec3 Centre = centre(Spider);
        long Now = World.getGameTime();
        int Touched = 0;
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Spider.getBoundingBox().inflate(AuraRadius), Each -> Each != Spider && susceptible(Each))) {
            if (distance(Centre, Target) <= AuraRadius) {
                expose(Target, Now);
                Touched++;
            }
        }
        return Touched;
    }

    public static void expose(LivingEntity Target, long Now) {
        Exposure Record = Records.computeIfAbsent(Target, Key -> new Exposure());
        if (Record.Seen != Now) {
            Record.Seen = Now;
            Record.Count = 1;
        } else {
            Record.Count++;
        }
    }

    public static int exposure(LivingEntity Target) {
        Exposure Record = Records.get(Target);
        return Record == null ? 0 : Record.Clock;
    }

    public static float rate(int Count) {
        return AuraRate * (1.0f + StackShare * (Mth.clamp(Count, 1, StackCap) - 1));
    }

    public static void apply(ServerLevel World) {
        if (Records.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<Map.Entry<LivingEntity, Exposure>> Entries = Records.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<LivingEntity, Exposure> Entry = Entries.next();
            LivingEntity Target = Entry.getKey();
            Exposure Record = Entry.getValue();
            if (Target == null || Target.isRemoved() || !Target.isAlive()) {
                Entries.remove();
                continue;
            }
            if (Target.level() != World) {
                continue;
            }
            if (Record.Seen == Now && susceptible(Target)) {
                chill(World, Target, Record);
            } else {
                Entries.remove();
            }
        }
    }

    private static void chill(ServerLevel World, LivingEntity Target, Exposure Record) {
        Record.Clock++;
        float Shield = Scp244Chill.ward(Target);
        if (Shield >= 1.0f) {
            Record.Carry = 0.0f;
            return;
        }
        Record.Carry += rate(Record.Count) * (1.0f - Shield);
        int Whole = (int) Record.Carry;
        Record.Carry -= Whole;
        int Frozen = Target.getTicksFrozen();
        int Cap = Target.getTicksRequiredToFreeze() + Overfreeze;
        Target.setTicksFrozen(Math.max(Frozen, Math.min(Cap, Frozen + 2 + Whole)));
        if (Record.Clock % HissGap == 1) {
            Scp548Swarm.playLayers(World, Target.position(), Scp548Registry.FrostLayers, 0.35f, 1.0f + World.getRandom().nextFloat() * 0.15f);
        }
        if (Target.isFullyFrozen() && Record.Clock % HurtGap == 0) {
            Target.hurt(World.damageSources().freeze(), HurtAmount * (1.0f - Shield));
        }
    }

    public static boolean canFrostbite(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || Target instanceof Scp548Spider || Target instanceof ArmorStand || Target.isSpectator()) {
            return false;
        }
        if (Target.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES) || !Targeting.isTargetable(Target)) {
            return false;
        }
        return !(Target instanceof Player User) || !User.getAbilities().invulnerable;
    }

    public static boolean bite(LivingEntity Target, @Nullable Entity Source) {
        if (!canFrostbite(Target)) {
            return false;
        }
        MobEffectInstance Existing = Target.getEffect(Scp548Registry.Frostbite);
        int Duration = Math.min(FrostbiteCap, Math.max(FrostbiteTicks, Existing == null ? 0 : Existing.getDuration() + FrostbiteTicks / 2));
        boolean Added = Target.addEffect(new MobEffectInstance(Scp548Registry.Frostbite, Duration, 0), Source);
        if (Added) {
            hold(Target);
        }
        return Added;
    }

    public static boolean isFrostbitten(LivingEntity Target) {
        return Target.hasEffect(Scp548Registry.Frostbite);
    }

    public static void hold(LivingEntity Target) {
        int Hold = Target.getTicksRequiredToFreeze() + 1;
        if (Target.getTicksFrozen() < Hold) {
            Target.setTicksFrozen(Hold);
        }
    }

    public static boolean frostbite(LivingEntity Target, int Amplifier) {
        if (Target.level().isClientSide) {
            return true;
        }
        if (Target.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
            return false;
        }
        hold(Target);
        if (Target.tickCount % VenomGap == 0) {
            Target.hurt(Target.damageSources().freeze(), VenomDamage * (Amplifier + 1));
        }
        return true;
    }

    public static void forget() {
        Records.clear();
    }
}
