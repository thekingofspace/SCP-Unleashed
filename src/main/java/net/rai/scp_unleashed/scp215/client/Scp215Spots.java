package net.rai.scp_unleashed.scp215.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp215.Scp215Ground;
import net.rai.scp_unleashed.scp215.Scp215Variant;
import org.jetbrains.annotations.Nullable;

public final class Scp215Spots {
    public static final int Attempts = 28;
    public static final double NearDistance = 12.0;
    public static final double FarDistance = 30.0;
    public static final int ScanUp = 7;
    public static final int ScanDown = 10;
    public static final double PassingScore = 2.5;
    public static final double DarkWeight = 0.38;
    public static final double CornerBonus = 2.6;
    public static final double WallBonus = 1.0;
    public static final double TreeBonus = 2.6;
    public static final double PeekBonus = 1.6;
    public static final double EdgeBonus = 1.5;

    private Scp215Spots() {
    }

    public record Spot(Vec3 Feet, double Score) {
    }

    @Nullable
    public static Spot find(ClientLevel World, LocalPlayer User, Scp215Variant Kind, RandomSource Random) {
        Vec3 Eye = User.getEyePosition();
        Spot Best = null;
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Roll = Random.nextDouble();
            float Offset;
            if (Roll < 0.6) {
                Offset = (35.0f + Random.nextFloat() * 45.0f) * (Random.nextBoolean() ? 1.0f : -1.0f);
            } else if (Roll < 0.85) {
                Offset = (100.0f + Random.nextFloat() * 80.0f) * (Random.nextBoolean() ? 1.0f : -1.0f);
            } else {
                Offset = (Random.nextFloat() * 2.0f - 1.0f) * 25.0f;
            }
            double Distance = NearDistance + Random.nextDouble() * (FarDistance - NearDistance);
            float Yaw = (User.getYRot() + Offset) * Mth.DEG_TO_RAD;
            double X = User.getX() - Mth.sin(Yaw) * Distance;
            double Z = User.getZ() + Mth.cos(Yaw) * Distance;
            double Y = Scp215Ground.ground(World, X, User.getY(), Z, Kind.clearance(), ScanUp, ScanDown);
            if (Double.isNaN(Y)) {
                continue;
            }
            Vec3 Feet = new Vec3(Mth.floor(X) + 0.5, Y, Mth.floor(Z) + 0.5);
            Vec3 Head = Feet.add(0.0, Kind.Focus, 0.0);
            if (Head.distanceTo(Eye) < NearDistance - 1.0 || !Scp215Ground.sees(World, Eye, Head, User)) {
                continue;
            }
            double Score = score(World, User, Eye, Feet, Kind, Math.abs(Offset), Random);
            if (Best == null || Score > Best.Score()) {
                Best = new Spot(Feet, Score);
            }
        }
        return Best != null && Best.Score() >= PassingScore ? Best : null;
    }

    private static double score(ClientLevel World, LocalPlayer User, Vec3 Eye, Vec3 Feet, Scp215Variant Kind, float Offset, RandomSource Random) {
        BlockPos Base = BlockPos.containing(Feet);
        int Light = World.getMaxLocalRawBrightness(Base);
        double Score = (15 - Light) * DarkWeight * (Kind == Scp215Variant.Watcher ? 1.4 : 1.0);
        int Walls = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (solid(World, Base.relative(Side)) || solid(World, Base.relative(Side).above())) {
                Walls++;
            }
        }
        if (Walls >= 2) {
            Score += CornerBonus;
        } else if (Walls == 1) {
            Score += WallBonus;
        }
        if (nearTree(World, Base)) {
            Score += TreeBonus;
        }
        if (!Scp215Ground.sees(World, Eye, Feet.add(0.0, 0.3, 0.0), User)) {
            Score += PeekBonus;
        }
        if (Offset >= 30.0f && Offset <= 85.0f) {
            Score += EdgeBonus;
        } else if (Offset < 30.0f && Light > 7) {
            Score -= 2.0;
        }
        if (Kind == Scp215Variant.Crawler && solid(World, Base.above(1))) {
            Score += 1.0;
        }
        return Score + Random.nextDouble() * 1.5;
    }

    private static boolean solid(ClientLevel World, BlockPos At) {
        BlockState State = World.getBlockState(At);
        return !State.getCollisionShape(World, At).isEmpty() || State.is(BlockTags.LEAVES);
    }

    private static boolean nearTree(ClientLevel World, BlockPos Base) {
        for (BlockPos At : BlockPos.betweenClosed(Base.offset(-2, 0, -2), Base.offset(2, 2, 2))) {
            if (World.getBlockState(At).is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }
}
