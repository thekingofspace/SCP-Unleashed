package net.rai.scp_unleashed.scp553;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp553Cave {
    public static final int StartGroup = 4;
    public static final int SearchRadius = 24;
    public static final int ColumnStep = 3;
    public static final int MinCover = 4;
    public static final int CeilingReach = 7;
    public static final int FloorMargin = 4;
    public static final int GrottoDepth = 12;
    public static final double GrottoWide = 3.5;
    public static final double GrottoTall = 2.2;
    public static final int Flags = Block.UPDATE_CLIENTS;
    public static final int PlaceAttempts = 12;

    private Scp553Cave() {
    }

    public static int surface(ServerLevel World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
    }

    public static boolean airy(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.isAir() && State.getFluidState().isEmpty();
    }

    @Nullable
    public static String problem(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos.above(CeilingReach)) || Pos.getY() < World.getMinBuildHeight() + FloorMargin) {
            return "outside the loaded world";
        }
        if (!airy(World, Pos)) {
            return "not open air";
        }
        if (World.getBrightness(LightLayer.SKY, Pos) > 0) {
            return "open to the sky";
        }
        if (!Scp553Colony.dark(World, Pos)) {
            return "too bright";
        }
        if (Pos.getY() > surface(World, Pos.getX(), Pos.getZ()) - MinCover) {
            return "too close to the surface";
        }
        for (BlockPos Near : BlockPos.betweenClosed(Pos.offset(-1, -1, -1), Pos.offset(1, 1, 1))) {
            if (!airy(World, Near)) {
                return "too cramped";
            }
        }
        if (World.containsAnyLiquid(new AABB(Pos).inflate(2.0))) {
            return "flooded";
        }
        for (int Rise = 2; Rise <= CeilingReach; Rise++) {
            BlockPos Up = Pos.above(Rise);
            BlockState State = World.getBlockState(Up);
            if (State.isAir()) {
                continue;
            }
            return Scp553Colony.isRock(State) ? null : "no rock ceiling";
        }
        return "no ceiling in reach";
    }

    public static boolean spot(ServerLevel World, BlockPos Pos) {
        return problem(World, Pos) == null;
    }

    public static Optional<BlockPos> find(ServerLevel World, BlockPos Centre, int Radius) {
        for (int Ring = 0; Ring <= Radius; Ring += ColumnStep) {
            for (int Dx = -Ring; Dx <= Ring; Dx += ColumnStep) {
                for (int Dz = -Ring; Dz <= Ring; Dz += ColumnStep) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Ring) {
                        continue;
                    }
                    Optional<BlockPos> Found = column(World, Centre.getX() + Dx, Centre.getZ() + Dz);
                    if (Found.isPresent()) {
                        return Found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> column(ServerLevel World, int X, int Z) {
        BlockPos Top = new BlockPos(X, World.getMinBuildHeight(), Z);
        if (!World.isLoaded(Top)) {
            return Optional.empty();
        }
        int High = surface(World, X, Z) - MinCover;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, High, Z);
        for (int Y = High; Y >= World.getMinBuildHeight() + FloorMargin; Y--) {
            Cursor.setY(Y);
            if (!World.getBlockState(Cursor).isAir() || World.getBrightness(LightLayer.SKY, Cursor) > 0) {
                continue;
            }
            if (spot(World, Cursor)) {
                return Optional.of(Cursor.immutable());
            }
        }
        return Optional.empty();
    }

    public static BlockPos grottoFor(ServerLevel World, BlockPos Site) {
        int Ground = surface(World, Site.getX(), Site.getZ());
        int Y = Math.max(World.getMinBuildHeight() + FloorMargin + 3, Ground - GrottoDepth);
        return new BlockPos(Site.getX(), Y, Site.getZ());
    }

    public static void carve(ServerLevel World, BlockPos Centre) {
        int Wide = Mth.ceil(GrottoWide);
        int Tall = Mth.ceil(GrottoTall);
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-Wide, -Tall, -Wide), Centre.offset(Wide, Tall, Wide))) {
            double Dx = (Pos.getX() - Centre.getX()) / GrottoWide;
            double Dy = (Pos.getY() - Centre.getY()) / GrottoTall;
            double Dz = (Pos.getZ() - Centre.getZ()) / GrottoWide;
            if (Dx * Dx + Dy * Dy + Dz * Dz > 1.0) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (State.isAir() || State.getDestroySpeed(World, Pos) < 0.0f || State.hasBlockEntity()) {
                continue;
            }
            World.setBlock(Pos, Blocks.CAVE_AIR.defaultBlockState(), Flags);
        }
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        BlockPos Spot = Found.Pos();
        if (!spot(World, Spot)) {
            Optional<BlockPos> Near = find(World, Spot, SearchRadius / 2);
            if (Near.isPresent()) {
                Spot = Near.get();
            } else {
                carve(World, Spot);
            }
        }
        return !arrive(World, Spot, Found.Investigator()).isEmpty();
    }

    public static List<Scp553Butterfly> arrive(ServerLevel World, BlockPos Spot, @Nullable ServerPlayer Reader) {
        List<Scp553Butterfly> Made = new ArrayList<>();
        RandomSource Random = World.getRandom();
        int Room = Math.max(0, Scp553Colony.WorldCap - Scp553World.get(World.getServer()).count());
        int Wanted = Math.min(StartGroup, Room);
        Vec3 Centre = Vec3.atCenterOf(Spot);
        for (int Index = 0; Index < Wanted; Index++) {
            Scp553Butterfly Butterfly = Scp553Registry.Butterfly.get().create(World);
            if (Butterfly == null) {
                break;
            }
            Vec3 Place = Centre.subtract(0.0, Scp553Butterfly.Height * 0.5, 0.0);
            for (int Attempt = 0; Attempt < PlaceAttempts; Attempt++) {
                Vec3 Candidate = Centre.add((Random.nextDouble() - 0.5) * 2.0, (Random.nextDouble() - 0.5) * 1.2 - Scp553Butterfly.Height * 0.5, (Random.nextDouble() - 0.5) * 2.0);
                if (World.noCollision(Butterfly, Butterfly.getDimensions(Butterfly.getPose()).makeBoundingBox(Candidate))) {
                    Place = Candidate;
                    break;
                }
            }
            float Yaw = Random.nextFloat() * 360.0f;
            Butterfly.moveTo(Place.x, Place.y, Place.z, Yaw, 0.0f);
            Butterfly.setYBodyRot(Yaw);
            Butterfly.setYHeadRot(Yaw);
            Butterfly.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot), MobSpawnType.EVENT, null);
            Butterfly.arrived(Spot, Random);
            if (World.addFreshEntity(Butterfly)) {
                Made.add(Butterfly);
            }
        }
        if (!Made.isEmpty()) {
            World.sendParticles(Scp553Registry.Glint.get(), Centre.x, Centre.y, Centre.z, 18, 0.8, 0.5, 0.8, 0.02);
            Scp553Colony.play(World, Centre, Scp553Registry.CallLayers, 1.0f, 0.8f);
        }
        return Made;
    }
}
