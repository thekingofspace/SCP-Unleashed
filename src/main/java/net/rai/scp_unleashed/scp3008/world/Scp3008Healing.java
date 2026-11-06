package net.rai.scp_unleashed.scp3008.world;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.Scp3008World;
import net.rai.scp_unleashed.scp3008.block.Scp3008RackPostBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008RafterBlock;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Canvas;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008Healing {
    public static final int Layout = 5;
    public static final int RackTop = 16;
    public static final int HealsPerTick = 4;
    public static final int ChecksPerTick = 6;
    public static final int RebuildsPerTick = 1;
    public static final int Patience = 400;
    public static final int Reach = 48;
    public static final int MaxRadius = 8;
    public static final int[] OldHangerX = {16, 32};
    public static final int Quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    public static final int Wipe = Quiet | Block.UPDATE_SUPPRESS_DROPS;
    private static final Object Lock = new Object();
    private static final Deque<Pending> Healing = new ArrayDeque<>();
    private static final Set<Long> Queued = new HashSet<>();
    private static final Set<Long> Unsure = new HashSet<>();
    private static Job Running;

    private Scp3008Healing() {
    }

    private record Pending(long Chunk, boolean Fresh, int Waited) {
    }

    private static final class Pass {
        private boolean Unsure;
    }

    public record Report(int Chunks, int Changed, int Foreign, int Staff) {
        public Report plus(Report Other) {
            return new Report(Chunks + Other.Chunks, Changed + Other.Changed, Foreign + Other.Foreign, Staff + Other.Staff);
        }
    }

    private static final class Job {
        private final CommandSourceStack Source;
        private final List<ChunkPos> Chunks;
        private final boolean Apply;
        private final int Radius;
        private int Next;
        private Report Total = new Report(0, 0, 0, 0);

        private Job(CommandSourceStack Source, List<ChunkPos> Chunks, boolean Apply, int Radius) {
            this.Source = Source;
            this.Chunks = Chunks;
            this.Apply = Apply;
            this.Radius = Radius;
        }
    }

    private static final class Capture implements Scp3008Canvas {
        private final int MinX;
        private final int MinZ;
        private final int MaxX;
        private final int MaxZ;
        private final int LowY;
        private final int HighY;
        private final Set<Integer> Kinds;
        private final Map<Long, Integer> Codes = new HashMap<>();

        private Capture(int MinX, int MinZ, int MaxX, int MaxZ, int LowY, int HighY, Set<Integer> Kinds) {
            this.MinX = MinX;
            this.MinZ = MinZ;
            this.MaxX = MaxX;
            this.MaxZ = MaxZ;
            this.LowY = LowY;
            this.HighY = HighY;
            this.Kinds = Kinds;
        }

        @Override
        public int minX() {
            return MinX;
        }

        @Override
        public int minZ() {
            return MinZ;
        }

        @Override
        public int maxX() {
            return MaxX;
        }

        @Override
        public int maxZ() {
            return MaxZ;
        }

        @Override
        public void put(int X, int Y, int Z, int Code) {
            if (Y >= LowY && Y <= HighY && (Kinds == null || Kinds.contains(Scp3008Code.kind(Code)))) {
                Codes.put(BlockPos.asLong(X, Y, Z), Code);
            }
        }

        private Integer at(BlockPos Pos) {
            return Codes.get(Pos.asLong());
        }

        private static Capture paint(int MinX, int MinZ, int MaxX, int MaxZ, int LowY, int HighY, Set<Integer> Kinds) {
            Capture Made = new Capture(MinX, MinZ, MaxX, MaxZ, LowY, HighY, Kinds);
            Scp3008Painter.paintArea(MinX, MinZ, MaxX, MaxZ, Made);
            return Made;
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !Scp3008World.strict(World)) {
            return;
        }
        ChunkPos Pos = Event.getChunk().getPos();
        long Key = Pos.toLong();
        synchronized (Lock) {
            if (Queued.add(Key)) {
                Healing.add(new Pending(Key, Event.isNewChunk(), 0));
            }
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -Reach / 16; Dz <= Reach / 16; Dz++) {
                    long Near = ChunkPos.asLong(Pos.x + Dx, Pos.z + Dz);
                    if (Unsure.remove(Near) && Queued.add(Near)) {
                        Healing.add(new Pending(Near, false, 0));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !Scp3008World.strict(World)) {
            return;
        }
        drain(World);
        work(World);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        synchronized (Lock) {
            Healing.clear();
            Queued.clear();
            Unsure.clear();
            Running = null;
        }
    }

    public static boolean current(LevelChunk Chunk) {
        return Chunk.getData(Scp3008Registry.LayoutVersion) >= Layout;
    }

    public static void mark(LevelChunk Chunk) {
        Chunk.setData(Scp3008Registry.LayoutVersion, Layout);
        Chunk.setUnsaved(true);
    }

    private static void drain(ServerLevel World) {
        int Budget = HealsPerTick;
        int Visits;
        synchronized (Lock) {
            Visits = Healing.size();
        }
        while (Budget > 0 && Visits-- > 0) {
            Pending Next;
            synchronized (Lock) {
                Next = Healing.poll();
            }
            if (Next == null) {
                return;
            }
            ChunkPos Pos = new ChunkPos(Next.Chunk());
            LevelChunk Chunk = World.getChunkSource().getChunkNow(Pos.x, Pos.z);
            if (Chunk == null) {
                synchronized (Lock) {
                    if (Next.Waited() < Patience) {
                        Healing.add(new Pending(Next.Chunk(), Next.Fresh(), Next.Waited() + 1));
                    } else {
                        Queued.remove(Next.Chunk());
                    }
                }
                continue;
            }
            synchronized (Lock) {
                Queued.remove(Next.Chunk());
            }
            if (current(Chunk)) {
                continue;
            }
            if (!Next.Fresh()) {
                Pass Check = new Pass();
                heal(World, Pos, Check);
                Budget--;
                if (Check.Unsure) {
                    synchronized (Lock) {
                        Unsure.add(Next.Chunk());
                    }
                    continue;
                }
            }
            mark(Chunk);
        }
    }

    private static boolean loaded(ServerLevel World, BlockPos Pos, Pass Check) {
        boolean Found = World.getChunkSource().getChunkNow(Pos.getX() >> 4, Pos.getZ() >> 4) != null;
        if (!Found) {
            Check.Unsure = true;
        }
        return Found;
    }

    private static boolean wallUnder(ServerLevel World, BlockPos Pos, Pass Check) {
        return loaded(World, Pos, Check) && World.getBlockState(Pos.below()).is(Scp3008Registry.DisplayWall.get());
    }

    private static int arms(Capture Plan, BlockPos Pos) {
        Integer Code = Plan.at(Pos);
        return Code == null || Scp3008Code.kind(Code) != Scp3008Code.Rafter ? -1 : Scp3008Code.variant(Code);
    }

    private static boolean reaches(int Arms, Direction Side) {
        return Arms > 0 && (Arms & Scp3008RafterBlock.bit(Side)) != 0;
    }

    private static boolean held(ServerLevel World, Capture Plan, BlockPos Pos, Direction Side, Pass Check) {
        BlockPos At = Pos;
        for (int Step = 0; Step < Reach; Step++) {
            int Arms = arms(Plan, At);
            if (Arms < 0 || !loaded(World, At, Check)) {
                return false;
            }
            if (wallUnder(World, At, Check)) {
                return true;
            }
            if (!reaches(Arms, Side)) {
                return false;
            }
            At = At.relative(Side);
        }
        return false;
    }

    private static boolean grounded(ServerLevel World, Capture Plan, BlockPos Pos, Pass Check) {
        if (wallUnder(World, Pos, Check)) {
            return true;
        }
        int Arms = arms(Plan, Pos);
        for (Direction Side : new Direction[]{Direction.NORTH, Direction.WEST}) {
            if (reaches(Arms, Side) && reaches(Arms, Side.getOpposite()) && held(World, Plan, Pos, Side, Check) && held(World, Plan, Pos, Side.getOpposite(), Check)) {
                return true;
            }
        }
        return false;
    }

    private static boolean keeps(ServerLevel World, Capture Plan, BlockPos Pos, Direction Side, Pass Check) {
        BlockPos Next = Pos.relative(Side);
        if (!loaded(World, Next, Check)) {
            return true;
        }
        BlockState There = World.getBlockState(Next);
        if (There.is(Scp3008Registry.DisplayWall.get())) {
            return true;
        }
        if (arms(Plan, Next) < 0) {
            return false;
        }
        return There.is(Scp3008Registry.Rafter.get()) || There.isAir() && grounded(World, Plan, Next, Check);
    }

    public static int heal(ServerLevel World, ChunkPos Chunk) {
        return heal(World, Chunk, new Pass());
    }

    private static int heal(ServerLevel World, ChunkPos Chunk, Pass Check) {
        int MinX = Chunk.getMinBlockX();
        int MinZ = Chunk.getMinBlockZ();
        int MaxX = Chunk.getMaxBlockX();
        int MaxZ = Chunk.getMaxBlockZ();
        Capture Frame = Capture.paint(MinX - 1, MinZ - Reach, MaxX + 1, MaxZ + Reach, Scp3008Painter.RafterY, Scp3008Painter.RafterY, Set.of(Scp3008Code.Rafter));
        Capture Hangers = Capture.paint(MinX, MinZ, MaxX, MaxZ, Scp3008Plan.FloorY, Scp3008Plan.CeilingY, Set.of(Scp3008Code.Cable, Scp3008Code.Light, Scp3008Code.Sign));
        int Changed = 0;
        Scp3008RafterBlock Rafter = Scp3008Registry.Rafter.get();
        for (Map.Entry<Long, Integer> Entry : Frame.Codes.entrySet()) {
            BlockPos Pos = BlockPos.of(Entry.getKey());
            if (Pos.getX() < MinX || Pos.getX() > MaxX || Pos.getZ() < MinZ || Pos.getZ() > MaxZ) {
                continue;
            }
            BlockState Have = World.getBlockState(Pos);
            boolean Present = Have.is(Rafter);
            if (!Present && !(Have.isAir() && grounded(World, Frame, Pos, Check))) {
                continue;
            }
            int Wanted = Scp3008Code.variant(Entry.getValue());
            int Kept = 0;
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                if (reaches(Wanted, Side) && keeps(World, Frame, Pos, Side, Check)) {
                    Kept |= Scp3008RafterBlock.bit(Side);
                }
            }
            BlockState Want = Rafter.withArms(Kept);
            if (!Want.equals(Have)) {
                World.setBlock(Pos, Want, Quiet);
                Changed++;
            }
        }
        TreeMap<Integer, List<BlockPos>> Rising = new TreeMap<>();
        for (Map.Entry<Long, Integer> Entry : Hangers.Codes.entrySet()) {
            if (Scp3008Code.kind(Entry.getValue()) == Scp3008Code.Cable) {
                BlockPos Pos = BlockPos.of(Entry.getKey());
                Rising.computeIfAbsent(Pos.getY(), Ignored -> new ArrayList<>()).add(Pos);
            }
        }
        for (List<BlockPos> Layer : Rising.values()) {
            for (BlockPos Pos : Layer) {
                BlockState Have = World.getBlockState(Pos);
                BlockState Want = Scp3008Palette.state(Hangers.at(Pos));
                if (Want == null || Want.equals(Have)) {
                    continue;
                }
                BlockState Below = World.getBlockState(Pos.below());
                boolean Hung = Below.is(Scp3008Registry.Cable.get()) || Below.is(Scp3008Registry.StoreLight.get()) || Below.is(Scp3008Registry.DepartmentSign.get());
                if (Have.is(Scp3008Registry.Cable.get()) || Have.isAir() && Hung) {
                    World.setBlock(Pos, Want, Quiet);
                    Changed++;
                }
            }
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = MinX; X <= MaxX; X++) {
            int Lx = Scp3008Plan.localX(X);
            if (Lx != OldHangerX[0] && Lx != OldHangerX[1]) {
                continue;
            }
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                int Lz = Scp3008Plan.localZ(Z);
                if (Lz < Scp3008Painter.LightFirst || (Lz - Scp3008Painter.LightFirst) % Scp3008Painter.LightStep != 0) {
                    continue;
                }
                for (int Y = Scp3008Plan.LightY + 1; Y < Scp3008Plan.CeilingY; Y++) {
                    Cursor.set(X, Y, Z);
                    if (Hangers.at(Cursor) == null && World.getBlockState(Cursor).is(Scp3008Registry.Cable.get())) {
                        World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Quiet);
                        Changed++;
                    }
                }
            }
        }
        Changed += racks(World, MinX, MinZ, MaxX, MaxZ, Check);
        return Changed;
    }

    private static int racks(ServerLevel World, int MinX, int MinZ, int MaxX, int MaxZ, Pass Check) {
        Capture Posts = Capture.paint(MinX, MinZ, MaxX, MaxZ, Scp3008Plan.Stand, Scp3008Plan.Stand + RackTop, Set.of(Scp3008Code.RackPost));
        Scp3008RackPostBlock Post = Scp3008Registry.RackPost.get();
        int Changed = 0;
        for (Map.Entry<Long, Integer> Entry : Posts.Codes.entrySet()) {
            BlockPos Pos = BlockPos.of(Entry.getKey());
            BlockState Have = World.getBlockState(Pos);
            if (!Have.is(Post)) {
                continue;
            }
            boolean Ready = true;
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                Ready &= loaded(World, Pos.relative(Side), Check);
            }
            if (!Ready) {
                continue;
            }
            BlockState Planned = Post.fromBits(Scp3008Code.variant(Entry.getValue()));
            BlockState Want = Post.joined(World, Pos, Have.setValue(Scp3008RackPostBlock.Axis, Planned.getValue(Scp3008RackPostBlock.Axis)));
            if (!Want.equals(Have)) {
                World.setBlock(Pos, Want, Quiet);
                Changed++;
            }
        }
        return Changed;
    }

    public static Report rebuild(ServerLevel World, ChunkPos Chunk, boolean Apply) {
        int MinX = Chunk.getMinBlockX();
        int MinZ = Chunk.getMinBlockZ();
        int MaxX = Chunk.getMaxBlockX();
        int MaxZ = Chunk.getMaxBlockZ();
        int LowY = Scp3008Plan.FloorY;
        int HighY = Math.min(Scp3008Plan.CeilingTop, World.getMaxBuildHeight() - 1);
        Capture Plan = Capture.paint(MinX, MinZ, MaxX, MaxZ, LowY, HighY, null);
        Set<Long> Furnished = new HashSet<>();
        for (Map.Entry<Long, Integer> Entry : Plan.Codes.entrySet()) {
            if (Scp3008Code.decor(Entry.getValue())) {
                Furnished.add(Entry.getKey());
            }
        }
        for (int Ux = Scp3008Plan.unitX(MinX); Ux <= Scp3008Plan.unitX(MaxX); Ux++) {
            for (int Uz = Scp3008Plan.unitZ(MinZ); Uz <= Scp3008Plan.unitZ(MaxZ); Uz++) {
                for (Scp3008Plan.Plot Next : Scp3008Plan.plots(Ux, Uz)) {
                    if (Next.Type() != Scp3008Plan.RoomPlot) {
                        continue;
                    }
                    for (Scp3008Showrooms.Placement Piece : Scp3008Showrooms.plot(Ux, Uz, Next)) {
                        Furnished.add(Piece.Pos().asLong());
                    }
                }
            }
        }
        AABB Column = new AABB(MinX, LowY, MinZ, MaxX + 1, HighY + 1, MaxZ + 1);
        List<Scp3008Staff> Staff = World.getEntitiesOfClass(Scp3008Staff.class, Column);
        int Changed = 0;
        int Foreign = 0;
        BlockState Air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = MinX; X <= MaxX; X++) {
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                for (int Y = LowY; Y <= HighY; Y++) {
                    Cursor.set(X, Y, Z);
                    long Key = Cursor.asLong();
                    Integer Code = Plan.Codes.get(Key);
                    BlockState Have = World.getBlockState(Cursor);
                    if (Furnished.contains(Key)) {
                        if (Apply && !Have.isAir()) {
                            clear(World, Cursor);
                            World.setBlock(Cursor, Air, Wipe);
                        }
                        continue;
                    }
                    BlockState Want = Code == null || Scp3008Code.decor(Code) ? null : Scp3008Palette.state(Code);
                    if (Want == null) {
                        Want = Air;
                    }
                    if (Want.equals(Have)) {
                        continue;
                    }
                    Changed++;
                    if (Want.isAir()) {
                        Foreign++;
                    }
                    if (Apply) {
                        clear(World, Cursor);
                        World.setBlock(Cursor, Want, Wipe);
                    }
                }
            }
        }
        if (Apply) {
            for (Scp3008Staff Clerk : Staff) {
                Clerk.discard();
            }
            Scp3008Furnishing.decorateArea(World, MinX, MinZ, MaxX, MaxZ);
            LevelChunk Loaded = World.getChunkSource().getChunkNow(Chunk.x, Chunk.z);
            if (Loaded != null) {
                mark(Loaded);
            }
        }
        return new Report(1, Changed, Foreign, Staff.size());
    }

    private static void clear(ServerLevel World, BlockPos Pos) {
        BlockEntity Entity = World.getBlockEntity(Pos);
        if (Entity instanceof Container Holder) {
            Holder.clearContent();
        }
    }

    public static List<ChunkPos> around(ServerLevel World, BlockPos Centre, int Radius, int[] Skipped) {
        List<ChunkPos> Found = new ArrayList<>();
        ChunkPos Middle = new ChunkPos(Centre);
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                if (World.getChunkSource().getChunkNow(Middle.x + Dx, Middle.z + Dz) != null) {
                    Found.add(new ChunkPos(Middle.x + Dx, Middle.z + Dz));
                } else {
                    Skipped[0]++;
                }
            }
        }
        return Found;
    }

    public static boolean busy() {
        synchronized (Lock) {
            return Running != null;
        }
    }

    public static void start(CommandSourceStack Source, List<ChunkPos> Chunks, boolean Apply, int Radius) {
        synchronized (Lock) {
            Running = new Job(Source, Chunks, Apply, Radius);
        }
    }

    private static void work(ServerLevel World) {
        Job Current;
        synchronized (Lock) {
            Current = Running;
        }
        if (Current == null) {
            return;
        }
        int Budget = Current.Apply ? RebuildsPerTick : ChecksPerTick;
        while (Budget-- > 0 && Current.Next < Current.Chunks.size()) {
            ChunkPos Pos = Current.Chunks.get(Current.Next++);
            if (World.getChunkSource().getChunkNow(Pos.x, Pos.z) != null) {
                Current.Total = Current.Total.plus(rebuild(World, Pos, Current.Apply));
            }
        }
        if (Current.Next < Current.Chunks.size()) {
            return;
        }
        Report Total = Current.Total;
        if (Current.Apply) {
            Current.Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.regen.done", Total.Chunks(), Total.Changed() - Total.Foreign(),
                    Total.Foreign(), Total.Staff()), true);
        } else {
            Current.Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.regen.preview", Total.Chunks(), Total.Changed(), Total.Foreign(),
                    Total.Staff(), Current.Radius), false);
        }
        synchronized (Lock) {
            if (Running == Current) {
                Running = null;
            }
        }
    }
}
