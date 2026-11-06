package net.rai.scp_unleashed.scp409;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;

import java.util.List;

public final class Scp409Exposure {
    public static final int SpreadInterval = 4;
    public static final double ContactMargin = 0.15;
    public static final int HumCooldown = 30;

    private Scp409Exposure() {
    }

    public static boolean isExempt(LivingEntity Target) {
        return Target instanceof ArmorStand || Target.isInvulnerable() || InfectionImmunity.isImmune(Target, Scp409Registry.Immune)
                || Target instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean isProtected(LivingEntity Target) {
        return GearTags.wearsFullHazmatSuit(Target);
    }

    public static boolean canCatch(LivingEntity Target) {
        return Target.isAlive() && !Target.isRemoved() && !Scp409Infection.isInfected(Target) && !isExempt(Target) && !isProtected(Target);
    }

    public static boolean isPerson(Entity Target) {
        return Target instanceof Player || ControlBodies.isPersonLike(Target);
    }

    public static boolean allowed(LivingEntity Source, LivingEntity Target) {
        if (isPerson(Source) || isPerson(Target)) {
            return true;
        }
        return Scp409Registry.mobSpread(Target.level().getGameRules());
    }

    public static boolean pass(LivingEntity Source, LivingEntity Target) {
        if (Source == Target || !Scp409Infection.isInfected(Source) || Scp409Infection.isPaused(Source) || !allowed(Source, Target)) {
            return false;
        }
        if (Target.isAlive() && isExempt(Target)) {
            InfectionImmunity.absorb(Target, Scp409Registry.DocumentId);
            return false;
        }
        return canCatch(Target) && Scp409Infection.infect(Target);
    }

    public static void spread(ServerLevel World, LivingEntity Source) {
        if (Math.floorMod(Source.tickCount + Source.getId(), SpreadInterval) != 0 || !Source.isAlive()) {
            return;
        }
        AABB Reach = Source.getBoundingBox().inflate(ContactMargin);
        List<LivingEntity> Touching = World.getEntitiesOfClass(LivingEntity.class, Reach, Target -> Target != Source && Target.isAlive());
        for (LivingEntity Target : Touching) {
            pass(Source, Target);
        }
    }

    public static void melee(LivingEntity Attacker, LivingEntity Victim) {
        if (Attacker.level().isClientSide || Attacker == Victim) {
            return;
        }
        if (Scp409Infection.isInfected(Attacker)) {
            pass(Attacker, Victim);
        } else if (Scp409Infection.isInfected(Victim)) {
            pass(Victim, Attacker);
        }
    }

    public static boolean touchCrystal(Entity Toucher) {
        if (!(Toucher instanceof LivingEntity Target) || !(Toucher.level() instanceof ServerLevel World) || !Target.isAlive()) {
            return false;
        }
        if (Scp409Infection.isInfected(Target)) {
            return false;
        }
        if (isExempt(Target)) {
            InfectionImmunity.absorb(Target, Scp409Registry.DocumentId);
            return false;
        }
        if (isProtected(Target)) {
            Scp409Trace Trace = Target.getData(Scp409Registry.Trace.get());
            if (Target.tickCount >= Trace.HumCooldown) {
                Trace.HumCooldown = Target.tickCount + HumCooldown;
                World.playSound(null, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), Scp409Registry.Hum.get(), SoundSource.BLOCKS, 0.6f, 1.25f);
            }
            return false;
        }
        return Scp409Infection.infect(Target);
    }
}
