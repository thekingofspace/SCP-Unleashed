package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CourtArena {
    public static final int Pillars = 7;
    public static final int PillarRadius = 34;
    public static final int PillarHeight = 24;
    public static final int FloorRadius = 40;
    public static final int RimRadius = 38;
    public static final int PlayerRange = 48;
    private static int RadiusOverride = -1;

    private CourtArena() {
    }

    public static void overrideForTests(int Radius) {
        RadiusOverride = Radius;
    }

    public static boolean overridden() {
        return RadiusOverride > 0;
    }

    public static int pillarRadius() {
        return RadiusOverride > 0 ? RadiusOverride : PillarRadius;
    }

    public static int pillarHeight() {
        return RadiusOverride > 0 ? 4 : PillarHeight;
    }

    public static int floorRadius() {
        return RadiusOverride > 0 ? RadiusOverride + 1 : FloorRadius;
    }

    public static int rimRadius() {
        return RadiusOverride > 0 ? RadiusOverride : RimRadius;
    }

    public static int playerRange() {
        return RadiusOverride > 0 ? RadiusOverride + 16 : PlayerRange;
    }

    public static double angle(int Index) {
        return Index * Math.PI * 2.0 / Pillars;
    }

    public static BlockPos pillarBase(BlockPos Dais, int Index) {
        double Angle = angle(Index);
        int Radius = pillarRadius();
        return Dais.offset((int) Math.round(Math.sin(Angle) * Radius), 0, (int) Math.round(-Math.cos(Angle) * Radius));
    }

    public static BlockPos pillarTop(BlockPos Dais, int Index) {
        return pillarBase(Dais, Index).above(pillarHeight());
    }

    public static BlockPos inward(BlockPos Dais, int Index, int Steps) {
        double Angle = angle(Index);
        int Radius = Math.max(2, pillarRadius() - Steps);
        return Dais.offset((int) Math.round(Math.sin(Angle) * Radius), 0, (int) Math.round(-Math.cos(Angle) * Radius));
    }

    public static BlockPos onCircle(BlockPos Dais, double Angle, double Radius) {
        return Dais.offset((int) Math.round(Math.sin(Angle) * Radius), 0, (int) Math.round(-Math.cos(Angle) * Radius));
    }

    public static AABB area(BlockPos Dais) {
        int Radius = floorRadius() + 8;
        return new AABB(Dais).inflate(Radius, Math.max(16, pillarHeight() + 12), Radius);
    }

    public static boolean inside(BlockPos Dais, Vec3 Point) {
        double Dx = Point.x - (Dais.getX() + 0.5);
        double Dz = Point.z - (Dais.getZ() + 0.5);
        int Radius = floorRadius();
        return Dx * Dx + Dz * Dz <= Radius * Radius;
    }

    public static boolean standable(ServerLevel World, BlockPos Pos) {
        BlockState Below = World.getBlockState(Pos.below());
        if (Below.isAir() || !Below.isFaceSturdy(World, Pos.below(), net.minecraft.core.Direction.UP)) {
            return false;
        }
        return World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty() && World.getBlockState(Pos.above()).getCollisionShape(World, Pos.above()).isEmpty()
                && World.getFluidState(Pos).isEmpty();
    }

    @Nullable
    public static BlockPos ground(ServerLevel World, BlockPos Near, int Reach) {
        int Lowest = Math.max(World.getMinBuildHeight() + 1, Near.getY() - Reach);
        int Highest = Math.min(World.getMaxBuildHeight() - 2, Near.getY() + Reach);
        for (int Offset = 0; Offset <= Reach; Offset++) {
            int Down = Near.getY() - Offset;
            if (Down >= Lowest) {
                BlockPos Pos = new BlockPos(Near.getX(), Down, Near.getZ());
                if (standable(World, Pos)) {
                    return Pos;
                }
            }
            int Up = Near.getY() + Offset;
            if (Offset > 0 && Up <= Highest) {
                BlockPos Pos = new BlockPos(Near.getX(), Up, Near.getZ());
                if (standable(World, Pos)) {
                    return Pos;
                }
            }
        }
        return null;
    }

    public static BlockPos groundOr(ServerLevel World, BlockPos Near, BlockPos Fallback) {
        BlockPos Found = ground(World, Near, 12);
        return Found != null ? Found : Fallback;
    }

    public static BlockPos scatter(ServerLevel World, BlockPos Dais, double MinRadius, double MaxRadius, net.minecraft.util.RandomSource Random) {
        for (int Attempt = 0; Attempt < 12; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Mth.lerp(Random.nextDouble(), MinRadius, MaxRadius);
            BlockPos Found = ground(World, onCircle(Dais, Angle, Radius), 12);
            if (Found != null) {
                return Found;
            }
        }
        return groundOr(World, Dais.offset(2, 0, 2), Dais);
    }
}
