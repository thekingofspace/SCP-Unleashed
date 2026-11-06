package net.rai.scp_unleashed.scp553;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp553Colony {
    public static final int SwarmThreshold = 10;
    public static final double PopulationRadius = 32.0;
    public static final int AreaCap = 20;
    public static final int WorldCap = 64;
    public static final int DarkLimit = 5;
    public static final int BreedReach = 7;
    public static final int BreedRise = 8;
    public static final int BreedSamples = 28;
    public static final int StalactiteSpacing = 2;
    public static final double SpitReach = 5.5;
    public static final int SpitPuffs = 4;
    public static final int SpitGlints = 5;
    public static final double SpitPuffPush = 0.12;

    private Scp553Colony() {
    }

    public static boolean dark(BlockGetter World, BlockPos Pos) {
        if (World instanceof Level Placed) {
            return Placed.getRawBrightness(Pos, 0) < DarkLimit;
        }
        return false;
    }

    public static AABB colonyBox(Vec3 Centre) {
        return AABB.ofSize(Centre, PopulationRadius * 2.0, PopulationRadius * 2.0, PopulationRadius * 2.0);
    }

    public static List<Scp553Butterfly> colony(Level World, Vec3 Centre) {
        double Reach = PopulationRadius * PopulationRadius;
        return World.getEntitiesOfClass(Scp553Butterfly.class, colonyBox(Centre), Found -> Found.isAlive() && Found.position().distanceToSqr(Centre) <= Reach);
    }

    public static int population(Level World, Vec3 Centre) {
        return colony(World, Centre).size();
    }

    public static boolean swarming(int Population) {
        return Population >= SwarmThreshold;
    }

    public static int stalactitesNear(ServerLevel World, BlockPos Centre, int Radius) {
        int Count = 0;
        int Reach = SectionPos.blockToSectionCoord(Radius) + 1;
        ChunkPos Middle = new ChunkPos(Centre);
        double Limit = (double) Radius * Radius;
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                if (!World.hasChunk(Middle.x + Dx, Middle.z + Dz)) {
                    continue;
                }
                for (BlockEntity Found : World.getChunk(Middle.x + Dx, Middle.z + Dz).getBlockEntities().values()) {
                    if (Found instanceof Scp553StalactiteBlockEntity && Found.getBlockPos().distSqr(Centre) <= Limit) {
                        Count++;
                    }
                }
            }
        }
        return Count;
    }

    public static boolean worldFull(ServerLevel World) {
        return Scp553World.get(World.getServer()).count() >= WorldCap;
    }

    public static boolean roomToBreed(ServerLevel World, BlockPos Pos) {
        if (worldFull(World)) {
            return false;
        }
        int Pending = stalactitesNear(World, Pos, (int) PopulationRadius);
        return population(World, Vec3.atCenterOf(Pos)) + Pending < AreaCap;
    }

    public static boolean roomToHatch(ServerLevel World, BlockPos Pos) {
        return !worldFull(World) && population(World, Vec3.atCenterOf(Pos)) < AreaCap;
    }

    public static boolean isRock(BlockState State) {
        return State.is(Scp553Registry.BreedingRock);
    }

    public static boolean canHang(Level World, BlockPos Spot) {
        BlockPos Anchor = Spot.above();
        BlockState Above = World.getBlockState(Anchor);
        return isRock(Above) && Above.isFaceSturdy(World, Anchor, Direction.DOWN);
    }

    public static boolean nestable(ServerLevel World, BlockPos Spot) {
        if (!World.isLoaded(Spot) || World.isOutsideBuildHeight(Spot)) {
            return false;
        }
        BlockState At = World.getBlockState(Spot);
        if (!At.isAir() || !At.getFluidState().isEmpty() || !canHang(World, Spot) || !dark(World, Spot)) {
            return false;
        }
        for (BlockPos Near : BlockPos.betweenClosed(Spot.offset(-StalactiteSpacing, -StalactiteSpacing, -StalactiteSpacing), Spot.offset(StalactiteSpacing, StalactiteSpacing, StalactiteSpacing))) {
            if (World.getBlockState(Near).is(Scp553Registry.Stalactite.get())) {
                return false;
            }
        }
        return true;
    }

    public static Vec3 faceOf(BlockPos Spot) {
        return new Vec3(Spot.getX() + 0.5, Spot.getY() + 0.98, Spot.getZ() + 0.5);
    }

    public static boolean clearShot(Level World, Vec3 From, BlockPos Spot, @Nullable Scp553Butterfly Shooter) {
        Vec3 Face = faceOf(Spot).add(0.0, 0.1, 0.0);
        ClipContext Line = Shooter != null ? new ClipContext(From, Face, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, Shooter)
                : new ClipContext(From, Face, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty());
        BlockHitResult Hit = World.clip(Line);
        return Hit.getType() == HitResult.Type.MISS || Hit.getBlockPos().equals(Spot.above()) && Hit.getDirection() == Direction.DOWN;
    }

    @Nullable
    public static BlockPos findNest(ServerLevel World, Scp553Butterfly Butterfly) {
        RandomSource Random = Butterfly.getRandom();
        BlockPos Origin = Butterfly.blockPosition();
        Vec3 Eye = Butterfly.getEyePosition();
        BlockPos Best = null;
        double Closest = Double.MAX_VALUE;
        for (int Sample = 0; Sample <= BreedSamples; Sample++) {
            int Dx = Sample == 0 ? 0 : Random.nextInt(BreedReach * 2 + 1) - BreedReach;
            int Dz = Sample == 0 ? 0 : Random.nextInt(BreedReach * 2 + 1) - BreedReach;
            BlockPos Column = Origin.offset(Dx, 0, Dz);
            if (!World.isLoaded(Column)) {
                continue;
            }
            BlockPos Spot = ceilingAbove(World, Column, Origin.getY() - 2, BreedRise);
            if (Spot == null || !nestable(World, Spot)) {
                continue;
            }
            double Distance = Vec3.atCenterOf(Spot).distanceToSqr(Eye);
            if (Distance < Closest && clearShot(World, Eye, Spot, Butterfly)) {
                Closest = Distance;
                Best = Spot.immutable();
            }
        }
        return Best;
    }

    @Nullable
    public static BlockPos ceilingAbove(Level World, BlockPos Column, int FromY, int Rise) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(Column.getX(), FromY, Column.getZ());
        boolean Open = false;
        for (int Step = 0; Step <= Rise + 2; Step++) {
            BlockState State = World.getBlockState(Cursor);
            if (State.isAir()) {
                Open = true;
            } else if (Open) {
                return Cursor.below().immutable();
            }
            Cursor.move(Direction.UP);
        }
        return null;
    }

    public static Vec3 hoverBelow(Level World, BlockPos Spot, Scp553Butterfly Butterfly) {
        RandomSource Random = Butterfly.getRandom();
        for (int Attempt = 0; Attempt < 8; Attempt++) {
            double Drop = 1.4 + Random.nextDouble() * 1.2;
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Side = Attempt < 4 ? 0.4 + Random.nextDouble() * 0.8 : 0.0;
            Vec3 Point = new Vec3(Spot.getX() + 0.5 + Math.cos(Angle) * Side, Spot.getY() + 0.5 - Drop, Spot.getZ() + 0.5 + Math.sin(Angle) * Side);
            AABB Box = Butterfly.getDimensions(Butterfly.getPose()).makeBoundingBox(Point);
            if (World.noCollision(Butterfly, Box) && !World.containsAnyLiquid(Box) && clearShot(World, Point.add(0.0, Scp553Butterfly.EyeHeight, 0.0), Spot, Butterfly)) {
                return Point;
            }
        }
        return Butterfly.position();
    }

    public static boolean seed(ServerLevel World, BlockPos Spot) {
        if (!nestable(World, Spot)) {
            return false;
        }
        World.setBlock(Spot, Scp553Registry.Stalactite.get().defaultBlockState(), Block.UPDATE_ALL);
        Vec3 Face = faceOf(Spot);
        World.sendParticles(Scp553Registry.Glint.get(), Face.x, Face.y - 0.1, Face.z, 8, 0.15, 0.05, 0.15, 0.01);
        play(World, Face, Scp553Registry.GrowLayers, 1.1f, 0.8f);
        return true;
    }

    public static void spit(ServerLevel World, Scp553Butterfly Butterfly, Vec3 Mouth) {
        Vec3 Ahead = Butterfly.getLookAngle().scale(SpitPuffPush);
        World.sendParticles(ParticleTypes.SPIT, Mouth.x, Mouth.y, Mouth.z, SpitPuffs, 0.04, 0.04, 0.04, 0.02);
        World.sendParticles(Scp553Registry.Glint.get(), Mouth.x + Ahead.x, Mouth.y + Ahead.y, Mouth.z + Ahead.z, SpitGlints, 0.06, 0.06, 0.06, 0.02);
        Butterfly.playLayers(Scp553Registry.SpitLayers, 1.0f, 0.95f + World.getRandom().nextFloat() * 0.12f);
    }

    public static void spitTrail(ServerLevel World, Vec3 From, Vec3 To) {
        int Steps = Math.max(3, (int) Math.ceil(From.distanceTo(To) * 3.0));
        for (int Step = 1; Step <= Steps; Step++) {
            Vec3 Point = From.lerp(To, Step / (double) Steps);
            World.sendParticles(Scp553Registry.Glint.get(), Point.x, Point.y, Point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch, float Volume) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.NEUTRAL, Volume, Varied);
        }
    }
}
