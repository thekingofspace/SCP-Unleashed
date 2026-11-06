package net.rai.scp_unleashed.scp261;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Scp261Pals {
    public static final String PalTag = "scp261_pal";
    public static final String ExpiryKey = "Scp261PalExpiry";
    public static final int Lifetime = 2400;
    public static final int MaxPerOwner = 2;
    public static final double SearchRadius = 96.0;
    public static final int SpotTries = 8;
    private static final Map<UUID, Set<UUID>> Owned = new HashMap<>();

    private Scp261Pals() {
    }

    public static boolean isPal(Entity Target) {
        return Target.getTags().contains(PalTag);
    }

    public static long expiryOf(Entity Target) {
        return Target.getPersistentData().getLong(ExpiryKey);
    }

    public static List<TamableAnimal> palsOf(ServerLevel World, ServerPlayer Owner) {
        List<TamableAnimal> Found = new ArrayList<>(World.getEntitiesOfClass(TamableAnimal.class, Owner.getBoundingBox().inflate(SearchRadius),
                Pal -> Pal.isAlive() && isPal(Pal) && Owner.getUUID().equals(Pal.getOwnerUUID())));
        Found.sort(Comparator.comparingLong(Scp261Pals::expiryOf));
        return Found;
    }

    public static Optional<TamableAnimal> summon(ServerLevel World, ServerPlayer Owner) {
        List<TamableAnimal> Existing = palsOf(World, Owner);
        while (Existing.size() >= MaxPerOwner) {
            poof(Existing.removeFirst());
        }
        EntityType<? extends TamableAnimal> Breed = World.getRandom().nextBoolean() ? EntityType.WOLF : EntityType.CAT;
        TamableAnimal Pal = Breed.create(World);
        if (Pal == null) {
            return Optional.empty();
        }
        Vec3 Spot = spotNear(World, Owner, Pal);
        Pal.moveTo(Spot.x, Spot.y, Spot.z, Owner.getYRot() + 180.0f, 0.0f);
        Pal.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.MOB_SUMMONED, null);
        Pal.setBaby(true);
        Pal.setTame(true, true);
        Pal.setOwnerUUID(Owner.getUUID());
        Pal.addTag(PalTag);
        Pal.getPersistentData().putLong(ExpiryKey, World.getGameTime() + Lifetime);
        if (!World.addFreshEntity(Pal)) {
            return Optional.empty();
        }
        Owned.computeIfAbsent(Owner.getUUID(), Key -> new LinkedHashSet<>()).add(Pal.getUUID());
        World.sendParticles(ParticleTypes.POOF, Spot.x, Spot.y + 0.3, Spot.z, 10, 0.25, 0.2, 0.25, 0.02);
        World.sendParticles(ParticleTypes.HEART, Spot.x, Spot.y + 0.7, Spot.z, 2, 0.2, 0.1, 0.2, 0.0);
        Scp261Registry.playCue(World, Spot, Scp261Registry.SummonCue, SoundSource.NEUTRAL, 0.7f, 1.0f + World.getRandom().nextFloat() * 0.2f);
        return Optional.of(Pal);
    }

    private static Vec3 spotNear(ServerLevel World, ServerPlayer Owner, Entity Pal) {
        Vec3 Look = Owner.getLookAngle().multiply(1.0, 0.0, 1.0);
        Vec3 Ahead = Look.lengthSqr() < 1.0e-4 ? Vec3.ZERO : Look.normalize();
        for (int Try = 0; Try < SpotTries; Try++) {
            double Turn = Try * Math.PI / 4.0;
            Vec3 Offset = new Vec3(Ahead.x * Math.cos(Turn) - Ahead.z * Math.sin(Turn), 0.0, Ahead.x * Math.sin(Turn) + Ahead.z * Math.cos(Turn)).scale(1.2);
            Vec3 Spot = Owner.position().add(Offset);
            if (World.noCollision(Pal, Pal.getType().getDimensions().makeBoundingBox(Spot))) {
                return Spot;
            }
        }
        return Owner.position();
    }

    public static void tick(Entity Target) {
        if (!(Target.level() instanceof ServerLevel World) || !isPal(Target)) {
            return;
        }
        long Expiry = expiryOf(Target);
        if (Expiry <= 0L || World.getGameTime() >= Expiry) {
            poof(Target);
        }
    }

    public static void poof(Entity Target) {
        if (Target.isRemoved()) {
            return;
        }
        if (Target.level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.POOF, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), 12, 0.25, 0.25, 0.25, 0.02);
            Scp261Registry.playCue(World, Target.position(), Scp261Registry.PoofCue, SoundSource.NEUTRAL, 0.6f, 1.0f + World.getRandom().nextFloat() * 0.2f);
        }
        if (Target instanceof TamableAnimal Pal && Pal.getOwnerUUID() != null) {
            Set<UUID> Mine = Owned.get(Pal.getOwnerUUID());
            if (Mine != null) {
                Mine.remove(Pal.getUUID());
            }
        }
        Target.discard();
    }

    public static int ownerLeft(MinecraftServer Server, UUID Owner) {
        Set<UUID> Mine = Owned.remove(Owner);
        if (Mine == null) {
            return 0;
        }
        int Gone = 0;
        for (UUID Id : List.copyOf(Mine)) {
            for (ServerLevel World : Server.getAllLevels()) {
                Entity Found = World.getEntity(Id);
                if (Found != null && isPal(Found)) {
                    poof(Found);
                    Gone++;
                    break;
                }
            }
        }
        return Gone;
    }

    public static void forgetAll() {
        Owned.clear();
    }
}
