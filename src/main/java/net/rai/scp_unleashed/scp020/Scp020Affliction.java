package net.rai.scp_unleashed.scp020;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.scp020.network.Scp020StatePayload;

import java.util.function.Predicate;

public final class Scp020Affliction {
    public static final int MaxIntensity = 100;
    public static final int DoseStep = 3;
    public static final int DoseInterval = 20;
    public static final int Grace = 200;
    public static final int DecayTicks = 50;
    public static final int AfflictIntensity = 60;

    private Scp020Affliction() {
    }

    public record Dose(int Intensity, long Dosed) {
        public static final Dose None = new Dose(0, Long.MIN_VALUE / 2);
        public static final Codec<Dose> DoseCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.optionalFieldOf("intensity", 0).forGetter(Dose::Intensity),
                Codec.LONG.optionalFieldOf("dosed", 0L).forGetter(Dose::Dosed)
        ).apply(Instance, Dose::new));
    }

    public static Dose doseOf(LivingEntity Subject) {
        Dose Found = Subject.getExistingDataOrNull(Scp020Registry.Affliction.get());
        return Found == null ? Dose.None : Found;
    }

    public static int intensity(LivingEntity Subject) {
        return doseOf(Subject).Intensity();
    }

    public static boolean isAfflicted(LivingEntity Subject) {
        return intensity(Subject) > 0;
    }

    public static boolean isImmune(LivingEntity Subject) {
        return InfectionImmunity.isImmune(Subject);
    }

    public static boolean isImmuneKind(LivingEntity Subject) {
        return Subject.getType().is(InfectionImmunity.Immune);
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (!Subject.isAlive() || isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player Target) {
            return !Target.isCreative() && !Target.isSpectator();
        }
        return Subject instanceof AbstractVillager || ControlBodies.isPersonLike(Subject);
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.protectsFromToxicGas(Subject);
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius) {
        for (ServerPlayer Exposed : LabExposure.playersInSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isProtected(Target))) {
            dose(Exposed);
        }
        for (LivingEntity Exposed : ControlBodies.inSight(World, Source, Ignored, Radius, Target -> canCatch(Target) && !isProtected(Target))) {
            dose(Exposed);
        }
        AABB Reach = AABB.ofSize(Source, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        for (AbstractVillager Exposed : World.getEntitiesOfClass(AbstractVillager.class, Reach,
                Target -> Target.distanceToSqr(Source) <= Radius * Radius && canCatch(Target) && LabExposure.sees(World, Source, Target, Ignored))) {
            dose(Exposed);
        }
    }

    public static boolean dose(LivingEntity Subject) {
        if (isImmune(Subject)) {
            return false;
        }
        long Now = Subject.level().getGameTime();
        Dose Current = doseOf(Subject);
        if (Now - Current.Dosed() < DoseInterval) {
            return false;
        }
        store(Subject, new Dose(Math.min(MaxIntensity, Current.Intensity() + DoseStep), Now));
        return true;
    }

    public static boolean afflict(LivingEntity Subject) {
        if (isImmuneKind(Subject)) {
            return false;
        }
        Dose Current = doseOf(Subject);
        store(Subject, new Dose(Math.max(Current.Intensity(), AfflictIntensity), Subject.level().getGameTime()));
        return true;
    }

    public static void setIntensity(LivingEntity Subject, int Intensity) {
        store(Subject, new Dose(Mth.clamp(Intensity, 0, MaxIntensity), Subject.level().getGameTime()));
    }

    public static boolean cure(LivingEntity Subject) {
        boolean Was = isAfflicted(Subject);
        store(Subject, Dose.None);
        return Was;
    }

    public static int secondsLeft(LivingEntity Subject) {
        Dose Current = doseOf(Subject);
        if (Current.Intensity() <= 0) {
            return 0;
        }
        long Since = Subject.level().getGameTime() - Current.Dosed();
        long Waiting = Math.max(0L, Grace - Since);
        return (int) ((Waiting + (long) Current.Intensity() * DecayTicks + 19L) / 20L);
    }

    public static void tick(LivingEntity Subject) {
        Dose Current = doseOf(Subject);
        if (Current.Intensity() <= 0) {
            return;
        }
        if (isImmuneKind(Subject)) {
            store(Subject, Dose.None);
            return;
        }
        long Since = Subject.level().getGameTime() - Current.Dosed();
        if (Since > Grace && Math.floorMod(Since - Grace, DecayTicks) == 0) {
            store(Subject, new Dose(Current.Intensity() - 1, Current.Dosed()));
        }
    }

    private static void store(LivingEntity Subject, Dose Next) {
        if (Next.Intensity() > 0 && isImmuneKind(Subject)) {
            Next = Dose.None;
        }
        int Before = intensity(Subject);
        if (Next.Intensity() <= 0) {
            Subject.removeData(Scp020Registry.Affliction.get());
        } else {
            Subject.setData(Scp020Registry.Affliction.get(), Next);
        }
        if (Subject instanceof ServerPlayer User && Before != Math.max(0, Next.Intensity())) {
            sync(User);
        }
    }

    public static void sync(ServerPlayer User) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp020StatePayload(intensity(User)));
        }
    }
}
