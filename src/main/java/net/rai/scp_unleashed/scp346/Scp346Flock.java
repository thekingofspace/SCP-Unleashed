package net.rai.scp_unleashed.scp346;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Scp346Flock {
    public static final double FlockRadius = 20.0;
    public static final int PerchAttempts = 24;
    public static final int SkyAttempts = 40;
    public static final double ApproachDistance = 20.0;
    public static final double ApproachHeight = 12.0;
    public static final double CallHeight = 5.0;
    public static final double SpawnRing = 1.6;
    public static final double LandingRing = 1.2;

    private Scp346Flock() {
    }

    public static boolean sameFlock(Scp346Pterosaur First, Scp346Pterosaur Second) {
        if (First.isTame() != Second.isTame()) {
            return false;
        }
        return !First.isTame() || Objects.equals(First.getOwnerUUID(), Second.getOwnerUUID());
    }

    public static List<Scp346Pterosaur> mates(Scp346Pterosaur Bird, double Radius) {
        return Bird.level().getEntitiesOfClass(Scp346Pterosaur.class, Bird.getBoundingBox().inflate(Radius),
                Other -> Other != Bird && Other.isAlive() && sameFlock(Bird, Other));
    }

    public static Scp346Pterosaur leader(Scp346Pterosaur Bird) {
        if (Bird.isTame()) {
            return Bird;
        }
        Scp346Pterosaur Best = Bird;
        for (Scp346Pterosaur Other : mates(Bird, FlockRadius)) {
            if (Other.getUUID().compareTo(Best.getUUID()) < 0) {
                Best = Other;
            }
        }
        return Best;
    }

    public static Vec3 centre(Scp346Pterosaur Bird) {
        if (Bird.isTame()) {
            LivingEntity Owner = Bird.getOwner();
            return Owner != null && Owner.level() == Bird.level() ? Owner.position() : Bird.position();
        }
        Scp346Pterosaur Leader = leader(Bird);
        BlockPos Home = Leader.home();
        if (Home != null && Home.distSqr(Leader.blockPosition()) < 48.0 * 48.0) {
            return Vec3.atBottomCenterOf(Home);
        }
        return Leader.position();
    }

    public static Vec3 roostSpot(Scp346Pterosaur Bird, Scp346Pterosaur Leader) {
        if (Leader == Bird) {
            return Bird.position();
        }
        double Turn = (Bird.getUUID().getLeastSignificantBits() & 0xFFFF) / 65536.0 * Mth.TWO_PI;
        return Leader.position().add(Math.cos(Turn) * 1.1, 0.0, Math.sin(Turn) * 1.1);
    }

    public static boolean unsafe(BlockState State) {
        return State.is(BlockTags.CAMPFIRES) || State.is(Blocks.MAGMA_BLOCK) || State.is(Blocks.CACTUS) || State.is(Blocks.SWEET_BERRY_BUSH)
                || State.is(Blocks.POINTED_DRIPSTONE) || State.is(BlockTags.FIRE) || State.is(Blocks.POWDER_SNOW) || !State.getFluidState().isEmpty();
    }

    public static AABB roomAt(Vec3 Spot) {
        double Half = Scp346Pterosaur.Width / 2.0;
        return new AABB(Spot.x - Half, Spot.y + 0.01, Spot.z - Half, Spot.x + Half, Spot.y + Scp346Pterosaur.Height + 0.25, Spot.z + Half);
    }

    public static Optional<Vec3> surface(Level World, BlockPos Column, Heightmap.Types Kind) {
        if (!World.isLoaded(Column)) {
            return Optional.empty();
        }
        BlockPos Top = World.getHeightmapPos(Kind, Column);
        BlockPos Below = Top.below();
        BlockState State = World.getBlockState(Below);
        if (unsafe(State) || !World.getFluidState(Top).isEmpty()) {
            return Optional.empty();
        }
        VoxelShape Shape = State.getCollisionShape(World, Below);
        if (Shape.isEmpty()) {
            return Optional.empty();
        }
        Vec3 Spot = new Vec3(Top.getX() + 0.5, Below.getY() + Shape.max(Direction.Axis.Y), Top.getZ() + 0.5);
        return World.noCollision(roomAt(Spot)) ? Optional.of(Spot) : Optional.empty();
    }

    public static Optional<Vec3> perch(Level World, Vec3 Around, int Radius, int Rise, RandomSource Random) {
        for (int Attempt = 0; Attempt < PerchAttempts; Attempt++) {
            BlockPos Column = BlockPos.containing(Around.x + Random.nextInt(Radius * 2 + 1) - Radius, Around.y, Around.z + Random.nextInt(Radius * 2 + 1) - Radius);
            Optional<Vec3> Found = surface(World, Column, Heightmap.Types.MOTION_BLOCKING);
            if (Found.isPresent() && Math.abs(Found.get().y - Around.y) <= Rise) {
                return Found;
            }
        }
        return Optional.empty();
    }

    public static boolean perchedOnLedge(Scp346Pterosaur Bird) {
        BlockState Under = Bird.level().getBlockState(BlockPos.containing(Bird.getX(), Bird.getY() - 0.2, Bird.getZ()));
        return Under.is(BlockTags.LEAVES) || Under.is(BlockTags.FENCES) || Under.is(BlockTags.WALLS) || Under.is(BlockTags.FENCE_GATES);
    }

    public static Optional<BlockPos> openSky(ServerLevel World, Vec3 Centre, int Low, int High, RandomSource Random, boolean Strict) {
        int Least = Math.min(Low, High);
        int Most = Math.max(Low, High);
        for (int Attempt = 0; Attempt < SkyAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Least + Random.nextDouble() * (Most - Least);
            BlockPos Column = BlockPos.containing(Centre.x + Math.cos(Angle) * Radius, Centre.y, Centre.z + Math.sin(Angle) * Radius);
            Optional<Vec3> Ground = surface(World, Column, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
            if (Ground.isEmpty()) {
                continue;
            }
            BlockPos Spot = BlockPos.containing(Ground.get());
            if (Strict && (!World.canSeeSky(Spot) || Math.abs(Spot.getY() - Centre.y) > 24)) {
                continue;
            }
            if (!World.noCollision(new AABB(Spot).inflate(1.0, 0.0, 1.0).setMaxY(Spot.getY() + 2.5))) {
                continue;
            }
            return Optional.of(Spot);
        }
        return Optional.empty();
    }

    public static boolean clear(Level World, Vec3 Spot) {
        return World.isLoaded(BlockPos.containing(Spot)) && World.noCollision(roomAt(Spot)) && !World.containsAnyLiquid(roomAt(Spot));
    }

    private static Vec3 startFor(ServerLevel World, Vec3 Base, Vec3 Offset, Vec3 Spot, int Index) {
        List<Vec3> Options = new ArrayList<>();
        Options.add(Base.add(Offset));
        Options.add(Spot.add(Offset.x, ApproachHeight, Offset.z));
        Options.add(Spot.add(Offset.x, CallHeight, Offset.z));
        Options.add(Spot.add(Offset.x, 1.5, Offset.z));
        Options.add(Spot.add(0.0, 1.0 + Index * 0.8, 0.0));
        for (Vec3 Option : Options) {
            if (clear(World, Option)) {
                return Option;
            }
        }
        return Spot;
    }

    public static List<Scp346Pterosaur> spawnFlock(ServerLevel World, Vec3 Spot, int Count, @Nullable Scp346Variant Variant, @Nullable Vec3 From, boolean FarApproach) {
        RandomSource Random = World.getRandom();
        Vec3 Away = From == null ? Vec3.ZERO : new Vec3(Spot.x - From.x, 0.0, Spot.z - From.z);
        if (Away.lengthSqr() < 1.0e-4) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            Away = new Vec3(Math.cos(Angle), 0.0, Math.sin(Angle));
        }
        Away = Away.normalize();
        Vec3 Base = FarApproach ? Spot.add(Away.scale(ApproachDistance)).add(0.0, ApproachHeight, 0.0) : Spot.add(0.0, CallHeight, 0.0);
        List<Scp346Pterosaur> Made = new ArrayList<>();
        float Facing = (float) (Mth.atan2(-Away.z, -Away.x) * Mth.RAD_TO_DEG) - 90.0f;
        for (int Index = 0; Index < Count; Index++) {
            Scp346Pterosaur Bird = Scp346Registry.Pterosaur.get().create(World);
            if (Bird == null) {
                continue;
            }
            double Turn = Index * Mth.TWO_PI / Math.max(1, Count) + Random.nextDouble() * 0.5;
            Vec3 Offset = new Vec3(Math.cos(Turn) * SpawnRing, Random.nextDouble() * 1.2, Math.sin(Turn) * SpawnRing);
            Vec3 Start = startFor(World, Base, Offset, Spot, Index);
            Bird.moveTo(Start.x, Start.y, Start.z, Facing, 0.0f);
            Bird.setYHeadRot(Facing);
            Bird.setYBodyRot(Facing);
            Bird.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Start)), MobSpawnType.EVENT, null);
            Bird.setVariant(Variant != null ? Variant : Scp346Variant.random(Random));
            Bird.setPersistenceRequired();
            Bird.setHome(BlockPos.containing(Spot));
            Bird.setArrival(Spot.add(Math.cos(Turn) * LandingRing, 0.0, Math.sin(Turn) * LandingRing));
            if (Start.y > Spot.y + 0.75) {
                Bird.setFlying(true);
            }
            if (World.addFreshEntity(Bird)) {
                Made.add(Bird);
            }
        }
        return Made;
    }
}
