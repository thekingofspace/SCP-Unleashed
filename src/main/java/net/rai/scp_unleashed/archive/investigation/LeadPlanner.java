package net.rai.scp_unleashed.archive.investigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class LeadPlanner {
    private static final int BorderMargin = 32;
    private static final int TargetAttempts = 32;

    private LeadPlanner() {
    }

    public static BlockPos pickTarget(ServerLevel World, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        WorldBorder Border = World.getWorldBorder();
        int Low = Math.max(1, Math.min(MinDistance, MaxDistance));
        int High = Math.max(Low, Math.max(MinDistance, MaxDistance));
        for (int Attempt = 0; Attempt < TargetAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Low + Random.nextDouble() * (High - Low);
            int X = Origin.getX() + (int) Math.round(Math.cos(Angle) * Distance);
            int Z = Origin.getZ() + (int) Math.round(Math.sin(Angle) * Distance);
            if (Border.isWithinBounds(X, Z) && Border.getDistanceToBorder(X, Z) > BorderMargin) {
                return new BlockPos(X, Origin.getY(), Z);
            }
        }
        int X = Mth.floor(Mth.clamp(Origin.getX() + Low, Border.getMinX() + BorderMargin, Border.getMaxX() - BorderMargin));
        int Z = Mth.floor(Mth.clamp(Origin.getZ(), Border.getMinZ() + BorderMargin, Border.getMaxZ() - BorderMargin));
        return new BlockPos(X, Origin.getY(), Z);
    }

    public static int roughly(int Value, int Accuracy, RandomSource Random) {
        int Shifted = Value + (Accuracy > 0 ? Random.nextInt(Accuracy * 2 + 1) - Accuracy : 0);
        return Math.round(Shifted / 5.0f) * 5;
    }

    public static String dimensionOf(Level World) {
        return World.dimension().location().toString();
    }

    public static boolean inDimension(ServerPlayer Reader, String Dimension) {
        return dimensionOf(Reader.level()).equals(Dimension);
    }

    public static double horizontalDistance(ServerPlayer Reader, int X, int Z) {
        double Dx = Reader.getX() - (X + 0.5);
        double Dz = Reader.getZ() - (Z + 0.5);
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static Component dimensionName(String Dimension) {
        ResourceLocation Id = ResourceLocation.tryParse(Dimension);
        if (Id == null) {
            return Component.literal(Dimension);
        }
        String Key = "archive.scp_unleashed.dimension." + Id.getNamespace() + "." + Id.getPath();
        return Component.translatableWithFallback(Key, Id.toString());
    }

    public static BlockPos surface(ServerLevel World, int X, int Z, int FallbackY) {
        if (World.dimensionType().hasCeiling()) {
            BlockPos Centre = new BlockPos(X, FallbackY, Z);
            for (int Dy = 8; Dy >= -24; Dy--) {
                BlockPos Candidate = Centre.above(Dy);
                if (isDryStandable(World, Candidate)) {
                    return Candidate;
                }
            }
            return Centre;
        }
        BlockPos Pos = new BlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z), Z);
        while (Pos.getY() > World.getMinBuildHeight() + 1 && !World.getFluidState(Pos.below()).isEmpty()) {
            Pos = Pos.below();
        }
        return Pos;
    }

    public static Optional<BlockPos> standableNear(ServerLevel World, BlockPos Centre, int MinRadius, int MaxRadius, int Vertical, RandomSource Random) {
        for (int Attempt = 0; Attempt < 24; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = MinRadius + Random.nextDouble() * Math.max(0, MaxRadius - MinRadius);
            BlockPos Column = Centre.offset((int) Math.round(Math.cos(Angle) * Radius), 0, (int) Math.round(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (isDryStandable(World, Candidate)) {
                    return Optional.of(Candidate);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isDryStandable(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.isOutsideBuildHeight(Pos)) {
            return false;
        }
        BlockState At = World.getBlockState(Pos);
        BlockState Above = World.getBlockState(Pos.above());
        BlockState Below = World.getBlockState(Pos.below());
        return At.canBeReplaced() && At.getFluidState().isEmpty() && Above.canBeReplaced() && Above.getFluidState().isEmpty()
                && Below.isFaceSturdy(World, Pos.below(), Direction.UP);
    }

    public static void playTo(ServerPlayer Reader, Holder<SoundEvent> Sound, Vec3 Where, float Volume, float Pitch) {
        Reader.connection.send(new ClientboundSoundPacket(Sound, SoundSource.AMBIENT, Where.x, Where.y, Where.z, Volume, Pitch, Reader.getRandom().nextLong()));
    }

    public static Vec3 towards(ServerPlayer Reader, double X, double Z, double Reach) {
        Vec3 Eye = Reader.getEyePosition();
        double Dx = X - Eye.x;
        double Dz = Z - Eye.z;
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0e-3) {
            return Eye;
        }
        return Eye.add(Dx / Length * Reach, 0.0, Dz / Length * Reach);
    }

    public static String arrowTowards(ServerPlayer Reader, double X, double Z) {
        double Bearing = Math.toDegrees(Math.atan2(-(X - Reader.getX()), Z - Reader.getZ()));
        double Relative = Mth.wrapDegrees(Bearing - Reader.getYRot());
        String[] Arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int Index = Math.floorMod((int) Math.round(Relative / 45.0), 8);
        return Arrows[Index];
    }
}
