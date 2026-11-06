package net.rai.scp_unleashed.flamethrower;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class FlamerBurns {
    public static final int Second = 20;
    public static final float BurnDamage = 2.5F;
    public static final int SpreadTicks = 160;
    public static final double SpreadRadius = 2.5;
    public static final int MaxGeneration = 8;
    public static final int GlowEvery = 4;
    private static final Map<ServerLevel, Map<Integer, Burn>> Burning = new WeakHashMap<>();

    private FlamerBurns() {
    }

    private static final class Burn {
        private int Remaining;
        @Nullable
        private UUID Attacker;
        private int Generation;
    }

    public static boolean vulnerable(Entity Target) {
        Entity Body = Target instanceof PartEntity<?> Part ? Part.getParent() : Target;
        return Body.getType().is(FlamerRegistry.FlameVulnerable);
    }

    public static void kindle(ServerLevel World, LivingEntity Target, int Ticks, @Nullable Entity Attacker, int Generation) {
        Map<Integer, Burn> Found = Burning.computeIfAbsent(World, Key -> new HashMap<>());
        Burn Entry = Found.get(Target.getId());
        if (Entry == null) {
            Entry = new Burn();
            Entry.Generation = Generation;
            Found.put(Target.getId(), Entry);
        } else {
            Entry.Generation = Math.min(Entry.Generation, Generation);
        }
        Entry.Remaining = Math.max(Entry.Remaining, Ticks);
        if (Attacker != null) {
            Entry.Attacker = Attacker.getUUID();
        }
        Target.igniteForTicks(Ticks);
    }

    public static int remaining(Entity Target) {
        Map<Integer, Burn> Found = Target.level() instanceof ServerLevel World ? Burning.get(World) : null;
        Burn Entry = Found == null ? null : Found.get(Target.getId());
        return Entry == null ? 0 : Entry.Remaining;
    }

    public static void tick(ServerLevel World, long Now) {
        Map<Integer, Burn> Found = Burning.get(World);
        if (Found == null || Found.isEmpty()) {
            return;
        }
        List<Runnable> Later = new ArrayList<>();
        Iterator<Map.Entry<Integer, Burn>> Entries = Found.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<Integer, Burn> Entry = Entries.next();
            Burn Fire = Entry.getValue();
            Entity Target = World.getEntity(Entry.getKey());
            if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Living.isInWaterRainOrBubble() || --Fire.Remaining <= 0) {
                Entries.remove();
                continue;
            }
            long Phase = Now + Entry.getKey();
            if (Living.fireImmune() && Phase % GlowEvery == 0) {
                glow(World, Living);
            }
            if (Phase % Second == 0) {
                Entity Attacker = Fire.Attacker == null ? null : World.getEntity(Fire.Attacker);
                Later.add(() -> scald(World, Living, Attacker));
                if (Fire.Generation < MaxGeneration) {
                    int Next = Fire.Generation + 1;
                    Later.add(() -> spread(World, Living, Attacker, Next));
                }
            }
        }
        Later.forEach(Runnable::run);
    }

    public static DamageSource source(ServerLevel World, @Nullable Entity Attacker) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FlamerRegistry.BurnDamage), null, Attacker);
    }

    private static void scald(ServerLevel World, LivingEntity Target, @Nullable Entity Attacker) {
        if (!Target.isAlive()) {
            return;
        }
        Target.invulnerableTime = 0;
        Target.hurt(source(World, Attacker), BurnDamage);
    }

    private static void spread(ServerLevel World, LivingEntity Source, @Nullable Entity Attacker, int Generation) {
        if (!Source.isAlive()) {
            return;
        }
        Map<Integer, Burn> Found = Burning.get(World);
        AABB Area = Source.getBoundingBox().inflate(SpreadRadius);
        for (LivingEntity Near : World.getEntitiesOfClass(LivingEntity.class, Area, Candidate -> Candidate != Source && Candidate.isAlive() && vulnerable(Candidate)
                && !Candidate.isInWaterRainOrBubble())) {
            if (Found != null && Found.containsKey(Near.getId())) {
                continue;
            }
            kindle(World, Near, SpreadTicks, Attacker, Generation);
            World.sendParticles(ParticleTypes.FLAME, Near.getX(), Near.getY() + Near.getBbHeight() * 0.5, Near.getZ(), 6, Near.getBbWidth() * 0.4, Near.getBbHeight() * 0.3,
                    Near.getBbWidth() * 0.4, 0.02);
        }
    }

    private static void glow(ServerLevel World, LivingEntity Target) {
        double Width = Target.getBbWidth() * 0.45;
        double Height = Target.getBbHeight();
        World.sendParticles(ParticleTypes.FLAME, Target.getX(), Target.getY() + Height * 0.45, Target.getZ(), 3, Width, Height * 0.35, Width, 0.01);
        if (World.random.nextInt(3) == 0) {
            World.sendParticles(ParticleTypes.SMOKE, Target.getX(), Target.getY() + Height * 0.8, Target.getZ(), 1, Width * 0.5, 0.1, Width * 0.5, 0.01);
        }
    }

    public static void forget(ServerLevel World) {
        Burning.remove(World);
    }

    public static void clear() {
        Burning.clear();
    }
}
