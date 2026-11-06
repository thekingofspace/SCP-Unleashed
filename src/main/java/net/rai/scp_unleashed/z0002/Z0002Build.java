package net.rai.scp_unleashed.z0002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class Z0002Build {
    public static final int PlaceFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_MOVE_BY_PISTON;
    public static final int NotifyFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final double IsletCore = 4.6;
    public static final double IsletEdge = 6.8;
    public static final int BodyHalf = 2;
    public static final int BodyHeight = 4;
    public static final int LanternLight = 13;
    public static final int RingLight = 8;
    public static final double RingLightRadius = 10.0;
    public static final int RingLights = 8;
    public static final int SkullCount = 6;
    public static final int BoneCount = 4;
    public static final double LeashReach = 16.0;
    public static final List<BlockState> Rocks = List.of(Blocks.BLACKSTONE.defaultBlockState(), Blocks.BASALT.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
            Blocks.BLACKSTONE.defaultBlockState(), Blocks.TUFF.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState(), Blocks.POLISHED_BLACKSTONE.defaultBlockState());

    private Z0002Build() {
    }

    public static boolean untouchable(ServerLevel World, BlockPos Pos, BlockState State, Z0002Region Region) {
        if (State.getDestroySpeed(World, Pos) < 0.0f || State.is(BlockTags.WITHER_IMMUNE)) {
            return true;
        }
        if (State.getBlock() instanceof PistonHeadBlock && State.hasProperty(PistonHeadBlock.FACING)) {
            return !Region.contains(Pos.relative(State.getValue(PistonHeadBlock.FACING).getOpposite()));
        }
        return false;
    }

    public static Z0002Snapshot capture(ServerLevel World, Z0002Region Region) {
        Z0002Snapshot Snapshot = new Z0002Snapshot();
        HolderLookup.Provider Lookup = World.registryAccess();
        BlockState Air = Blocks.AIR.defaultBlockState();
        Region.forEach(Pos -> {
            BlockState State = World.getBlockState(Pos);
            int Index = Region.index(Pos);
            if (untouchable(World, Pos, State, Region)) {
                Snapshot.skip(Index);
                return;
            }
            if (State == Air) {
                return;
            }
            Snapshot.record(Index, State);
            if (State.hasBlockEntity()) {
                BlockEntity Found = World.getBlockEntity(Pos);
                if (Found != null) {
                    Snapshot.recordBlockEntity(Index, Found.saveWithFullMetadata(Lookup));
                }
            }
        });
        Snapshot.seal();
        return Snapshot;
    }

    public static boolean attached(Entity Target) {
        if (Target instanceof Player || Target instanceof ItemEntity || Target instanceof ExperienceOrb || Target instanceof Projectile || Target instanceof PrimedTnt) {
            return false;
        }
        if (Target instanceof Z0002Shrine || Target instanceof Z0002Slash) {
            return false;
        }
        if (Target instanceof ArmorStand) {
            return true;
        }
        return !(Target instanceof LivingEntity);
    }

    public static boolean within(Z0002Region Region, Entity Target) {
        return Region.contains(BlockPos.containing(Target.getBoundingBox().getCenter())) || Region.contains(Target.blockPosition());
    }

    public static int captureEntities(ServerLevel World, Z0002Domain Domain) {
        Z0002Region Region = Domain.Region;
        List<Entity> Found = World.getEntities((Entity) null, Region.box().inflate(2.0), Target -> attached(Target) && within(Region, Target));
        Set<Entity> Taking = new HashSet<>(Found);
        for (Entity Target : Found) {
            if (Target instanceof LeashFenceKnotEntity) {
                for (Mob Held : World.getEntitiesOfClass(Mob.class, Target.getBoundingBox().inflate(LeashReach), Each -> Each.getLeashHolder() == Target)) {
                    Taking.add(Held);
                }
            }
        }
        int Count = 0;
        for (Entity Target : Taking) {
            if (Target.isRemoved()) {
                continue;
            }
            if (Target.isPassenger() && !Taking.contains(Target.getVehicle())) {
                Target.stopRiding();
            }
            if (Target.isPassenger()) {
                continue;
            }
            List<Entity> Aboard = new ArrayList<>();
            for (Entity Rider : Target.getIndirectPassengers()) {
                Aboard.add(Rider);
            }
            for (Entity Rider : Aboard) {
                if (Rider instanceof Player) {
                    Rider.stopRiding();
                }
            }
            CompoundTag Data = new CompoundTag();
            if (Target.saveAsPassenger(Data)) {
                Domain.Snapshot.capture(Data);
                for (int Index = Aboard.size() - 1; Index >= 0; Index--) {
                    if (!(Aboard.get(Index) instanceof Player)) {
                        Aboard.get(Index).remove(Entity.RemovalReason.CHANGED_DIMENSION);
                    }
                }
                Target.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                Count++;
            }
        }
        return Count;
    }

    public static int restoreEntities(ServerLevel World, Z0002Snapshot Snapshot) {
        int Count = 0;
        for (CompoundTag Data : Snapshot.captured()) {
            if (Data.hasUUID("UUID") && World.getEntity(Data.getUUID("UUID")) != null) {
                continue;
            }
            Entity Made = EntityType.loadEntityRecursive(Data.copy(), World, Each -> Each);
            if (Made != null && World.tryAddFreshEntityWithPassengers(Made)) {
                Count++;
            }
        }
        return Count;
    }

    public static Map<Long, BlockState> design(Z0002Domain Domain) {
        Map<Long, BlockState> Design = new HashMap<>();
        Z0002Region Region = Domain.Region;
        BlockPos Centre = Region.Centre();
        RandomSource Random = RandomSource.create(Domain.Id.getMostSignificantBits() ^ Domain.Id.getLeastSignificantBits());
        int Floor = Region.floorY();
        double FaceX = -Mth.sin(Domain.Facing * Mth.DEG_TO_RAD);
        double FaceZ = Mth.cos(Domain.Facing * Mth.DEG_TO_RAD);
        int Reach = (int) Math.ceil(IsletEdge);
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz) + (Random.nextDouble() - 0.5) * 0.9;
                BlockPos Ground = Centre.offset(Dx, 1, Dz);
                double Ahead = (Dx * FaceX + Dz * FaceZ) / Math.max(0.001, Math.sqrt(Dx * Dx + Dz * Dz));
                if (Distance <= IsletCore) {
                    Design.put(Ground.asLong(), rock(Random));
                } else if (Distance <= IsletEdge && Random.nextFloat() < (Ahead > 0.55 ? 0.25f : 0.6f)) {
                    Design.put(Ground.asLong(), rock(Random));
                    if (Ahead < 0.35 && Random.nextFloat() < 0.4f) {
                        Design.put(Ground.above().asLong(), rock(Random));
                    }
                }
            }
        }
        for (int Dx = -BodyHalf; Dx <= BodyHalf; Dx++) {
            for (int Dz = -BodyHalf; Dz <= BodyHalf; Dz++) {
                for (int Dy = 2; Dy < 2 + BodyHeight; Dy++) {
                    Design.put(Centre.offset(Dx, Dy, Dz).asLong(), Blocks.BARRIER.defaultBlockState());
                }
            }
        }
        int SideX = (int) Math.round(FaceZ);
        int SideZ = (int) Math.round(-FaceX);
        int FrontX = (int) Math.round(FaceX);
        int FrontZ = (int) Math.round(FaceZ);
        for (int Side = -1; Side <= 1; Side += 2) {
            BlockPos Lantern = Centre.offset(FrontX * (BodyHalf + 1) + SideX * Side * (BodyHalf + 1), 2, FrontZ * (BodyHalf + 1) + SideZ * Side * (BodyHalf + 1));
            Design.put(Lantern.asLong(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LanternLight));
        }
        for (int Index = 0; Index < RingLights; Index++) {
            double Angle = Math.PI * 2.0 * Index / RingLights;
            BlockPos Spot = BlockPos.containing(Centre.getX() + 0.5 + Math.cos(Angle) * RingLightRadius, Floor + 5, Centre.getZ() + 0.5 + Math.sin(Angle) * RingLightRadius);
            if (Region.contains(Spot) && !Region.isShell(Spot)) {
                Design.put(Spot.asLong(), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, RingLight));
            }
        }
        for (int Index = 0; Index < SkullCount + BoneCount; Index++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = IsletEdge + 0.5 + Random.nextDouble() * 2.5;
            BlockPos Spot = BlockPos.containing(Centre.getX() + 0.5 + Math.cos(Angle) * Distance, Floor + 1, Centre.getZ() + 0.5 + Math.sin(Angle) * Distance);
            if (Design.containsKey(Spot.asLong())) {
                continue;
            }
            if (Index < SkullCount) {
                Design.put(Spot.asLong(), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, Random.nextInt(16)));
            } else {
                Direction.Axis Axis = Random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
                Design.put(Spot.asLong(), Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis));
            }
        }
        return Design;
    }

    private static BlockState rock(RandomSource Random) {
        BlockState Picked = Rocks.get(Random.nextInt(Rocks.size()));
        if (Picked.hasProperty(RotatedPillarBlock.AXIS)) {
            return Picked.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        return Picked;
    }

    public static BlockState target(Z0002Region Region, BlockPos Pos, Map<Long, BlockState> Design) {
        if (Region.isShell(Pos)) {
            return Z0002Registry.Barrier.get().defaultBlockState();
        }
        if (Region.isFloor(Pos)) {
            return Z0002Registry.Floor.get().defaultBlockState();
        }
        BlockState Feature = Design.get(Pos.asLong());
        return Feature != null ? Feature : Blocks.AIR.defaultBlockState();
    }

    public static void withCapture(ServerLevel World, Runnable Work) {
        boolean Previous = World.captureBlockSnapshots;
        World.captureBlockSnapshots = true;
        try {
            Work.run();
        } finally {
            World.captureBlockSnapshots = Previous;
        }
    }

    public static void placeIndex(ServerLevel World, Z0002Domain Domain, Map<Long, BlockState> Design, int Index) {
        if (Domain.Snapshot.isSkipped(Index)) {
            return;
        }
        BlockPos Pos = Domain.Region.position(Index);
        BlockState Wanted = target(Domain.Region, Pos, Design);
        BlockState Current = World.getBlockState(Pos);
        if (Current == Wanted) {
            return;
        }
        if (Current.hasBlockEntity()) {
            World.removeBlockEntity(Pos);
        }
        place(World, Pos, Wanted);
    }

    public static void restoreIndex(ServerLevel World, Z0002Domain Domain, int Index) {
        Z0002Snapshot Snapshot = Domain.Snapshot;
        if (Snapshot.isSkipped(Index)) {
            return;
        }
        BlockPos Pos = Domain.Region.position(Index);
        BlockState Wanted = Snapshot.stateAt(Index);
        BlockState Current = World.getBlockState(Pos);
        if (Current != Wanted) {
            if (Current.hasBlockEntity()) {
                World.removeBlockEntity(Pos);
            }
            place(World, Pos, Wanted);
        }
        CompoundTag Data = Snapshot.blockEntityAt(Index);
        BlockState State = World.getBlockState(Pos);
        if (Data != null && State.hasBlockEntity()) {
            HolderLookup.Provider Lookup = World.registryAccess();
            BlockEntity Found = World.getBlockEntity(Pos);
            if (Found == null) {
                Found = BlockEntity.loadStatic(Pos, State, Data, Lookup);
                if (Found != null) {
                    World.setBlockEntity(Found);
                }
            } else {
                Found.loadWithComponents(Data, Lookup);
            }
            if (Found != null) {
                Found.setChanged();
                World.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
            }
        }
        FluidState Fluid = World.getFluidState(Pos);
        if (!Fluid.isEmpty() && !Fluid.isSource()) {
            World.scheduleTick(Pos, Fluid.getType(), Fluid.getType().getTickDelay(World));
        }
    }

    public static void restore(ServerLevel World, Z0002Domain Domain, @Nullable Predicate<ChunkPos> Only) {
        Z0002Region Region = Domain.Region;
        withCapture(World, () -> Region.forEach(Pos -> {
            if (Only == null || Only.test(new ChunkPos(Pos))) {
                restoreIndex(World, Domain, Region.index(Pos));
            }
        }));
    }

    public static boolean showsDomain(ServerLevel World, Z0002Region Region, ChunkPos Chunk, List<Z0002Region> Claimed) {
        int Found = 0;
        int Checked = 0;
        for (int X = Chunk.getMinBlockX(); X <= Chunk.getMaxBlockX(); X++) {
            for (int Z = Chunk.getMinBlockZ(); Z <= Chunk.getMaxBlockZ(); Z++) {
                BlockPos Floor = new BlockPos(X, Region.floorY(), Z);
                if (!Region.contains(Floor) || claimed(Claimed, Floor)) {
                    continue;
                }
                Checked++;
                BlockState State = World.getBlockState(Floor);
                if (Z0002FloorBlock.is(State) || Z0002BarrierBlock.is(State)) {
                    Found++;
                }
            }
        }
        return Checked > 0 && Found * 2 >= Checked;
    }

    private static boolean claimed(List<Z0002Region> Claimed, BlockPos Pos) {
        for (Z0002Region Other : Claimed) {
            if (Other.contains(Pos)) {
                return true;
            }
        }
        return false;
    }

    private static void place(ServerLevel World, BlockPos Pos, BlockState Wanted) {
        BlockState Old = World.getBlockState(Pos);
        if (Old == Wanted) {
            return;
        }
        int Before = World.capturedBlockSnapshots.size();
        boolean Changed = World.setBlock(Pos, Wanted, PlaceFlags);
        while (World.capturedBlockSnapshots.size() > Before) {
            World.capturedBlockSnapshots.remove(World.capturedBlockSnapshots.size() - 1);
        }
        if (Changed) {
            World.markAndNotifyBlock(Pos, World.getChunkAt(Pos), Old, Wanted, NotifyFlags, Block.UPDATE_LIMIT);
        }
    }

    public static Vec3 shrineSpot(Z0002Domain Domain) {
        BlockPos Centre = Domain.Region.Centre();
        return new Vec3(Centre.getX() + 0.5, Centre.getY() + 2.0, Centre.getZ() + 0.5);
    }
}
