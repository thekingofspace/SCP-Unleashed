package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class ReactorGridManager {
    public static final int BuildsPerTick = 8;

    private static final Map<ServerLevel, ReactorGridManager> Managers = new WeakHashMap<>();

    private final Map<BlockPos, ReactorGrid> GridByMember = new HashMap<>();
    private final Set<ReactorGrid> Grids = new LinkedHashSet<>();
    private final Set<BlockPos> Seeds = new LinkedHashSet<>();

    private ReactorGridManager() {
    }

    public static ReactorGridManager get(ServerLevel World) {
        return Managers.computeIfAbsent(World, Key -> new ReactorGridManager());
    }

    public static void tickLevel(ServerLevel World) {
        ReactorGridManager Manager = Managers.get(World);
        if (Manager != null) {
            Manager.tick(World);
        }
    }

    public static void saveLevel(ServerLevel World) {
        ReactorGridManager Manager = Managers.get(World);
        if (Manager != null) {
            for (ReactorGrid Grid : Manager.Grids) {
                Grid.writeBack();
            }
        }
    }

    public static void forget(ServerLevel World) {
        saveLevel(World);
        Managers.remove(World);
    }

    public static void unloaded(ServerLevel World, BlockPos Pos) {
        ReactorGridManager Manager = Managers.get(World);
        if (Manager != null) {
            Manager.discard(Manager.GridByMember.get(Pos));
            Manager.Seeds.remove(Pos);
        }
    }

    public static void chunkUnloading(ServerLevel World, ChunkPos Chunk) {
        ReactorGridManager Manager = Managers.get(World);
        if (Manager == null || Manager.Grids.isEmpty()) {
            return;
        }
        for (ReactorGrid Grid : List.copyOf(Manager.Grids)) {
            boolean Inside = false;
            for (BlockPos Member : Grid.getMembers()) {
                if (Member.getX() >> 4 == Chunk.x && Member.getZ() >> 4 == Chunk.z) {
                    Inside = true;
                    break;
                }
            }
            if (!Inside) {
                continue;
            }
            Manager.discard(Grid);
            for (BlockPos Member : Grid.getMembers()) {
                if (Member.getX() >> 4 != Chunk.x || Member.getZ() >> 4 != Chunk.z) {
                    Manager.Seeds.add(Member);
                }
            }
        }
        Manager.Seeds.removeIf(Seed -> Seed.getX() >> 4 == Chunk.x && Seed.getZ() >> 4 == Chunk.z);
    }

    public void seed(BlockPos Pos) {
        if (!GridByMember.containsKey(Pos)) {
            Seeds.add(Pos.immutable());
        }
    }

    public void changed(BlockPos Pos) {
        discard(GridByMember.get(Pos));
        Seeds.add(Pos.immutable());
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            discard(GridByMember.get(Next));
            Seeds.add(Next);
        }
    }

    public void discard(@Nullable ReactorGrid Grid) {
        if (Grid == null || !Grids.remove(Grid)) {
            return;
        }
        Grid.writeBack();
        Grid.markDiscarded();
        for (BlockPos Member : Grid.getMembers()) {
            GridByMember.remove(Member, Grid);
        }
    }

    @Nullable
    public ReactorGrid gridAt(BlockPos Pos) {
        return GridByMember.get(Pos);
    }

    @Nullable
    public ReactorGrid gridAt(ServerLevel World, BlockPos Pos) {
        ReactorGrid Grid = GridByMember.get(Pos);
        if (Grid != null) {
            return Grid;
        }
        if (!World.isLoaded(Pos) || !(World.getBlockState(Pos).getBlock() instanceof TowerSegmentBlock)) {
            return null;
        }
        Seeds.remove(Pos);
        return register(ReactorGrid.build(World, Pos));
    }

    @Nullable
    public GridTower towerAt(ServerLevel World, BlockPos Pos) {
        ReactorGrid Grid = gridAt(World, Pos);
        return Grid == null ? null : Grid.towerAt(Pos);
    }

    private ReactorGrid register(ReactorGrid Grid) {
        for (BlockPos Member : Grid.getMembers()) {
            ReactorGrid Previous = GridByMember.get(Member);
            if (Previous != null && Previous != Grid) {
                discard(Previous);
            }
        }
        for (BlockPos Member : Grid.getMembers()) {
            GridByMember.put(Member, Grid);
            Seeds.remove(Member);
        }
        Grids.add(Grid);
        return Grid;
    }

    private void tick(ServerLevel World) {
        if (!Seeds.isEmpty()) {
            List<BlockPos> Batch = new ArrayList<>();
            Iterator<BlockPos> Pending = Seeds.iterator();
            while (Pending.hasNext() && Batch.size() < BuildsPerTick) {
                Batch.add(Pending.next());
                Pending.remove();
            }
            for (BlockPos Seed : Batch) {
                if (!GridByMember.containsKey(Seed) && World.isLoaded(Seed) && World.getBlockState(Seed).getBlock() instanceof TowerSegmentBlock) {
                    register(ReactorGrid.build(World, Seed));
                }
            }
        }
        for (ReactorGrid Grid : List.copyOf(Grids)) {
            if (!Grid.isDiscarded()) {
                Grid.tick(World);
            }
        }
    }
}
