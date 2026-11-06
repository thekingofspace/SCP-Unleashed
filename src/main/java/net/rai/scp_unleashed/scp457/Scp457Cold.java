package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerIndex;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp457Cold {
    public static final int SnuffTicks = 60;
    public static final int ThawStep = 2;
    public static final int SenseBase = 12;
    public static final int SenseSpread = 18;
    public static final float GutterDepth = 0.7f;
    public static final int HissEvery = 12;
    public static final int CrackleEvery = 9;
    public static final float CrackleVolume = 0.6f;
    public static final float SnuffVolume = 1.2f;
    public static final int FleeRange = 16;
    public static final int FleeHeight = 7;
    public static final double SenseReach = 4.0;

    private Scp457Cold() {
    }

    public static int chill(Scp457 Burning) {
        return Burning.getEntityData().get(Scp457.ChillData);
    }

    public static float chillShare(Scp457 Burning) {
        return Mth.clamp(chill(Burning) / (float) SnuffTicks, 0.0f, 1.0f);
    }

    public static float flameScale(Scp457 Burning) {
        return 1.0f - GutterDepth * chillShare(Burning);
    }

    public static boolean cold(Level World, Vec3 Point) {
        return CoolerIndex.cooling(World, Point);
    }

    public static boolean chilly(LevelReader Reader, BlockPos Pos) {
        return Reader instanceof Level World && cold(World, Vec3.atBottomCenterOf(Pos));
    }

    public static int senseDelay(Scp457 Burning) {
        return SenseBase + Math.floorMod(Burning.getUUID().hashCode(), SenseSpread + 1);
    }

    public static boolean tick(ServerLevel World, Scp457 Burning) {
        if (Burning.isRising()) {
            return false;
        }
        boolean Cold = cold(World, Burning.position());
        int Before = chill(Burning);
        int After = Cold ? Before + 1 : Math.max(0, Before - ThawStep);
        if (After != Before) {
            Burning.getEntityData().set(Scp457.ChillData, (byte) Math.min(After, SnuffTicks));
        }
        if (!Cold || After <= 0) {
            return false;
        }
        gutter(World, Burning, After);
        if (After >= SnuffTicks) {
            snuff(World, Burning);
            return true;
        }
        return false;
    }

    private static void play(ServerLevel World, Vec3 At, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Sound, SoundSource.HOSTILE, Volume, Pitch);
    }

    public static void gutter(ServerLevel World, Scp457 Burning, int Chill) {
        float Share = Chill / (float) SnuffTicks;
        double Middle = Burning.getY() + Burning.getBbHeight() * 0.6;
        int Steam = 1 + Math.round(Share * 5.0f);
        World.sendParticles(Scp457Registry.Steam.get(), Burning.getX(), Middle, Burning.getZ(), Steam, 0.3, 0.6, 0.3, 0.015);
        if (Chill % 2 == 0) {
            World.sendParticles(ParticleTypes.SMOKE, Burning.getX(), Middle + 0.4, Burning.getZ(), 1 + Math.round(Share * 3.0f), 0.25, 0.4, 0.25, 0.01);
            World.sendParticles(ParticleTypes.SNOWFLAKE, Burning.getX(), Middle, Burning.getZ(), 1 + Math.round(Share * 4.0f), 0.4, 0.7, 0.4, 0.02);
        }
        if (Chill % HissEvery == 1) {
            play(World, Burning.position(), Scp457Registry.Hiss.get(), 0.6f + Share * 0.5f, 1.1f - Share * 0.3f);
        }
        if (Chill % CrackleEvery == 4) {
            play(World, Burning.position(), Scp457Registry.FrostCrackle.get(), CrackleVolume * (0.5f + Share), 0.9f + World.getRandom().nextFloat() * 0.2f);
        }
    }

    public static void snuff(ServerLevel World, Scp457 Burning) {
        if (Burning.isDeadOrDying() || Burning.isRemoved()) {
            return;
        }
        Burning.getEntityData().set(Scp457.DeathData, Scp457.DeathFrost);
        double Middle = Burning.getY() + Burning.getBbHeight() * 0.55;
        World.sendParticles(Scp457Registry.Steam.get(), Burning.getX(), Middle, Burning.getZ(), 36, 0.45, 0.8, 0.45, 0.04);
        World.sendParticles(ParticleTypes.SNOWFLAKE, Burning.getX(), Middle, Burning.getZ(), 40, 0.5, 0.9, 0.5, 0.05);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Burning.getX(), Middle, Burning.getZ(), 10, 0.35, 0.6, 0.35, 0.02);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp457Registry.FrostLayers) {
            play(World, Burning.position(), Layer.get(), SnuffVolume, 1.0f);
        }
        DamageSource Source = Burning.damageSources().freeze();
        Burning.hurt(Source, Float.MAX_VALUE);
        if (!Burning.isDeadOrDying()) {
            Burning.setHealth(0.0f);
            Burning.die(Source);
        }
    }

    public static void collapsing(ServerLevel World, Scp457 Burning, int Time, double Middle) {
        if (Burning.deathKind() != Scp457.DeathFrost || Time % 3 != 0) {
            return;
        }
        World.sendParticles(ParticleTypes.SNOWFLAKE, Burning.getX(), Middle, Burning.getZ(), 4, 0.35, 0.4, 0.35, 0.01);
    }

    public static void ashes(ServerLevel World, Scp457 Burning) {
        if (Burning.deathKind() != Scp457.DeathFrost) {
            return;
        }
        World.sendParticles(ParticleTypes.SNOWFLAKE, Burning.getX(), Burning.getY() + 0.3, Burning.getZ(), 24, 0.5, 0.2, 0.5, 0.02);
        play(World, Burning.position(), Scp457Registry.FrostCrackle.get(), CrackleVolume, 0.8f);
    }

    public static void frostTick(Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextFloat() < 0.12f) {
            double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.5;
            double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.5;
            World.addParticle(ParticleTypes.SNOWFLAKE, X, Pos.getY() + 0.2, Z, 0.0, 0.01, 0.0);
        }
    }

    public static void thaw(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.getValue(Scp457AshPileBlock.Frosted) && !cold(World, Vec3.atCenterOf(Pos))) {
            World.setBlock(Pos, State.setValue(Scp457AshPileBlock.Frosted, false), Block.UPDATE_ALL);
        }
    }

    @Nullable
    public static Vec3 nearestCooler(Level World, Vec3 From) {
        BlockPos Found = CoolerIndex.nearest(World, From, CoolerBlockEntity.Radius + SenseReach);
        return Found == null ? null : Vec3.atCenterOf(Found);
    }

    public static class FleeCold extends Goal {
        private final Scp457 Owner;
        @Nullable
        private Vec3 Refuge;

        public FleeCold(Scp457 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Owner.isRising() || chill(Owner) < senseDelay(Owner)) {
                return false;
            }
            Vec3 Cooler = nearestCooler(Owner.level(), Owner.position());
            if (Cooler == null) {
                return false;
            }
            Refuge = DefaultRandomPos.getPosAway(Owner, FleeRange, FleeHeight, Cooler);
            return Refuge != null && !cold(Owner.level(), Refuge);
        }

        @Override
        public void start() {
            if (Refuge != null) {
                Owner.getNavigation().moveTo(Refuge.x, Refuge.y, Refuge.z, Scp457.FleeSprint);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.getNavigation().isDone() && chill(Owner) > 0;
        }

        @Override
        public void stop() {
            Refuge = null;
        }
    }
}
