package net.rai.scp_unleashed.scp009;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class RedIceSpread {
    public static final int StepDelay = 30;
    public static final int StepJitter = 30;
    public static final int RetryDelay = 40;
    public static final int PerTickBudget = 64;
    public static final int CrackleChance = 8;
    private static final Map<ResourceKey<Level>, long[]> Spent = new HashMap<>();

    private RedIceSpread() {
    }

    public static boolean enabled(ServerLevel World) {
        return Scp009Registry.crystalSpread(World.getGameRules());
    }

    public static void schedule(ServerLevel World, BlockPos Pos, RandomSource Random) {
        World.scheduleTick(Pos, Scp009Registry.RedIce.get(), StepDelay + Random.nextInt(StepJitter + 1));
    }

    public static void step(ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (!enabled(World)) {
            return;
        }
        for (Direction Side : Direction.values()) {
            BlockPos Near = Pos.relative(Side);
            if (!World.isLoaded(Near) || !RedIceGrowth.isWater(World.getBlockState(Near)) || isOccupied(World, Near)) {
                continue;
            }
            if (!take(World)) {
                World.scheduleTick(Pos, Scp009Registry.RedIce.get(), RetryDelay + Random.nextInt(RetryDelay + 1));
                return;
            }
            World.setBlockAndUpdate(Near, Scp009Registry.RedIce.get().defaultBlockState());
            World.sendParticles(Scp009Registry.Glint.get(), Near.getX() + 0.5, Near.getY() + 0.9, Near.getZ() + 0.5, 1, 0.3, 0.1, 0.3, 0.02);
            if (Random.nextInt(CrackleChance) == 0) {
                World.playSound(null, Near, Scp009Registry.Crackle.get(), SoundSource.BLOCKS, 0.5f, 0.85f + Random.nextFloat() * 0.3f);
            }
        }
    }

    public static void clear() {
        Spent.clear();
    }

    private static boolean take(ServerLevel World) {
        long[] Entry = Spent.computeIfAbsent(World.dimension(), Key -> new long[]{Long.MIN_VALUE, 0L});
        long Now = World.getGameTime();
        if (Entry[0] != Now) {
            Entry[0] = Now;
            Entry[1] = 0L;
        }
        if (Entry[1] >= PerTickBudget) {
            return false;
        }
        Entry[1]++;
        return true;
    }

    private static boolean isOccupied(ServerLevel World, BlockPos Pos) {
        return !World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos), LivingEntity::isAlive).isEmpty();
    }
}
