package net.rai.scp_unleashed.scp129;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.labware.LabExposure;

public final class Scp129Spread {
    public static final int Interval = 40;
    public static final double Range = 3.5;
    public static final double CloseRange = 1.5;
    public static final float CloseChance = 0.12f;
    public static final float FarChance = 0.04f;
    public static final double CoughRange = 4.0;
    public static final double CoughCone = 0.25;
    public static final float CoughChance = 0.3f;
    public static final float HitChance = 0.45f;
    public static final float TouchChance = 0.2f;
    public static final float HandOverChance = 0.15f;
    public static final int FreshItemTicks = 1200;

    private Scp129Spread() {
    }

    public static boolean sheds(LivingEntity Carrier) {
        return Carrier.isAlive() && !Carrier.isSpectator() && !Scp129Infection.isPaused(Carrier) && !Scp129Infection.isProtected(Carrier)
                && Scp129Infection.isInfected(Carrier);
    }

    public static float chanceAt(double Distance) {
        if (Distance <= CloseRange) {
            return CloseChance;
        }
        float Fraction = (float) Mth.clamp((Distance - CloseRange) / (Range - CloseRange), 0.0, 1.0);
        return Mth.lerp(Fraction, CloseChance, FarChance);
    }

    public static void breathe(LivingEntity Carrier, ServerLevel World) {
        if (!sheds(Carrier)) {
            return;
        }
        int Strains = Scp129Infection.strainsOf(Carrier);
        Vec3 Mouth = Carrier.getEyePosition();
        for (LivingEntity Target : Scp129Infection.peopleNear(World, Mouth, LabExposure.NothingIgnored, Range, Carrier)) {
            pass(Target, Strains, chanceAt(Target.getEyePosition().distanceTo(Mouth)), true);
        }
    }

    public static void cough(LivingEntity Carrier, ServerLevel World) {
        if (!sheds(Carrier)) {
            return;
        }
        int Strains = Scp129Infection.strainsOf(Carrier);
        Vec3 Mouth = Carrier.getEyePosition();
        Vec3 Look = Carrier.getLookAngle();
        for (LivingEntity Target : Scp129Infection.peopleNear(World, Mouth, LabExposure.NothingIgnored, CoughRange, Carrier)) {
            Vec3 Toward = Target.getEyePosition().subtract(Mouth);
            double Distance = Toward.length();
            if (Distance > CloseRange && Toward.scale(1.0 / Math.max(Distance, 1.0E-4)).dot(Look) < CoughCone) {
                continue;
            }
            pass(Target, Strains, CoughChance, true);
        }
    }

    public static void contact(LivingEntity Attacker, LivingEntity Victim) {
        if (Attacker == Victim) {
            return;
        }
        if (sheds(Attacker) && Scp129Infection.canCatch(Victim)) {
            pass(Victim, Scp129Infection.strainsOf(Attacker), HitChance, false);
        }
        if (sheds(Victim) && Scp129Infection.canCatch(Attacker)) {
            pass(Attacker, Scp129Infection.strainsOf(Victim), TouchChance, false);
        }
    }

    public static void handOver(Player Picker, ItemEntity Dropped) {
        Entity Owner = Dropped.getOwner();
        if (!(Owner instanceof LivingEntity Thrower) || Thrower == Picker || Dropped.getAge() > FreshItemTicks || !sheds(Thrower) || !Scp129Infection.canCatch(Picker)) {
            return;
        }
        pass(Picker, Scp129Infection.strainsOf(Thrower), HandOverChance, false);
    }

    public static int pass(LivingEntity Target, int Strains, float Chance, boolean Airborne) {
        boolean Suited = Scp129Infection.isProtected(Target);
        RandomSource Random = Target.getRandom();
        int Caught = 0;
        for (Scp129Strain Strain : Scp129Strain.values()) {
            if (!Strain.in(Strains) || Random.nextFloat() >= Chance) {
                continue;
            }
            if (Suited) {
                if (Airborne && LabContamination.canCarry(Target, Strain.id())) {
                    LabContamination.add(Target, Strain.id());
                }
            } else if (Scp129Infection.infect(Target, Strain)) {
                Caught |= Strain.bit();
            }
        }
        return Caught;
    }
}
