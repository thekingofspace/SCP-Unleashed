package net.rai.scp_unleashed.scp244;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Scp244Flood {
    public static final int RadiusSquared = 256;
    public static final int MaxCells = 9000;
    public static final float Diagonal = 1.4142f;
    public static final int SolidBit = 1 << 6;
    public static final int ProbedBit = 1 << 7;
    private static final double[] Samples = {0.125, 0.375, 0.625, 0.875};
    private static final double Slack = 1.0e-4;
    private static final int[][] Faces = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
    private static final int[][] Slants = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final Level World;
    private final BlockPos Origin;
    private final long OriginKey;
    private final Long2ByteOpenHashMap Kinds = new Long2ByteOpenHashMap();
    private final Long2FloatOpenHashMap Best = new Long2FloatOpenHashMap();
    private final LongOpenHashSet Done = new LongOpenHashSet();
    private final LongArrayList Keys = new LongArrayList();
    private final FloatArrayList Distances = new FloatArrayList();
    private final Map<VoxelShape, Integer> Seals = new IdentityHashMap<>();
    private final BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
    private long[] HeapKeys = new long[256];
    private float[] HeapCosts = new float[256];
    private int HeapSize;
    private boolean Finished;

    public Scp244Flood(Level World, BlockPos Origin) {
        this.World = World;
        this.Origin = Origin.immutable();
        this.OriginKey = Origin.asLong();
        this.Best.defaultReturnValue(Float.MAX_VALUE);
        this.Best.put(OriginKey, 0.0f);
        push(OriginKey, 0.0f);
    }

    public static Scp244Field fill(Level World, BlockPos Origin) {
        Scp244Flood Flood = new Scp244Flood(World, Origin);
        while (!Flood.step(Integer.MAX_VALUE)) {
        }
        return Flood.result();
    }

    public boolean finished() {
        return Finished;
    }

    public Scp244Field result() {
        return new Scp244Field(Origin, Keys.toLongArray(), Distances.toFloatArray());
    }

    public boolean step(int Budget) {
        int Used = 0;
        while (!Finished && Used < Budget) {
            if (HeapSize == 0 || Keys.size() >= MaxCells) {
                Finished = true;
                break;
            }
            long Key = HeapKeys[0];
            float Cost = HeapCosts[0];
            pop();
            if (Done.contains(Key) || Cost > Best.get(Key)) {
                continue;
            }
            Done.add(Key);
            Keys.add(Key);
            Distances.add(Cost);
            Used++;
            spread(Key, Cost);
        }
        return Finished;
    }

    private void spread(long Key, float Cost) {
        int X = BlockPos.getX(Key);
        int Y = BlockPos.getY(Key);
        int Z = BlockPos.getZ(Key);
        int Here = kind(X, Y, Z);
        for (int[] Step : Faces) {
            int Nx = X + Step[0];
            int Ny = Y + Step[1];
            int Nz = Z + Step[2];
            if (!inside(Nx, Ny, Nz)) {
                continue;
            }
            int There = kind(Nx, Ny, Nz);
            if ((There & SolidBit) != 0 || !passes(Here, There, Step[0], Step[1], Step[2])) {
                continue;
            }
            relax(Nx, Ny, Nz, Cost + 1.0f);
        }
        for (int[] Slant : Slants) {
            int Dx = Slant[0];
            int Dz = Slant[1];
            int Nx = X + Dx;
            int Nz = Z + Dz;
            if (!inside(Nx, Y, Nz)) {
                continue;
            }
            int Side = kind(Nx, Y, Z);
            int Front = kind(X, Y, Nz);
            int There = kind(Nx, Y, Nz);
            if (((Side | Front | There) & SolidBit) != 0) {
                continue;
            }
            if (passes(Here, Side, Dx, 0, 0) && passes(Side, There, 0, 0, Dz) && passes(Here, Front, 0, 0, Dz) && passes(Front, There, Dx, 0, 0)) {
                relax(Nx, Y, Nz, Cost + Diagonal);
            }
        }
    }

    private boolean inside(int X, int Y, int Z) {
        int Dx = X - Origin.getX();
        int Dy = Y - Origin.getY();
        int Dz = Z - Origin.getZ();
        return Dx * Dx + Dy * Dy + Dz * Dz <= RadiusSquared;
    }

    private static boolean passes(int From, int To, int Dx, int Dy, int Dz) {
        Direction Toward = Direction.fromDelta(Dx, Dy, Dz);
        if (Toward == null) {
            return false;
        }
        return (From & (1 << Toward.get3DDataValue())) == 0 && (To & (1 << Toward.getOpposite().get3DDataValue())) == 0;
    }

    private void relax(int X, int Y, int Z, float Cost) {
        long Key = BlockPos.asLong(X, Y, Z);
        if (Done.contains(Key) || Cost >= Best.get(Key)) {
            return;
        }
        Best.put(Key, Cost);
        push(Key, Cost);
    }

    public int kind(int X, int Y, int Z) {
        long Key = BlockPos.asLong(X, Y, Z);
        if (Key == OriginKey) {
            return 0;
        }
        byte Known = Kinds.get(Key);
        if (Known != 0) {
            return Known & ~ProbedBit & 0xFF;
        }
        Cursor.set(X, Y, Z);
        int Kind;
        if (World.isOutsideBuildHeight(Cursor) || !World.isLoaded(Cursor)) {
            Kind = SolidBit;
        } else {
            BlockState State = World.getBlockState(Cursor);
            if (State.isAir()) {
                Kind = 0;
            } else if (!State.getFluidState().isEmpty()) {
                Kind = SolidBit;
            } else {
                VoxelShape Shape = State.getCollisionShape(World, Cursor);
                Kind = Shape.isEmpty() ? 0 : Shape == Shapes.block() ? SolidBit : Seals.computeIfAbsent(Shape, Scp244Flood::seals);
            }
        }
        Kinds.put(Key, (byte) (Kind | ProbedBit));
        return Kind;
    }

    public static int seals(VoxelShape Shape) {
        List<AABB> Boxes = Shape.toAabbs();
        int Result = 0;
        for (Direction Side : Direction.values()) {
            if (halfSealed(Boxes, Side)) {
                Result |= 1 << Side.get3DDataValue();
            }
        }
        boolean AcrossX = (Result & bit(Direction.EAST)) != 0 && (Result & bit(Direction.WEST)) != 0;
        boolean AcrossZ = (Result & bit(Direction.NORTH)) != 0 && (Result & bit(Direction.SOUTH)) != 0;
        boolean AcrossY = (Result & bit(Direction.UP)) != 0 && (Result & bit(Direction.DOWN)) != 0;
        return AcrossX || AcrossZ || AcrossY ? SolidBit : Result;
    }

    private static int bit(Direction Side) {
        return 1 << Side.get3DDataValue();
    }

    private static boolean halfSealed(List<AABB> Boxes, Direction Side) {
        Direction.Axis Axis = Side.getAxis();
        boolean Positive = Side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double Low = Positive ? 0.5 : 0.0;
        double High = Positive ? 1.0 : 0.5;
        for (double First : Samples) {
            for (double Second : Samples) {
                boolean Covered = false;
                for (AABB Box : Boxes) {
                    double Min = Box.min(Axis);
                    double Max = Box.max(Axis);
                    if (Min >= High - Slack || Max <= Low + Slack) {
                        continue;
                    }
                    if (covers(Box, Axis, First, Second)) {
                        Covered = true;
                        break;
                    }
                }
                if (!Covered) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean covers(AABB Box, Direction.Axis Axis, double First, double Second) {
        return switch (Axis) {
            case X -> Box.minY <= First && Box.maxY >= First && Box.minZ <= Second && Box.maxZ >= Second;
            case Y -> Box.minX <= First && Box.maxX >= First && Box.minZ <= Second && Box.maxZ >= Second;
            case Z -> Box.minX <= First && Box.maxX >= First && Box.minY <= Second && Box.maxY >= Second;
        };
    }

    private void push(long Key, float Cost) {
        if (HeapSize == HeapKeys.length) {
            HeapKeys = Arrays.copyOf(HeapKeys, HeapSize * 2);
            HeapCosts = Arrays.copyOf(HeapCosts, HeapSize * 2);
        }
        int At = HeapSize++;
        while (At > 0) {
            int Parent = (At - 1) >> 1;
            if (HeapCosts[Parent] <= Cost) {
                break;
            }
            HeapKeys[At] = HeapKeys[Parent];
            HeapCosts[At] = HeapCosts[Parent];
            At = Parent;
        }
        HeapKeys[At] = Key;
        HeapCosts[At] = Cost;
    }

    private void pop() {
        HeapSize--;
        if (HeapSize == 0) {
            return;
        }
        long Key = HeapKeys[HeapSize];
        float Cost = HeapCosts[HeapSize];
        int At = 0;
        while (true) {
            int Child = At * 2 + 1;
            if (Child >= HeapSize) {
                break;
            }
            if (Child + 1 < HeapSize && HeapCosts[Child + 1] < HeapCosts[Child]) {
                Child++;
            }
            if (HeapCosts[Child] >= Cost) {
                break;
            }
            HeapKeys[At] = HeapKeys[Child];
            HeapCosts[At] = HeapCosts[Child];
            At = Child;
        }
        HeapKeys[At] = Key;
        HeapCosts[At] = Cost;
    }
}
