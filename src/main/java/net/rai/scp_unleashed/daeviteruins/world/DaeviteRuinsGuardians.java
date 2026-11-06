package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;
import net.rai.scp_unleashed.daeviteruins.plan.RuinDanger;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPages;

import java.util.Iterator;

public final class DaeviteRuinsGuardians {
    public static final String Tag = "scp_unleashed_daevite_guardian";
    public static final float RobeDrop = 0.06f;

    private DaeviteRuinsGuardians() {
    }

    public static EntityType<? extends Mob> typeFor(int Kind) {
        return switch (Kind) {
            case RuinDanger.Skeleton -> EntityType.SKELETON;
            case RuinDanger.Stray -> EntityType.STRAY;
            case RuinDanger.Vindicator -> EntityType.VINDICATOR;
            case RuinDanger.WitherSkeleton -> EntityType.WITHER_SKELETON;
            case RuinDanger.Evoker -> EntityType.EVOKER;
            case RuinDanger.Pillager -> EntityType.PILLAGER;
            case RuinDanger.Blaze -> EntityType.BLAZE;
            default -> EntityType.HUSK;
        };
    }

    public static boolean archer(int Kind) {
        return Kind == RuinDanger.Skeleton || Kind == RuinDanger.Stray || Kind == RuinDanger.Pillager;
    }

    public static int wakeRadius(int Tier) {
        return Math.min(32, 14 + Tier * 4);
    }

    public static boolean near(ServerLevel World, DaeviteRuin Ruin, double Radius) {
        for (Player User : World.players()) {
            if (User.isSpectator() || !User.isAlive()) {
                continue;
            }
            double Dx = User.getX() - (Ruin.Centre.getX() + 0.5);
            double Dz = User.getZ() - (Ruin.Centre.getZ() + 0.5);
            if (Dx * Dx + Dz * Dz <= Radius * Radius && Math.abs(User.getY() - Ruin.Centre.getY()) <= Radius) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos free(ServerLevel World, BlockPos Pos) {
        for (int Reach = 0; Reach <= 3; Reach++) {
            for (int Dy = 0; Dy <= 2; Dy++) {
                for (int Dx = -Reach; Dx <= Reach; Dx++) {
                    for (int Dz = -Reach; Dz <= Reach; Dz++) {
                        BlockPos Candidate = Pos.offset(Dx, Dy, Dz);
                        if (World.getBlockState(Candidate).getCollisionShape(World, Candidate).isEmpty()
                                && World.getBlockState(Candidate.above()).getCollisionShape(World, Candidate.above()).isEmpty()
                                && !World.getBlockState(Candidate.below()).getCollisionShape(World, Candidate.below()).isEmpty()
                                && World.getFluidState(Candidate).isEmpty()) {
                            return Candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static int wake(ServerLevel World, DaeviteRuin Ruin) {
        int Tier = RuinPages.tierOf(Ruin.Page);
        int Count = 0;
        Iterator<DaeviteRuin.Guardian> Pending = Ruin.Guardians.iterator();
        while (Pending.hasNext()) {
            DaeviteRuin.Guardian Entry = Pending.next();
            if (!World.isLoaded(Entry.Pos)) {
                continue;
            }
            Pending.remove();
            BlockPos Spot = free(World, Entry.Pos);
            if (Spot == null) {
                continue;
            }
            Mob Body = typeFor(Entry.Kind).create(World);
            if (Body == null) {
                continue;
            }
            float Yaw = Direction.from2DDataValue(Entry.Facing).toYRot();
            Body.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Yaw, 0.0f);
            Body.setYHeadRot(Yaw);
            Body.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.STRUCTURE, null);
            if (Entry.Kind != RuinDanger.Evoker) {
                Body.equip(DaeviteRuinsLoot.gear(Tier, archer(Entry.Kind)), DaeviteRuinsBuilder.dropChances(RobeDrop, archer(Entry.Kind)));
            }
            Body.setPersistenceRequired();
            Body.addTag(Tag);
            World.addFreshEntity(Body);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, Spot.getX() + 0.5, Spot.getY() + 0.4, Spot.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.01);
            World.sendParticles(ParticleTypes.FLAME, Spot.getX() + 0.5, Spot.getY() + 0.2, Spot.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.02);
            World.sendParticles(ParticleTypes.ASH, Spot.getX() + 0.5, Spot.getY() + 1.0, Spot.getZ() + 0.5, 30, 0.5, 0.8, 0.5, 0.01);
            Count++;
        }
        if (Count > 0) {
            World.playSound(null, Ruin.Centre, DaeviteRuinsRegistry.RuinWake.get(), SoundSource.HOSTILE, 1.6f, 0.8f);
        }
        Ruin.Woken = Ruin.Guardians.isEmpty();
        DaeviteRuinsWorld.get(World.getServer()).setDirty();
        return Count;
    }
}
