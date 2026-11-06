package net.rai.scp_unleashed.scp5001.world;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Blocks;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Plan;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Slot;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp5001Emitter {
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final long BudgetNanos = 8_000_000L;
    public static final int SlabHeight = 6;
    public static final int Slabs = (Scp5001Plan.Height + SlabHeight - 1) / SlabHeight;
    public static final int ShellStep = 1;
    public static final int PropStep = 2 + Slabs;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, BlockState> States = new ConcurrentHashMap<>();
    private static final LinkedHashSet<Long> Pending = new LinkedHashSet<>();
    private static Job Current;
    private static Scp5001Plan Cached;
    private static boolean Warned;

    private static final class Job {
        final long Chunk;
        int Step;

        Job(long Chunk) {
            this.Chunk = Chunk;
        }
    }

    private Scp5001Emitter() {
    }

    public static synchronized Scp5001Plan plan(long Seed) {
        if (Cached == null || Cached.seed() != Seed) {
            Cached = Scp5001Plan.build(Seed);
        }
        return Cached;
    }

    public static synchronized void clear() {
        Pending.clear();
        Current = null;
        Warned = false;
    }

    public static synchronized int pending() {
        return Pending.size() + (Current != null ? 1 : 0);
    }

    public static BlockState state(String Text) {
        BlockState Found = States.get(Text);
        if (Found != null) {
            return Found;
        }
        int Bracket = Text.indexOf('[');
        String Id = Bracket < 0 ? Text : Text.substring(0, Bracket);
        ResourceLocation Key = ResourceLocation.tryParse(Id);
        Optional<Block> Owner = Key == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(Key);
        if (Owner.isEmpty()) {
            Found = Scp5001Blocks.eventBlock(Id) ? Blocks.STRUCTURE_VOID.defaultBlockState() : Blocks.AIR.defaultBlockState();
        } else {
            Found = Owner.get().defaultBlockState();
            if (Bracket >= 0) {
                for (String Pair : Text.substring(Bracket + 1, Text.length() - 1).split(",")) {
                    int Equals = Pair.indexOf('=');
                    if (Equals > 0) {
                        Found = with(Found, Pair.substring(0, Equals), Pair.substring(Equals + 1));
                    }
                }
            }
        }
        States.put(Text, Found);
        return Found;
    }

    public static BlockState with(BlockState State, String Name, String Value) {
        Property<?> Found = State.getBlock().getStateDefinition().getProperty(Name);
        return Found == null ? State : apply(State, Found, Value);
    }

    private static <T extends Comparable<T>> BlockState apply(BlockState State, Property<T> Found, String Value) {
        Optional<T> Parsed = Found.getValue(Value);
        return Parsed.isPresent() ? State.setValue(Found, Parsed.get()) : State;
    }

    public static synchronized void planned(ServerLevel World, Scp5001Sites Data) {
        if (Data.legacy()) {
            return;
        }
        for (ChunkPos Each : Data.chunks()) {
            if (World.hasChunk(Each.x, Each.z) && !Data.done(Each.toLong())) {
                Pending.add(Each.toLong());
            }
        }
    }

    public static synchronized void chunkLoaded(ServerLevel World, ChunkPos Pos) {
        if (World.dimension() != Level.OVERWORLD) {
            return;
        }
        Scp5001Sites Data = Scp5001Sites.get(World.getServer());
        if (!Data.planned() || Data.legacy() || Data.done(Pos.toLong())) {
            return;
        }
        BoundingBox Box = Data.bounds();
        if (Pos.getMaxBlockX() < Box.minX() || Pos.getMinBlockX() > Box.maxX() || Pos.getMaxBlockZ() < Box.minZ() || Pos.getMinBlockZ() > Box.maxZ()) {
            return;
        }
        Pending.add(Pos.toLong());
    }

    public static synchronized void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        Scp5001Sites Data = Scp5001Sites.get(Server);
        if (!Data.planned()) {
            return;
        }
        if (Data.legacy()) {
            if (!Warned) {
                Warned = true;
                Log.warn("SCP-5001 site at {} uses plan version {}; it is kept as built. Use /scp_unleashed scp5001 site reset to plan a new one.", Data.bounds(),
                        Data.version());
            }
            return;
        }
        if (!Data.told() && World.getGameTime() % 20 == 7) {
            tryTell(World, Data);
        }
        long Start = System.nanoTime();
        while (System.nanoTime() - Start < BudgetNanos) {
            if (Current == null) {
                Current = next(World, Data);
                if (Current == null) {
                    return;
                }
            }
            ChunkPos Pos = new ChunkPos(Current.Chunk);
            if (!World.hasChunk(Pos.x, Pos.z)) {
                Current = null;
                continue;
            }
            if (step(World, Data, Pos, Current.Step)) {
                Data.finish(Current.Chunk);
                Current = null;
            } else {
                Current.Step++;
            }
        }
    }

    private static Job next(ServerLevel World, Scp5001Sites Data) {
        while (!Pending.isEmpty()) {
            Long Chunk = Pending.iterator().next();
            Pending.remove(Chunk);
            ChunkPos Pos = new ChunkPos(Chunk);
            if (!Data.done(Chunk) && World.hasChunk(Pos.x, Pos.z)) {
                return new Job(Chunk);
            }
        }
        return null;
    }

    public static void emitChunk(ServerLevel World, Scp5001Sites Data, ChunkPos Pos) {
        int Step = 0;
        while (!step(World, Data, Pos, Step)) {
            Step++;
        }
        Data.finish(Pos.toLong());
    }

    public static boolean step(ServerLevel World, Scp5001Sites Data, ChunkPos Pos, int Step) {
        BlockPos Origin = Data.origin();
        Scp5001Plan Plan = plan(Data.seed());
        int MinX = Pos.getMinBlockX() - Origin.getX();
        int MinZ = Pos.getMinBlockZ() - Origin.getZ();
        int MaxX = Pos.getMaxBlockX() - Origin.getX();
        int MaxZ = Pos.getMaxBlockZ() - Origin.getZ();
        if (MaxX < 0 || MaxZ < 0 || MinX >= Scp5001Plan.Width || MinZ >= Scp5001Plan.Depth) {
            return true;
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        LevelChunk Chunk = World.getChunk(Pos.x, Pos.z);
        if (Step == 0) {
            evict(World, Data, Pos);
            return false;
        }
        if (Step == ShellStep) {
            Plan.paint(MinX, MinZ, MaxX, MaxZ, 0, (X, Y, Z, Text) -> write(World, Chunk, Cursor.set(Origin.getX() + X, Origin.getY() + Y, Origin.getZ() + Z), state(Text)));
            return false;
        }
        if (Step < PropStep) {
            int Slab = Step - 2;
            Plan.paint(MinX, MinZ, MaxX, MaxZ, Slab * SlabHeight, Slab * SlabHeight + SlabHeight - 1, 1,
                    (X, Y, Z, Text) -> write(World, Chunk, Cursor.set(Origin.getX() + X, Origin.getY() + Y, Origin.getZ() + Z), state(Text)));
            return false;
        }
        for (Scp5001Slot Each : Plan.slotsIn(MinX, MinZ, MaxX, MaxZ)) {
            prop(World, Origin, Each, Plan.seed());
        }
        return true;
    }

    private static void write(ServerLevel World, LevelChunk Chunk, BlockPos Pos, BlockState State) {
        if (Chunk.getBlockEntityNbt(Pos) != null) {
            Chunk.getBlockEntity(Pos, LevelChunk.EntityCreationType.CHECK);
        }
        BlockState Old = Chunk.getBlockState(Pos);
        if (Old == State && (!State.hasBlockEntity() || Chunk.getBlockEntity(Pos) != null)) {
            return;
        }
        if (Old.hasBlockEntity() || Chunk.getBlockEntities().containsKey(Pos)) {
            World.removeBlockEntity(Pos);
        }
        World.setBlock(Pos, State, Flags);
    }

    private static void evict(ServerLevel World, Scp5001Sites Data, ChunkPos Pos) {
        BoundingBox Box = Data.bounds();
        AABB Area = new AABB(Math.max(Box.minX(), Pos.getMinBlockX()), Box.minY(), Math.max(Box.minZ(), Pos.getMinBlockZ()), Math.min(Box.maxX(), Pos.getMaxBlockX()) + 1,
                Box.maxY() + 1, Math.min(Box.maxZ(), Pos.getMaxBlockZ()) + 1);
        BlockPos Tell = Data.tell();
        for (Entity Each : World.getEntitiesOfClass(Entity.class, Area)) {
            if (Each instanceof ServerPlayer User) {
                User.teleportTo(Tell.getX() + 0.5, World.getHeight(Heightmap.Types.MOTION_BLOCKING, Tell.getX(), Tell.getZ()), Tell.getZ() + 0.5);
                continue;
            }
            ResourceLocation Type = BuiltInRegistries.ENTITY_TYPE.getKey(Each.getType());
            if (!Scp_unleashed.ModId.equals(Type.getNamespace())) {
                Each.discard();
            }
        }
    }

    public static long mix(int X, int Y, int Z, long Seed) {
        long Value = X * 73856093L ^ Y * 19349663L ^ Z * 83492791L ^ Seed;
        Value ^= Value >>> 33;
        Value *= 0xff51afd7ed558ccdL;
        Value ^= Value >>> 33;
        return Value;
    }

    public static void prop(ServerLevel World, BlockPos Origin, Scp5001Slot Each, long Seed) {
        Scp5001Palettes.Role Role = Scp5001Palettes.role(Each.Role());
        if (Role == null) {
            return;
        }
        ResourceLocation Chosen = Role.pick(mix(Each.X(), Each.Y(), Each.Z(), Seed));
        Optional<Block> Owner = Chosen == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(Chosen);
        if (Owner.isEmpty()) {
            return;
        }
        BlockState Base = with(Owner.get().defaultBlockState(), "facing", Each.Facing());
        int X = Origin.getX() + Each.X();
        int Z = Origin.getZ() + Each.Z();
        int Y = Origin.getY() + Each.Y();
        switch (Role.Kind()) {
            case "ceiling" -> place(World, new BlockPos(X, Origin.getY() + Each.Ceiling(), Z), Base);
            case "wall" -> {
                if (Each.Wall()) {
                    place(World, new BlockPos(X, Y + (Each.Role().equals("signage") ? 2 : 1), Z), Base);
                }
            }
            case "floor_tall" -> {
                BlockPos Lower = new BlockPos(X, Y, Z);
                if (World.getBlockState(Lower).isAir() && World.getBlockState(Lower.above()).isAir()) {
                    LevelChunk Chunk = World.getChunkAt(Lower);
                    write(World, Chunk, Lower, with(Base, "half", "lower"));
                    write(World, Chunk, Lower.above(), with(Base, "half", "upper"));
                }
            }
            default -> place(World, new BlockPos(X, Y, Z), Base);
        }
    }

    private static void place(ServerLevel World, BlockPos Pos, BlockState State) {
        if (World.getBlockState(Pos).isAir()) {
            write(World, World.getChunkAt(Pos), Pos, State);
        }
    }

    public static boolean tryTell(ServerLevel World, Scp5001Sites Data) {
        if (Data.told()) {
            return true;
        }
        BlockPos Column = Data.tell();
        if (!World.hasChunk(Column.getX() >> 4, Column.getZ() >> 4)) {
            return false;
        }
        int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Column.getX(), Column.getZ());
        BlockPos Rod = new BlockPos(Column.getX(), Top, Column.getZ());
        BlockState Ground = World.getBlockState(Rod.below());
        if (World.getBlockState(Rod).canBeReplaced() && Ground.isFaceSturdy(World, Rod.below(), Direction.UP) && World.getFluidState(Rod).isEmpty()) {
            World.setBlock(Rod, state("minecraft:lightning_rod[facing=up]"), Block.UPDATE_ALL);
            World.setBlock(Rod.below(), state("minecraft:polished_andesite"), Block.UPDATE_ALL);
        }
        Data.tellPlaced();
        Log.info("SCP-5001 surface tell placed above the airlock at {}", Rod);
        return true;
    }

    public static Direction facing(String Name) {
        Direction Found = Direction.byName(Name);
        return Found == null ? Direction.NORTH : Found;
    }

    public static ArrayDeque<Long> snapshot() {
        synchronized (Scp5001Emitter.class) {
            return new ArrayDeque<>(Pending);
        }
    }
}
