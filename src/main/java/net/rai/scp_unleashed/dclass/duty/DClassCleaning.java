package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp173.Scp173ResidueBlock;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DClassCleaning {
    public static final int ScrubTicks = 50;
    public static final int StrokeGap = 9;
    public static final double ScrubReach = 1.6;
    public static final double ScrubRise = 1.5;
    public static final int StuckTicks = 80;
    public static final int SkipTicks = 600;
    public static final double WalkSpeed = 0.62;
    public static final int RepathTicks = 10;
    public static final int RouteTicks = 20;
    public static final double NodeReach = 0.45;
    public static final float ScrubVolume = 0.55f;

    public enum Outcome {
        Idle,
        Working,
        Cleaned,
        Gone
    }

    private DClassCleaning() {
    }

    public static boolean cleanable(BlockState State) {
        return State.is(DClassDutyRegistry.Cleanable);
    }

    public static List<BlockPos> find(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        List<BlockPos> Found = new ArrayList<>();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dy = -Height; Dy <= Height; Dy++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    Cursor.set(Centre.getX() + Dx, Centre.getY() + Dy, Centre.getZ() + Dz);
                    if (World.isLoaded(Cursor) && cleanable(World.getBlockState(Cursor))) {
                        Found.add(Cursor.immutable());
                    }
                }
            }
        }
        return Found;
    }

    public static int count(ServerLevel World, BlockPos Centre, int Radius, int Height) {
        return find(World, Centre, Radius, Height).size();
    }

    public static boolean clean(ServerLevel World, BlockPos Pos, @Nullable DClass By) {
        BlockState State = World.getBlockState(Pos);
        if (!cleanable(State)) {
            return false;
        }
        if (State.getBlock() instanceof Scp173ResidueBlock) {
            Scp173ResidueBlock.scrape(World, Pos, State, null);
        } else {
            World.destroyBlock(Pos, false, By);
        }
        play(World, Vec3.atBottomCenterOf(Pos), DClassDutyRegistry.ScrubDoneLayers, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        return true;
    }

    public static int scrubTicks(DClass Owner) {
        float Scale = switch (Owner.trait()) {
            case Lazy -> 1.6f;
            case Gluttonous, Insomniac -> 1.15f;
            case Restless, Obedient -> 0.85f;
            default -> 1.0f;
        };
        return Math.round(ScrubTicks * Scale);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y + 0.1, At.z, Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }

    public static final class Worker {
        private final DClass Owner;
        private final Map<BlockPos, Long> Skipped = new HashMap<>();
        @Nullable
        private BlockPos Target;
        @Nullable
        private Path Route;
        private boolean Strafing;
        private int Scrubbing;
        private int Walking;
        private int SinceBest;
        private double Best;

        public Worker(DClass Owner) {
            this.Owner = Owner;
        }

        @Nullable
        public BlockPos target() {
            return Target;
        }

        public boolean hasTarget() {
            return Target != null;
        }

        public boolean scrubbing() {
            return Scrubbing > 0;
        }

        public int scrubbed() {
            return Scrubbing;
        }

        public int walked() {
            return Walking;
        }

        public boolean pick(ServerLevel World, BlockPos Centre, int Radius, int Height) {
            long Now = World.getGameTime();
            Skipped.values().removeIf(Until -> Until <= Now);
            List<BlockPos> Found = find(World, Centre, Radius, Height);
            Found.removeIf(Skipped::containsKey);
            if (Found.isEmpty()) {
                return false;
            }
            Vec3 Feet = Owner.position();
            Found.sort(Comparator.comparingDouble(Pos -> Vec3.atBottomCenterOf(Pos).distanceToSqr(Feet)));
            begin(Found.get(0));
            return true;
        }

        public void begin(BlockPos Pos) {
            Target = Pos.immutable();
            Route = null;
            Scrubbing = 0;
            Walking = 0;
            SinceBest = 0;
            Best = Double.MAX_VALUE;
        }

        public void skip(ServerLevel World, BlockPos Pos) {
            Skipped.put(Pos.immutable(), World.getGameTime() + SkipTicks);
        }

        public Outcome tick(ServerLevel World, @Nullable Scp173Statue Watched) {
            if (Target == null) {
                return Outcome.Idle;
            }
            BlockState State = World.getBlockState(Target);
            if (!cleanable(State)) {
                release();
                return Outcome.Gone;
            }
            double Dx = Target.getX() + 0.5 - Owner.getX();
            double Dz = Target.getZ() + 0.5 - Owner.getZ();
            double Dy = Target.getY() - Owner.getY();
            double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
            if (Flat <= ScrubReach && Math.abs(Dy) <= ScrubRise) {
                return scrub(World, State, Watched);
            }
            if (Scrubbing > 0) {
                Scrubbing = 0;
                pose(false);
            }
            Walking++;
            if (Flat + 0.2 < Best) {
                Best = Flat;
                SinceBest = 0;
            } else if (++SinceBest > StuckTicks) {
                skip(World, Target);
                release();
                return Outcome.Gone;
            }
            if (Watched != null && facingWalk(Target, Watched, Walking)) {
                return Outcome.Working;
            }
            halt();
            Route = null;
            if (Walking % RepathTicks == 1 || Owner.getNavigation().isDone()) {
                boolean Moving = Owner.getNavigation().moveTo(Target.getX() + 0.5, Target.getY(), Target.getZ() + 0.5, WalkSpeed);
                if (!Moving && Walking > RepathTicks * 3) {
                    skip(World, Target);
                    release();
                    return Outcome.Gone;
                }
            }
            return Outcome.Working;
        }

        private Outcome scrub(ServerLevel World, BlockState State, @Nullable Scp173Statue Watched) {
            if (Target == null) {
                return Outcome.Idle;
            }
            Owner.getNavigation().stop();
            halt();
            Route = null;
            if (Scrubbing == 0) {
                pose(true);
            }
            Scrubbing++;
            Vec3 Spot = Vec3.atBottomCenterOf(Target);
            if (Watched == null) {
                Owner.getLookControl().setLookAt(Spot.x, Spot.y, Spot.z, 20.0f, 40.0f);
            }
            if (Scrubbing % StrokeGap == 1) {
                play(World, Spot, DClassDutyRegistry.ScrubLayers, ScrubVolume, 0.9f + World.getRandom().nextFloat() * 0.2f);
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, State), Spot.x, Spot.y + 0.1, Spot.z, 3, 0.25, 0.02, 0.25, 0.02);
                World.sendParticles(ParticleTypes.SPLASH, Spot.x, Spot.y + 0.15, Spot.z, 2, 0.25, 0.02, 0.25, 0.05);
            }
            if (Scrubbing < scrubTicks(Owner)) {
                return Outcome.Working;
            }
            clean(World, Target, Owner);
            release();
            return Outcome.Cleaned;
        }

        public boolean approach(BlockPos Goal, @Nullable Scp173Statue Watched, int Clock, double Speed) {
            if (Watched != null && facingWalk(Goal, Watched, Clock)) {
                return true;
            }
            halt();
            Route = null;
            if (Clock % RepathTicks == 1 || Owner.getNavigation().isDone()) {
                return Owner.getNavigation().moveTo(Goal.getX() + 0.5, Goal.getY(), Goal.getZ() + 0.5, Speed);
            }
            return true;
        }

        private boolean facingWalk(BlockPos Goal, Scp173Statue Watched, int Clock) {
            if (Route == null || Route.isDone() || Clock % RouteTicks == 1) {
                Route = Owner.getNavigation().createPath(Goal, 0);
                Owner.getNavigation().stop();
            }
            if (Route == null) {
                return false;
            }
            Vec3 Next = null;
            while (!Route.isDone()) {
                Vec3 Node = Route.getNextEntityPos(Owner);
                if (Node.subtract(Owner.position()).horizontalDistance() > NodeReach) {
                    Next = Node;
                    break;
                }
                Route.advance();
            }
            if (Next == null) {
                Next = Vec3.atBottomCenterOf(Goal);
            }
            double Wx = Next.x - Owner.getX();
            double Wz = Next.z - Owner.getZ();
            double Length = Math.sqrt(Wx * Wx + Wz * Wz);
            if (Length < 1.0e-3) {
                return true;
            }
            Wx /= Length;
            Wz /= Length;
            float Yaw = (float) (Mth.atan2(Watched.getZ() - Owner.getZ(), Watched.getX() - Owner.getX()) * Mth.RAD_TO_DEG) - 90.0f;
            Owner.setYRot(Yaw);
            Owner.setYBodyRot(Yaw);
            float Sin = Mth.sin(Yaw * Mth.DEG_TO_RAD);
            float Cos = Mth.cos(Yaw * Mth.DEG_TO_RAD);
            float Forward = (float) (-Wx * Sin + Wz * Cos);
            float Side = (float) (Wx * Cos + Wz * Sin);
            Owner.getMoveControl().strafe(Forward, Side);
            Strafing = true;
            if (Next.y > Owner.getY() + 0.5 && Owner.onGround()) {
                Owner.getJumpControl().jump();
            }
            return true;
        }

        public void halt() {
            if (Strafing) {
                Strafing = false;
                Owner.setXxa(0.0f);
                Owner.setZza(0.0f);
            }
        }

        private void pose(boolean Scrub) {
            if (Scrub) {
                if (Owner.action() == DClassLike.ActionNone) {
                    Owner.setAction(DClassDuties.ActionScrubbing);
                }
            } else if (Owner.action() == DClassDuties.ActionScrubbing) {
                Owner.setAction(DClassLike.ActionNone);
            }
        }

        public void release() {
            if (Scrubbing > 0 || Owner.action() == DClassDuties.ActionScrubbing) {
                pose(false);
            }
            halt();
            Target = null;
            Route = null;
            Scrubbing = 0;
            Walking = 0;
            SinceBest = 0;
            Best = Double.MAX_VALUE;
        }

        public void stop() {
            release();
            Owner.getNavigation().stop();
        }
    }
}
