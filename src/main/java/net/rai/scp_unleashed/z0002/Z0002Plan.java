package net.rai.scp_unleashed.z0002;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Z0002Plan {
    public static final float FloorWave = 0.8f;
    public static final float ShellStart = 0.12f;
    public static final float ShellRise = 0.86f;
    public static final float IsletStart = 0.25f;
    public static final float IsletSpread = 0.5f;
    public static final float ShellFall = 0.55f;
    public static final float WaveStart = 0.35f;
    public static final float WaveSpan = 0.6f;
    public static final double PadReach = 1.0;
    public final int[] Build;
    public final float[] BuildAt;
    public final int[] Restore;
    public final float[] RestoreAt;
    public final Map<Long, BlockState> Design;
    public int Built;
    public int Restored;

    private Z0002Plan(int[] Build, float[] BuildAt, int[] Restore, float[] RestoreAt, Map<Long, BlockState> Design) {
        this.Build = Build;
        this.BuildAt = BuildAt;
        this.Restore = Restore;
        this.RestoreAt = RestoreAt;
        this.Design = Design;
    }

    public int size() {
        return Build.length;
    }

    public boolean built() {
        return Built >= Build.length;
    }

    public boolean restored() {
        return Restored >= Restore.length;
    }

    public static Z0002Plan of(Z0002Domain Domain) {
        Z0002Region Region = Domain.Region;
        Z0002Snapshot Snapshot = Domain.Snapshot;
        Map<Long, BlockState> Design = Z0002Build.design(Domain);
        Set<Integer> Pads = pads(Domain);
        Set<Integer> Holders = new HashSet<>(Snapshot.blockEntities().keySet());
        List<Integer> Indices = new ArrayList<>();
        List<Float> BuildTimes = new ArrayList<>();
        List<Float> RestoreTimes = new ArrayList<>();
        float Radius = Region.Radius();
        Region.forEach(Pos -> {
            int Index = Region.index(Pos);
            if (Snapshot.isSkipped(Index)) {
                return;
            }
            int Dy = Pos.getY() - Region.floorY();
            double Across = Math.sqrt(Math.pow(Pos.getX() - Region.Centre().getX(), 2) + Math.pow(Pos.getZ() - Region.Centre().getZ(), 2)) / Radius;
            double Reach = Region.reach(Vec3.atCenterOf(Pos)) / Radius;
            float Jitter = (((Index * 73856093) ^ 0x5bd1e995) & 1023) / 1023.0f * 0.02f;
            float BuildTime;
            float RestoreTime;
            if (Pads.contains(Index)) {
                BuildTime = 0.0f;
                RestoreTime = 1.0f;
            } else if (Region.isShell(Pos)) {
                BuildTime = ShellStart + ShellRise * Dy / Radius + Jitter;
                RestoreTime = ShellFall * (1.0f - (float) Dy / Radius) + Jitter;
            } else if (Region.isFloor(Pos)) {
                BuildTime = (float) (FloorWave * Across);
                RestoreTime = (float) (WaveStart + WaveSpan * (1.0 - Across)) + Jitter;
            } else if (Design.containsKey(Pos.asLong())) {
                BuildTime = (float) (IsletStart + IsletSpread * Across);
                RestoreTime = (float) (WaveStart + WaveSpan * (1.0 - Across)) + Jitter;
            } else {
                BuildTime = (float) (FloorWave * Math.min(1.0, Reach));
                RestoreTime = (float) (WaveStart + WaveSpan * (1.0 - Math.min(1.0, Reach))) + Jitter;
            }
            if (Holders.contains(Index)) {
                BuildTime = 0.0f;
            }
            if (Snapshot.stateAt(Index).getBlock() instanceof PistonHeadBlock) {
                BuildTime = 1.0f;
            }
            Indices.add(Index);
            BuildTimes.add(Math.min(1.0f, BuildTime));
            RestoreTimes.add(Math.min(1.0f, RestoreTime));
        });
        int Count = Indices.size();
        Integer[] ByBuild = new Integer[Count];
        Integer[] ByRestore = new Integer[Count];
        for (int Index = 0; Index < Count; Index++) {
            ByBuild[Index] = Index;
            ByRestore[Index] = Index;
        }
        Arrays.sort(ByBuild, (A, B) -> Float.compare(BuildTimes.get(A), BuildTimes.get(B)));
        Arrays.sort(ByRestore, (A, B) -> Float.compare(RestoreTimes.get(A), RestoreTimes.get(B)));
        int[] Build = new int[Count];
        float[] BuildAt = new float[Count];
        int[] Restore = new int[Count];
        float[] RestoreAt = new float[Count];
        for (int Index = 0; Index < Count; Index++) {
            Build[Index] = Indices.get(ByBuild[Index]);
            BuildAt[Index] = BuildTimes.get(ByBuild[Index]);
            Restore[Index] = Indices.get(ByRestore[Index]);
            RestoreAt[Index] = RestoreTimes.get(ByRestore[Index]);
        }
        return new Z0002Plan(Build, BuildAt, Restore, RestoreAt, Design);
    }

    public static Set<Integer> pads(Z0002Domain Domain) {
        Set<Integer> Found = new HashSet<>();
        Z0002Region Region = Domain.Region;
        for (Vec3 Spot : Domain.Arrivals.values()) {
            BlockPos Feet = BlockPos.containing(Spot.x, Region.floorY() + 1, Spot.z);
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    for (int Dy = -1; Dy <= 1; Dy++) {
                        BlockPos Cell = Feet.offset(Dx, Dy, Dz);
                        if (Region.contains(Cell) && Vec3.atBottomCenterOf(Cell).distanceTo(new Vec3(Spot.x, Cell.getY(), Spot.z)) <= PadReach + 0.75) {
                            Found.add(Region.index(Cell));
                        }
                    }
                }
            }
        }
        return Found;
    }

    public int build(ServerLevel World, Z0002Domain Domain, float Progress, int Budget) {
        int[] Done = {0};
        Z0002Build.withCapture(World, () -> {
            while (Built < Build.length && BuildAt[Built] <= Progress && Done[0] < Budget) {
                Z0002Build.placeIndex(World, Domain, Design, Build[Built]);
                Built++;
                Done[0]++;
            }
        });
        return Done[0];
    }

    public int restore(ServerLevel World, Z0002Domain Domain, float Progress, int Budget) {
        int[] Done = {0};
        Z0002Build.withCapture(World, () -> {
            while (Restored < Restore.length && RestoreAt[Restored] <= Progress && Done[0] < Budget) {
                Z0002Build.restoreIndex(World, Domain, Restore[Restored]);
                Restored++;
                Done[0]++;
            }
        });
        return Done[0];
    }
}
