package net.rai.scp_unleashed.z0003;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.mtf.MtfMission;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.scp006.Scp006Mending;
import org.jetbrains.annotations.Nullable;

public final class Z0003Petrify {
    public static final int CrackTicks = 30;
    public static final int MobCrackTicks = 20;
    public static final int[] PlayerCues = {0, 5, 10, 15, 20, 25};
    public static final int[] MobCues = {0, 7, 14};
    public static final int TimeoutTicks = 600;
    public static final double Gravity = 0.08;
    public static final double Drag = 0.98;
    public static final double WaterSink = 0.02;
    public static final double WaterDrag = 0.8;
    public static final int SetParticles = 28;
    public static final int BreakParticles = 64;
    public static final float SetVolume = 0.9f;
    public static final float CrackVolume = 0.8f;
    public static final float BreakVolume = 1.0f;
    public static final int OrphanCheck = 20;
    public static final float PriseVolume = 0.5f;

    private Z0003Petrify() {
    }

    public static boolean isStatue(@Nullable Entity Subject) {
        return Subject != null && Subject.hasData(Z0003Registry.Statue.get());
    }

    @Nullable
    public static Z0003Statue statueOf(@Nullable Entity Subject) {
        return Subject == null ? null : Subject.getExistingDataOrNull(Z0003Registry.Statue.get());
    }

    public static boolean canPetrify(@Nullable Entity Subject) {
        if (!(Subject instanceof LivingEntity Living) || !Living.isAlive() || Living.isRemoved() || isStatue(Living)) {
            return false;
        }
        if (Living instanceof Player User) {
            return !User.isSpectator() && !User.isCreative();
        }
        return Living.getType().is(Z0003Registry.Petrifiable);
    }

    public static boolean canForce(@Nullable Entity Subject) {
        return Subject instanceof LivingEntity Living && Living.isAlive() && !Living.isRemoved() && !isStatue(Living) && !Living.isSpectator();
    }

    public static boolean petrify(LivingEntity Target) {
        return petrify(Target, false);
    }

    public static boolean petrify(LivingEntity Target, boolean Forced) {
        if (!(Target.level() instanceof ServerLevel World) || !(Forced ? canForce(Target) : canPetrify(Target))) {
            return false;
        }
        Scp006Mending.cure(Target, null);
        if (Target.isRemoved() || !Target.isAlive()) {
            return false;
        }
        Target.setData(Z0003Registry.Statue.get(), snapshot(Target, World.getGameTime()));
        Vec3 Motion = Target.getDeltaMovement();
        Target.setDeltaMovement(0.0, Math.min(0.0, Motion.y), 0.0);
        Target.clearFire();
        if (Target instanceof Mob Creature) {
            Creature.getNavigation().stop();
        }
        if (Target instanceof ServerPlayer User && User.containerMenu != User.inventoryMenu) {
            User.closeContainer();
        }
        Vec3 Centre = Target.getBoundingBox().getCenter();
        Z0003Registry.play(World, Centre, Z0003Registry.PetrifyLayers, SoundSource.NEUTRAL, SetVolume, 0.9f + World.random.nextFloat() * 0.15f);
        dust(World, Target, SetParticles, 0.02);
        return true;
    }

    public static Z0003Statue snapshot(LivingEntity Target, long Now) {
        float Position = Target.walkAnimation.position();
        float Speed = Target.walkAnimation.speed();
        if (Target instanceof Player) {
            Z0003Stride Tracked = Target.getExistingDataOrNull(Z0003Registry.Stride.get());
            if (Tracked != null) {
                Position = Tracked.position();
                Speed = Tracked.speed();
            }
        }
        boolean Crouching = Target.isCrouching() || Target instanceof Player User && User.isShiftKeyDown();
        return new Z0003Statue(Target.yBodyRot, Target.yHeadRot, Target.getXRot(), Position, Speed, Target.attackAnim, Crouching, Target.isSprinting(),
                Target.getX(), Target.getY(), Target.getZ(), Now, Z0003Statue.NotCracking);
    }

    public static int crackTicks(Entity Subject) {
        return Subject instanceof Player ? CrackTicks : MobCrackTicks;
    }

    public static int[] cuesFor(Entity Subject) {
        return Subject instanceof Player ? PlayerCues : MobCues;
    }

    public static boolean crack(LivingEntity Target) {
        Z0003Statue Statue = statueOf(Target);
        if (Statue == null || Statue.cracking() || !(Target.level() instanceof ServerLevel World)) {
            return false;
        }
        Target.setData(Z0003Registry.Statue.get(), Statue.withCrack(World.getGameTime()));
        cue(Target, World, 0);
        return true;
    }

    public static int cueIndex(Entity Subject, long Elapsed) {
        int[] Cues = cuesFor(Subject);
        for (int Index = 0; Index < Cues.length; Index++) {
            if (Cues[Index] == Elapsed) {
                return Index;
            }
        }
        return -1;
    }

    public static float cueStrength(Entity Subject, int Index) {
        int Count = cuesFor(Subject).length;
        return Count <= 1 ? 1.0f : Index / (float) (Count - 1);
    }

    private static void cue(LivingEntity Target, ServerLevel World, int Index) {
        float Strength = cueStrength(Target, Index);
        float Wobble = World.random.nextFloat() * 0.08f;
        Vec3 Centre = Target.getBoundingBox().getCenter();
        if (Strength < 0.34f) {
            Z0003Registry.play(World, Centre, Z0003Registry.CreakLayers, SoundSource.NEUTRAL, 0.45f + Strength * 0.6f, 1.55f - Strength * 0.6f + Wobble);
        } else if (Strength < 0.67f) {
            Z0003Registry.play(World, Centre, Z0003Registry.CrackLayers, SoundSource.NEUTRAL, CrackVolume * (0.8f + Strength * 0.3f), 1.4f - Strength * 0.45f + Wobble);
        } else {
            Z0003Registry.play(World, Centre, Z0003Registry.HeavyCrackLayers, SoundSource.NEUTRAL, 0.85f + Strength * 0.15f, 1.15f - Strength * 0.3f + Wobble);
        }
    }

    public static boolean free(LivingEntity Target) {
        if (!isStatue(Target)) {
            return false;
        }
        Target.removeData(Z0003Registry.Statue.get());
        if (Target.hasData(Z0003Registry.Passing.get())) {
            Target.removeData(Z0003Registry.Passing.get());
        }
        Target.resetFallDistance();
        Target.clearFire();
        Target.setTicksFrozen(0);
        Scp006Mending.restore(Target);
        if (Target.level() instanceof ServerLevel World) {
            Z0003Registry.play(World, Target.getBoundingBox().getCenter(), Z0003Registry.BreakLayers, SoundSource.NEUTRAL, BreakVolume, 0.92f + World.random.nextFloat() * 0.12f);
            dust(World, Target, BreakParticles, 0.15);
            AABB Box = Target.getBoundingBox();
            World.sendParticles(ParticleTypes.POOF, Box.getCenter().x, Box.getCenter().y, Box.getCenter().z, 10, Box.getXsize() * 0.4, Box.getYsize() * 0.3,
                    Box.getZsize() * 0.4, 0.02);
        }
        return true;
    }

    public static void tick(LivingEntity Target, ServerLevel World) {
        Z0003Statue Statue = statueOf(Target);
        if (Statue == null) {
            return;
        }
        long Now = World.getGameTime();
        Target.clearFire();
        Target.setAirSupply(Target.getMaxAirSupply());
        passing(Target, World, Statue);
        if (Statue.cracking()) {
            long Elapsed = Now - Statue.CrackStart();
            if (Elapsed >= crackTicks(Target)) {
                free(Target);
                return;
            }
            int Index = cueIndex(Target, Elapsed);
            if (Index > 0) {
                cue(Target, World, Index);
            }
            return;
        }
        if (Target instanceof Player && timesOut(World) && Statue.age(Now) >= TimeoutTicks - CrackTicks) {
            crack(Target);
        }
    }

    public static boolean timesOut(ServerLevel World) {
        return Z0003Host.isLocal(World.getServer()) || Z0003Registry.playersTimeOut(World.getGameRules());
    }

    public static long passesAt(Z0003Statue Statue) {
        return Statue.Since() + TimeoutTicks;
    }

    private static void passing(LivingEntity Target, ServerLevel World, Z0003Statue Statue) {
        if (!(Target instanceof Player)) {
            return;
        }
        long Wanted = timesOut(World) ? passesAt(Statue) : 0L;
        Long Known = Target.getExistingDataOrNull(Z0003Registry.Passing.get());
        long Have = Known == null ? 0L : Known;
        if (Wanted == Have) {
            return;
        }
        if (Wanted == 0L) {
            Target.removeData(Z0003Registry.Passing.get());
        } else {
            Target.setData(Z0003Registry.Passing.get(), Wanted);
        }
    }

    public static void prise(ServerLevel World, LivingEntity Target) {
        AABB Box = Target.getBoundingBox();
        Vec3 Hand = Box.getCenter().add(0.0, -Box.getYsize() * 0.1, 0.0);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), Hand.x, Hand.y, Hand.z, 8, Box.getXsize() * 0.25, 0.15,
                Box.getZsize() * 0.25, 0.02);
        World.sendParticles(new DustParticleOptions(Z0003VialItem.Fluid, 0.6f), Hand.x, Hand.y, Hand.z, 4, 0.15, 0.1, 0.15, 0.0);
        Z0003Registry.play(World, Hand, Z0003Registry.CreakLayers, SoundSource.PLAYERS, PriseVolume, 1.6f + World.random.nextFloat() * 0.15f);
        World.playSound(null, Hand.x, Hand.y, Hand.z, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.35f, 0.7f + World.random.nextFloat() * 0.1f);
    }

    public static void hold(LivingEntity Target, ServerLevel World) {
        tick(Target, World);
        if (!isStatue(Target)) {
            return;
        }
        if (Target instanceof MtfOperative Operative && orphaned(Operative, World)) {
            Operative.discard();
            return;
        }
        Entity Vehicle = Target.getVehicle();
        if (Vehicle != null) {
            Vehicle.positionRider(Target);
            return;
        }
        if (Target.onGround() || Target.isNoGravity()) {
            Target.setDeltaMovement(Vec3.ZERO);
            return;
        }
        double Fall = Target.getDeltaMovement().y;
        if (Target.isInWater()) {
            Fall = (Fall - WaterSink) * WaterDrag;
        } else {
            Fall = (Fall - Gravity) * Drag;
        }
        Target.move(MoverType.SELF, new Vec3(0.0, Fall, 0.0));
        Target.setDeltaMovement(0.0, Target.onGround() ? 0.0 : Fall, 0.0);
    }

    private static boolean orphaned(MtfOperative Operative, ServerLevel World) {
        Z0003Statue Statue = statueOf(Operative);
        if (Statue == null || Operative.tickCount % OrphanCheck != 0 || Statue.age(World.getGameTime()) < MtfOperative.OrphanLimit) {
            return false;
        }
        MtfMission Mission = Operative.mission();
        return Mission == null || !Mission.operatives().contains(Operative.getUUID());
    }

    private static void dust(ServerLevel World, LivingEntity Target, int Count, double Speed) {
        AABB Box = Target.getBoundingBox();
        Vec3 Centre = Box.getCenter();
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), Centre.x, Centre.y, Centre.z, Count,
                Box.getXsize() * 0.35, Box.getYsize() * 0.35, Box.getZsize() * 0.35, Speed);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState()), Centre.x, Centre.y, Centre.z, Count / 2,
                Box.getXsize() * 0.35, Box.getYsize() * 0.35, Box.getZsize() * 0.35, Speed);
    }
}
