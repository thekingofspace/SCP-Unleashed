package net.rai.scp_unleashed.scp143;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Scp143PetalPath {
    public static final int Life = 260;
    public static final double DriftTime = 18.0;
    public static final double WindTime = 12.0;
    public static final double FallTime = 6.0;
    public static final double SwayTime = 8.0;
    public static final double MeanFall = 0.056;

    private Scp143PetalPath() {
    }

    public record Spawn(int Id, double X, double Y, double Z, float DriftX, float DriftZ, float WindX, float WindZ, int Seed, long Born) {
        public static final StreamCodec<ByteBuf, Spawn> SpawnCodec = new StreamCodec<>() {
            @Override
            public Spawn decode(ByteBuf Buffer) {
                return new Spawn(ByteBufCodecs.VAR_INT.decode(Buffer), Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble(), Buffer.readFloat(), Buffer.readFloat(),
                        Buffer.readFloat(), Buffer.readFloat(), Buffer.readInt(), Buffer.readLong());
            }

            @Override
            public void encode(ByteBuf Buffer, Spawn Petal) {
                ByteBufCodecs.VAR_INT.encode(Buffer, Petal.Id());
                Buffer.writeDouble(Petal.X());
                Buffer.writeDouble(Petal.Y());
                Buffer.writeDouble(Petal.Z());
                Buffer.writeFloat(Petal.DriftX());
                Buffer.writeFloat(Petal.DriftZ());
                Buffer.writeFloat(Petal.WindX());
                Buffer.writeFloat(Petal.WindZ());
                Buffer.writeInt(Petal.Seed());
                Buffer.writeLong(Petal.Born());
            }
        };
    }

    public static double unit(int Seed, int Index) {
        long Value = Seed * 0x9E3779B97F4A7C15L + Index * 0xC2B2AE3D27D4EB4FL;
        Value = (Value ^ (Value >>> 33)) * 0xFF51AFD7ED558CCDL;
        Value = (Value ^ (Value >>> 33)) * 0xC4CEB9FE1A85EC53L;
        Value ^= Value >>> 33;
        return (Value >>> 11) * 0x1.0p-53;
    }

    public static double fallSpeed(int Seed) {
        return 0.042 + 0.028 * unit(Seed, 1);
    }

    public static void position(Spawn Petal, double Age, double[] Out) {
        int Seed = Petal.Seed();
        double Fall = fallSpeed(Seed);
        double Sway = 0.18 + 0.28 * unit(Seed, 2);
        double Rate = 0.07 + 0.05 * unit(Seed, 3);
        double Phase = unit(Seed, 4) * Math.PI * 2.0;
        double Heading = unit(Seed, 5) * Math.PI * 2.0;
        double Wobble = 0.35 + 0.25 * unit(Seed, 6);
        double Time = Math.max(0.0, Age);
        double Drift = DriftTime * (1.0 - StrictMath.exp(-Time / DriftTime));
        double Carried = Time - WindTime * (1.0 - StrictMath.exp(-Time / WindTime));
        double Dropped = Fall * (Time - FallTime * (1.0 - StrictMath.exp(-Time / FallTime)));
        double Ramp = 1.0 - StrictMath.exp(-Time / SwayTime);
        double Along = Sway * Ramp * StrictMath.sin(Rate * Time + Phase);
        double Across = Sway * Wobble * Ramp * StrictMath.sin(Rate * 1.9 * Time + Phase * 2.0);
        double Cos = StrictMath.cos(Heading);
        double Sin = StrictMath.sin(Heading);
        Out[0] = Petal.X() + Petal.DriftX() * Drift + Petal.WindX() * Carried + Along * Cos - Across * Sin;
        Out[1] = Petal.Y() - Dropped + 0.035 * Ramp * StrictMath.sin(Rate * 2.0 * Time + Phase);
        Out[2] = Petal.Z() + Petal.DriftZ() * Drift + Petal.WindZ() * Carried + Along * Sin + Across * Cos;
    }

    public static boolean landed(BlockGetter World, double X, double Y, double Z) {
        BlockPos Pos = BlockPos.containing(X, Y, Z);
        BlockState State = World.getBlockState(Pos);
        if (State.isAir() || State.is(BlockTags.LEAVES)) {
            return false;
        }
        if (!State.getFluidState().isEmpty()) {
            return true;
        }
        VoxelShape Shape = State.getCollisionShape(World, Pos);
        if (Shape.isEmpty()) {
            return false;
        }
        double Lx = X - Pos.getX();
        double Ly = Y - Pos.getY();
        double Lz = Z - Pos.getZ();
        for (AABB Box : Shape.toAabbs()) {
            if (Box.contains(Lx, Ly, Lz)) {
                return true;
            }
        }
        return false;
    }

    public static double restHeight(BlockGetter World, double X, double Y, double Z) {
        BlockPos Pos = BlockPos.containing(X, Y, Z);
        BlockState State = World.getBlockState(Pos);
        VoxelShape Shape = State.getCollisionShape(World, Pos);
        if (!State.getFluidState().isEmpty()) {
            return Pos.getY() + State.getFluidState().getHeight(World, Pos) + 0.01;
        }
        return Shape.isEmpty() ? Y : Pos.getY() + Shape.max(Direction.Axis.Y) + 0.01;
    }
}
